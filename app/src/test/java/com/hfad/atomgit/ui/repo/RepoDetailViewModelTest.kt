package com.hfad.atomgit.ui.repo

import com.hfad.atomgit.MainDispatcherRule
import com.hfad.atomgit.data.model.CommitSummary
import com.hfad.atomgit.data.model.PagedItems
import com.hfad.atomgit.data.model.ProfileCounts
import com.hfad.atomgit.data.model.RepoDetail
import com.hfad.atomgit.data.model.RepoEntry
import com.hfad.atomgit.data.model.RepoEntryKind
import com.hfad.atomgit.data.model.RepoFileContent
import com.hfad.atomgit.data.model.RepoRef
import com.hfad.atomgit.data.model.RepoRefKind
import com.hfad.atomgit.data.model.UserProfile
import com.hfad.atomgit.data.remote.error.ApiError
import com.hfad.atomgit.data.remote.error.ApiResult
import com.hfad.atomgit.data.repository.RepoContentRepository
import com.hfad.atomgit.data.repository.RepoDetailRepository
import com.hfad.atomgit.data.repository.UserProfileRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class RepoDetailViewModelTest {
    @get:Rule
    val dispatcher = MainDispatcherRule()
    private val repository = FakeRepository()
    private val contents = FakeContents()
    private val profiles = FakeProfiles()

    private fun start() =
        RepoDetailViewModel(repository, contents, profiles).also { it.start("owner", "repo") }

    private fun file(path: String) =
        RepoEntry(path.substringAfterLast('/'), path, RepoEntryKind.File)

    private fun commit(sha: String) = CommitSummary(sha, "message")
    private fun <T> page(vararg items: T) = ApiResult.Success(PagedItems(items.toList()))

    @Test
    fun initialLoadUsesDefaultBranchAndDoesNotFetchOtherTabs() = runTest {
        val vm = start()
        assertEquals("develop", vm.state.value.selectedRef?.name)
        assertEquals(listOf("" to "develop"), repository.directories)
        assertTrue(repository.commitPages.isEmpty())
        assertEquals(0, repository.branchCalls)
        assertTrue(contents.calls.isEmpty())
        vm.start("owner", "repo")
        assertEquals(1, repository.detailCalls)
    }

    @Test
    fun successfulEmptyTabIsCached() = runTest {
        val vm = start()
        vm.selectTab(RepoDetailTab.Tags)
        vm.selectTab(RepoDetailTab.Files)
        vm.selectTab(RepoDetailTab.Tags)
        assertEquals(1, repository.tagCalls)
        assertTrue(vm.state.value.tags.loaded)
        assertTrue(vm.state.value.tags.endReached)
    }

    @Test
    fun ownerProfileFillsMissingHeaderWithoutReloadingDirectory() = runTest {
        profiles.loader = {
            ApiResult.Success(
                UserProfile(
                    it,
                    name = "作者昵称",
                    avatarUrl = "https://example.com/avatar.png"
                )
            )
        }
        val vm = start()
        assertEquals("作者昵称", vm.state.value.detail?.ownerName)
        assertEquals("https://example.com/avatar.png", vm.state.value.detail?.ownerAvatarUrl)
        assertEquals(1, repository.directories.size)
        assertNull(vm.state.value.error)
    }

    @Test
    fun ownerProfileFailureDoesNotFailRepositoryHeader() = runTest {
        profiles.loader = { ApiResult.Failure(ApiError.Network) }
        val vm = start()
        assertNotNull(vm.state.value.detail)
        assertNull(vm.state.value.error)
        assertTrue(vm.state.value.files.loaded)
    }

    @Test
    fun completeOwnerDetailsDoNotRequestProfileAgain() = runTest {
        repository.detailFactory = { owner, repo ->
            RepoDetail(
                owner,
                repo,
                repo,
                ownerName = "作者",
                ownerAvatarUrl = "https://example.com/a.png"
            )
        }
        start()
        assertTrue(profiles.calls.isEmpty())
    }

    @Test
    fun oldOwnerProfileCannotUpdateAnotherRepository() = runTest {
        var pending: Continuation<ApiResult<UserProfile>>? = null
        profiles.loader = { login ->
            if (login == "owner") suspendCoroutine<ApiResult<UserProfile>> { pending = it }
            else ApiResult.Success(UserProfile(login, name = "新作者"))
        }
        val vm = start()
        vm.start("next-owner", "next-repo")
        pending!!.resume(ApiResult.Success(UserProfile("owner", name = "旧作者")))
        assertEquals("新作者", vm.state.value.detail?.ownerName)
        assertEquals("next-owner", vm.state.value.detail?.owner)
    }

    @Test
    fun paginationDeduplicatesAndStopsOnlyOnEmptyPageWithoutHeaders() = runTest {
        repository.commits.addAll(listOf(page(commit("a")), page(commit("a"), commit("b")), page()))
        val vm = start()
        vm.selectTab(RepoDetailTab.Commits)
        assertFalse(vm.state.value.commits.endReached)
        vm.loadMore()
        assertEquals(listOf("a", "b"), vm.state.value.commits.items.map { it.sha })
        vm.loadMore()
        vm.loadMore()
        assertEquals(listOf(1, 2, 3), repository.commitPages)
        assertTrue(vm.state.value.commits.endReached)
    }

    @Test
    fun failedPaginationKeepsDataAndRetriesSamePage() = runTest {
        repository.commits.addAll(
            listOf(
                page(commit("a")),
                ApiResult.Failure(ApiError.Network),
                page(commit("b"))
            )
        )
        val vm = start()
        vm.selectTab(RepoDetailTab.Commits)
        vm.loadMore()
        assertEquals(listOf("a"), vm.state.value.commits.items.map { it.sha })
        assertNull(vm.state.value.error)
        assertEquals(ApiError.Network, vm.state.value.commits.error)
        vm.retryCurrent()
        assertEquals(listOf(1, 2, 2), repository.commitPages)
        assertEquals(listOf("a", "b"), vm.state.value.commits.items.map { it.sha })
    }

    @Test
    fun directory404IsLocalAndCanNavigateBack() = runTest {
        repository.directory = { path, _ ->
            if (path.isEmpty()) ApiResult.Success(emptyList()) else ApiResult.Failure(ApiError.NotFound)
        }
        val vm = start()
        vm.openDirectory("missing")
        assertEquals(ApiError.NotFound, vm.state.value.files.error)
        assertNotNull(vm.state.value.detail)
        assertNull(vm.state.value.error)
        assertTrue(vm.navigateUp())
        assertNull(vm.state.value.files.error)
        assertEquals("", vm.state.value.files.path)
        assertFalse(vm.navigateUp())
    }

    @Test
    fun failedRefreshRetriesFirstPageInsteadOfAppendingNextPage() = runTest {
        repository.commits.addAll(
            listOf(
                page(commit("old")),
                page(),
                ApiResult.Failure(ApiError.Network),
                page(commit("new"))
            )
        )
        val vm = start()
        vm.selectTab(RepoDetailTab.Commits)
        vm.loadMore()
        assertTrue(vm.state.value.commits.endReached)
        vm.refresh()
        assertEquals(listOf("old"), vm.state.value.commits.items.map { it.sha })
        vm.retryCurrent()
        assertEquals(listOf(1, 2, 1, 1), repository.commitPages)
        assertEquals(listOf("new"), vm.state.value.commits.items.map { it.sha })
    }

    @Test
    fun oldDirectoryResponseCannotOverwriteNewDirectory() = runTest {
        var pending: Continuation<ApiResult<List<RepoEntry>>>? = null
        repository.directory = { path, _ ->
            if (path == "slow") suspendCoroutine<ApiResult<List<RepoEntry>>> {
                pending = it
            } else ApiResult.Success(listOf(file("$path/ok.txt")))
        }
        val vm = start()
        vm.openDirectory("slow")
        vm.openDirectory("fast")
        pending!!.resume(ApiResult.Success(listOf(file("slow/late.txt"))))
        assertEquals("fast", vm.state.value.files.path)
        assertEquals("fast/ok.txt", vm.state.value.files.entries.single().path)
    }

    @Test
    fun tagSwitchResetsContentUsesShaAndPreservesBranchList() = runTest {
        repository.commits += page(commit("a"))
        val vm = start()
        vm.selectTab(RepoDetailTab.Branches)
        vm.selectTab(RepoDetailTab.Commits)
        vm.openDirectory("src")
        vm.selectRef(RepoRef("develop", RepoRefKind.Tag, "tag-sha"))
        assertEquals(RepoDetailTab.Files, vm.state.value.tab)
        assertEquals("" to "tag-sha", repository.directories.last())
        assertTrue(vm.state.value.commits.items.isEmpty())
        assertFalse(vm.state.value.commits.loaded)
        assertTrue(vm.state.value.branches.loaded)
        vm.selectTab(RepoDetailTab.Branches)
        assertEquals(1, repository.branchCalls)
    }

    @Test
    fun readmeUsesActualCaseAndPathAndKeepsFullContent() = runTest {
        repository.directory =
            { _, _ -> ApiResult.Success(listOf(file("ReadMe.MD"), file("README.txt"))) }
        val markdown = "项目文档\n\n" + "段落内容\n\n".repeat(12_000) +
                (1..12).joinToString("\n\n") { "![图片 $it](docs/$it.png)" } + "\n\n文档结尾"
        contents.result = ApiResult.Success(
            RepoFileContent(
                "ReadMe.MD",
                "ReadMe.MD",
                markdown,
                "base64",
                null,
                null
            )
        )
        val vm = start()
        val readme = vm.state.first { it.readme.loaded }.readme
        assertEquals("ReadMe.MD" to "develop", contents.calls.single())
        assertEquals(markdown, readme.preview)
        assertTrue(contents.imageCalls.isEmpty())

        vm.imagesFor(readme)!!.load("docs/11.png")
        assertEquals(listOf("docs/11.png" to "develop"), contents.imageCalls)
        vm.openDirectory("src")
        vm.openDirectory("")
        assertEquals(1, contents.calls.size)
    }

    @Test
    fun updatedTagWithSameNameUsesNewSha() = runTest {
        val vm = start()
        vm.selectRef(RepoRef("v1", RepoRefKind.Tag, "old-sha"))
        vm.selectRef(RepoRef("v1", RepoRefKind.Tag, "new-sha"))
        assertEquals("" to "new-sha", repository.directories.last())
        assertEquals("new-sha", vm.state.value.selectedRef?.requestRef)
    }

    @Test
    fun readme404HidesPreviewButNetworkFailureCanRetryIndependently() = runTest {
        repository.directory = { _, _ -> ApiResult.Success(listOf(file("README.md"))) }
        contents.result = ApiResult.Failure(ApiError.Network)
        val vm = start()
        assertEquals(ApiError.Network, vm.state.value.readme.error)
        assertNull(vm.state.value.files.error)
        contents.result = ApiResult.Failure(ApiError.NotFound)
        vm.retryReadme()
        assertNull(vm.state.value.readme.preview)
        assertNull(vm.state.value.readme.error)
        assertNull(vm.state.value.readme.path)
    }

    @Test
    fun oldReadmeCannotAppearAfterSwitchingRef() = runTest {
        var pending: Continuation<ApiResult<RepoFileContent>>? = null
        repository.directory =
            { _, ref -> ApiResult.Success(if (ref == "develop") listOf(file("README.md")) else emptyList()) }
        contents.loader = { suspendCoroutine { pending = it } }
        val vm = start()
        vm.selectRef(RepoRef("next", RepoRefKind.Branch))
        pending!!.resume(
            ApiResult.Success(
                RepoFileContent(
                    "README.md",
                    "README.md",
                    "old readme",
                    null,
                    null,
                    null
                )
            )
        )
        assertNull(vm.state.value.readme.preview)
        assertEquals("next", vm.state.value.selectedRef?.name)
    }

    private class FakeRepository : RepoDetailRepository {
        var detailCalls = 0
        var detailFactory: (String, String) -> RepoDetail =
            { owner, repo -> RepoDetail(owner, repo, repo, defaultBranch = "develop") }
        val directories = mutableListOf<Pair<String, String>>()
        var directory: suspend (String, String) -> ApiResult<List<RepoEntry>> =
            { _, _ -> ApiResult.Success(emptyList()) }
        val commits = ArrayDeque<ApiResult<PagedItems<CommitSummary>>>()
        val commitPages = mutableListOf<Int>()
        var branchCalls = 0
        var tagCalls = 0
        override suspend fun getDetail(owner: String, repo: String): ApiResult<RepoDetail> {
            detailCalls++
            return ApiResult.Success(detailFactory(owner, repo))
        }

        override suspend fun getDirectory(
            owner: String,
            repo: String,
            path: String,
            ref: String
        ): ApiResult<List<RepoEntry>> {
            directories += path to ref
            return directory(path, ref)
        }

        override suspend fun listCommits(
            owner: String,
            repo: String,
            ref: String,
            page: Int,
            perPage: Int
        ): ApiResult<PagedItems<CommitSummary>> {
            commitPages += page
            return if (commits.isEmpty()) ApiResult.Success(PagedItems(emptyList())) else commits.removeFirst()
        }

        override suspend fun listBranches(
            owner: String,
            repo: String,
            page: Int,
            perPage: Int
        ): ApiResult<PagedItems<RepoRef>> {
            branchCalls++
            return ApiResult.Success(PagedItems(listOf(RepoRef("develop", RepoRefKind.Branch))))
        }

        override suspend fun listTags(
            owner: String,
            repo: String,
            page: Int,
            perPage: Int
        ): ApiResult<PagedItems<RepoRef>> {
            tagCalls++
            return ApiResult.Success(PagedItems(emptyList()))
        }
    }

    private class FakeProfiles : UserProfileRepository {
        val calls = mutableListOf<String>()
        var loader: suspend (String) -> ApiResult<UserProfile> =
            { ApiResult.Failure(ApiError.NotFound) }

        override suspend fun get(login: String): ApiResult<UserProfile> {
            calls += login
            return loader(login)
        }

        override suspend fun counts(login: String) = ProfileCounts()
    }

    private class FakeContents : RepoContentRepository {
        val calls = mutableListOf<Pair<String, String>>()
        val imageCalls = mutableListOf<Pair<String, String>>()
        var result: ApiResult<RepoFileContent> = ApiResult.Failure(ApiError.NotFound)
        var loader: (suspend () -> ApiResult<RepoFileContent>)? = null
        override suspend fun getFile(
            owner: String,
            repo: String,
            path: String,
            ref: String
        ): ApiResult<RepoFileContent> {
            calls += path to ref
            return loader?.invoke() ?: result
        }

        override suspend fun getFileBytes(
            owner: String,
            repo: String,
            path: String,
            ref: String
        ): ApiResult<ByteArray> {
            imageCalls += path to ref
            return ApiResult.Failure(ApiError.NotFound)
        }
    }
}
