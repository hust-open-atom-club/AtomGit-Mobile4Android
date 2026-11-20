package com.hfad.atomgit.data.repository

import com.hfad.atomgit.data.model.RepoEntryKind
import com.hfad.atomgit.data.model.RepoRefKind
import com.hfad.atomgit.data.remote.RemoteTestFixture
import com.hfad.atomgit.data.remote.error.ApiError
import com.hfad.atomgit.data.remote.error.ApiResult
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class RepoDetailRepositoryTest {
    private lateinit var fixture: RemoteTestFixture
    private lateinit var repository: RepoDetailRepository

    @Before fun setUp() {
        fixture = RemoteTestFixture()
        fixture.tokenProvider.setToken("test-pat")
        repository = DefaultRepoDetailRepository(fixture.v5Service())
    }

    @After fun tearDown() { fixture.close() }

    private fun respond(body: String, code: Int = 200) {
        fixture.server.enqueue(MockResponse().setResponseCode(code).setBody(body))
    }

    private fun takeRequest() = requireNotNull(fixture.server.takeRequest(5, TimeUnit.SECONDS))

    @Test fun detailUsesRouteIdentityAndWebUrlFallback() = runTest {
        respond("""{"name":"展示名","full_name":"组织 / 展示名","default_branch":"develop","private":true,"stargazers_count":8,"web_url":"https://atomgit.com/team/project"}""")
        val detail = (repository.getDetail("team", "project") as ApiResult.Success).value
        assertEquals("team", detail.owner)
        assertEquals("project", detail.path)
        assertEquals("展示名", detail.name)
        assertEquals("develop", detail.defaultBranch)
        assertEquals("https://atomgit.com/team/project", detail.htmlUrl)
        assertTrue(detail.isPrivate)
        assertEquals(8, detail.stars)
    }

    @Test fun rootDirectoryRequestsTrailingSlashAndPreservesRef() = runTest {
        respond("[]")
        val result = repository.getDirectory("owner", "repo", "", "feature/中文 分支")
        assertEquals(emptyList<Any>(), (result as ApiResult.Success).value)
        val url = takeRequest().requestUrl!!
        assertEquals("/api/v5/repos/owner/repo/contents/", url.encodedPath)
        assertEquals("feature/中文 分支", url.queryParameter("ref"))
    }

    @Test fun directoryEncodesEachPathSegmentOnce() = runTest {
        respond("[]")
        repository.getDirectory("owner", "repo", "中文/hello world/#a%.kt", "main")
        val url = takeRequest().requestUrl!!
        assertEquals("/api/v5/repos/owner/repo/contents/%E4%B8%AD%E6%96%87/hello%20world/%23a%25.kt", url.encodedPath)
    }

    @Test fun directorySortsFoldersFirstAndDoesNotRequireSize() = runTest {
        respond("""[{"type":"file","name":"z.txt","path":"z.txt"},{"type":"dir","name":"src","path":"src"},{"type":"file","path":"a.kt"},{"type":"submodule","path":"lib"}]""")
        val entries = (repository.getDirectory("o", "r", "", "main") as ApiResult.Success).value
        assertEquals(listOf("src", "a.kt", "lib", "z.txt"), entries.map { it.name })
        assertEquals(RepoEntryKind.Directory, entries.first().kind)
        assertEquals(RepoEntryKind.Unsupported, entries[2].kind)
    }

    @Test fun directory404IsNotAnEmptyRepository() = runTest {
        respond("""{"error_code":404,"error_message":"Project or file not found"}""", 404)
        assertEquals(ApiResult.Failure(ApiError.NotFound), repository.getDirectory("o", "r", "missing", "main"))
    }

    @Test fun tags201IsSuccessfulAndRetainsCommitSha() = runTest {
        respond("""[{"name":"v1.0","commit":{"sha":"abc123"}}]""", 201)
        val tags = (repository.listTags("o", "r", 2, 30) as ApiResult.Success).value.items
        assertEquals(RepoRefKind.Tag, tags.single().kind)
        assertEquals("abc123", tags.single().requestRef)
        val url = takeRequest().requestUrl!!
        assertEquals("2", url.queryParameter("page"))
        assertEquals("30", url.queryParameter("per_page"))
    }

    @Test fun emptyTags201HasUnknownTotals() = runTest {
        respond("[]", 201)
        val tags = (repository.listTags("o", "r", 1, 30) as ApiResult.Success).value
        assertTrue(tags.items.isEmpty())
        assertNull(tags.totalCount)
        assertNull(tags.totalPage)
    }

    @Test fun commitsMapFirstLineWithoutRequiringAuthorName() = runTest {
        fixture.server.enqueue(MockResponse().setHeader("total_page", "3").setBody("""[{"sha":"1234567890","commit":{"message":"fix bug\n\nmore details","author":{"email":"a@example.com","date":"2026-11-20T12:00:00Z"}}}]"""))
        val page = (repository.listCommits("o", "r", "feature/a", 2, 30) as ApiResult.Success).value
        assertEquals("fix bug", page.items.single().message)
        assertEquals("2026-11-20T12:00:00Z", page.items.single().date)
        assertEquals(3, page.totalPage)
        val url = takeRequest().requestUrl!!
        assertEquals("feature/a", url.queryParameter("sha"))
        assertEquals("2", url.queryParameter("page"))
    }

    @Test fun branchesPreserveDefaultFlagAndBranchName() = runTest {
        respond("""[{"name":"main","default_branch":true,"commit":{"sha":"abc"}},{"name":"feature/a"}]""")
        val branches = (repository.listBranches("o", "r", 1, 30) as ApiResult.Success).value.items
        assertTrue(branches.first().isDefault)
        assertEquals("main", branches.first().requestRef)
        assertFalse(branches.last().isDefault)
    }
}
