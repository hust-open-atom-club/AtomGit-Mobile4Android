package com.hfad.atomgit.ui.repo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hfad.atomgit.data.model.CommitSummary
import com.hfad.atomgit.data.model.PagedItems
import com.hfad.atomgit.data.model.RepoDetail
import com.hfad.atomgit.data.model.RepoEntry
import com.hfad.atomgit.data.model.RepoEntryKind
import com.hfad.atomgit.data.model.RepoRef
import com.hfad.atomgit.data.model.RepoRefKind
import com.hfad.atomgit.data.remote.error.ApiError
import com.hfad.atomgit.data.remote.error.ApiResult
import com.hfad.atomgit.data.repository.RepoContentRepository
import com.hfad.atomgit.data.repository.RepoDetailRepository
import com.hfad.atomgit.data.repository.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class RepoDetailTab(val label: String) { Files("文件"), Commits("提交"), Branches("分支"), Tags("标签") }

data class RepoPageState<T>(
    val items: List<T> = emptyList(),
    val loaded: Boolean = false,
    val page: Int = 0,
    // 重试使用失败页码，刷新失败时为第一页
    val failedPage: Int? = null,
    val endReached: Boolean = false,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: ApiError? = null,
)

data class RepoDirectoryState(
    val path: String = "",
    val entries: List<RepoEntry> = emptyList(),
    val loaded: Boolean = false,
    val isLoading: Boolean = false,
    val error: ApiError? = null,
)

data class RepoReadmeState(
    val path: String? = null,
    val preview: String? = null,
    val images: Map<String, ByteArray> = emptyMap(),
    val loaded: Boolean = false,
    val isLoading: Boolean = false,
    val error: ApiError? = null,
)

data class RepoDetailUiState(
    val owner: String = "",
    val repo: String = "",
    val detail: RepoDetail? = null,
    val isLoading: Boolean = false,
    val error: ApiError? = null,
    val tab: RepoDetailTab = RepoDetailTab.Files,
    val selectedRef: RepoRef? = null,
    val files: RepoDirectoryState = RepoDirectoryState(),
    val readme: RepoReadmeState = RepoReadmeState(),
    val commits: RepoPageState<CommitSummary> = RepoPageState(),
    val branches: RepoPageState<RepoRef> = RepoPageState(),
    val tags: RepoPageState<RepoRef> = RepoPageState(),
)

@HiltViewModel
class RepoDetailViewModel @Inject constructor(
    private val repository: RepoDetailRepository,
    private val contentRepository: RepoContentRepository,
    private val userProfileRepository: UserProfileRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(RepoDetailUiState())
    val state = _state.asStateFlow()
    // 切换仓库或版本后忽略旧响应
    private var repoEpoch = 0L
    private var refEpoch = 0L
    private var directoryRequest = 0L
    private var headerRequest = 0L
    private var readmeRequest = 0L
    private val pageRequests = mutableMapOf<RepoDetailTab, Long>()
    private var headerJob: Job? = null
    private var ownerJob: Job? = null
    private var directoryJob: Job? = null
    private var readmeJob: Job? = null
    private val pageJobs = mutableMapOf<RepoDetailTab, Job>()
    private val directories = linkedMapOf<String, RepoDirectoryState>()

    fun start(owner: String, repo: String) {
        if (_state.value.owner == owner && _state.value.repo == repo) return
        repoEpoch++
        refEpoch++
        headerJob?.cancel()
        ownerJob?.cancel()
        directoryJob?.cancel()
        readmeJob?.cancel()
        pageJobs.values.forEach { it.cancel() }
        directories.clear()
        _state.value = RepoDetailUiState(owner = owner, repo = repo)
        retryHeader()
    }

    fun retryHeader() {
        headerJob?.cancel()
        ownerJob?.cancel()
        val snapshot = _state.value
        val epoch = repoEpoch
        val request = ++headerRequest
        _state.update { it.copy(isLoading = true, error = null) }
        headerJob = viewModelScope.launch {
            when (val result = repository.getDetail(snapshot.owner, snapshot.repo)) {
                is ApiResult.Success -> if (epoch == repoEpoch && request == headerRequest) {
                    val initial = _state.value.selectedRef == null
                    _state.update {
                        it.copy(
                            detail = result.value,
                            isLoading = false,
                            error = null,
                            selectedRef = it.selectedRef ?: RepoRef(
                                result.value.defaultBranch ?: "HEAD", RepoRefKind.Branch,
                                isDefault = result.value.defaultBranch != null,
                            ),
                        )
                    }
                    if (initial) ensureTab()
                    loadOwner(result.value, epoch, request)
                }
                is ApiResult.Failure -> if (epoch == repoEpoch && request == headerRequest) {
                    _state.update { it.copy(isLoading = false, error = result.error) }
                }
            }
        }
    }

    private fun loadOwner(detail: RepoDetail, epoch: Long, request: Long) {
        if (!detail.ownerName.isNullOrBlank() && !detail.ownerAvatarUrl.isNullOrBlank()) return
        ownerJob = viewModelScope.launch {
            val result = userProfileRepository.get(detail.owner)
            if (epoch != repoEpoch || request != headerRequest || result !is ApiResult.Success) return@launch
            val profile = result.value
            _state.update { state ->
                state.copy(detail = state.detail?.copy(
                    ownerName = detail.ownerName ?: profile.name?.takeIf { it.isNotBlank() } ?: profile.login,
                    ownerAvatarUrl = detail.ownerAvatarUrl ?: profile.avatarUrl?.takeIf { it.isNotBlank() },
                ))
            }
        }
    }

    fun selectTab(tab: RepoDetailTab) {
        _state.update { it.copy(tab = tab) }
        ensureTab()
    }

    private fun ensureTab() {
        val current = _state.value
        if (current.selectedRef == null) return
        when (current.tab) {
            RepoDetailTab.Files -> if (!current.files.loaded && !current.files.isLoading) openDirectory(current.files.path)
            RepoDetailTab.Commits -> if (!current.commits.loaded && !current.commits.isLoading) loadCommits(false)
            RepoDetailTab.Branches -> if (!current.branches.loaded && !current.branches.isLoading) loadBranches(false)
            RepoDetailTab.Tags -> if (!current.tags.loaded && !current.tags.isLoading) loadTags(false)
        }
    }

    fun selectRef(ref: RepoRef) {
        val current = _state.value.selectedRef
        if (current?.kind == ref.kind && current.name == ref.name && current.requestRef == ref.requestRef) {
            selectTab(RepoDetailTab.Files)
            openDirectory("")
            return
        }
        refEpoch++
        directoryJob?.cancel()
        readmeJob?.cancel()
        pageJobs[RepoDetailTab.Commits]?.cancel()
        directories.clear()
        _state.update {
            it.copy(
                selectedRef = ref, tab = RepoDetailTab.Files,
                files = RepoDirectoryState(), readme = RepoReadmeState(), commits = RepoPageState(),
            )
        }
        openDirectory("")
    }

    fun openDirectory(path: String, force: Boolean = false) {
        val snapshot = _state.value
        val ref = snapshot.selectedRef ?: return
        val target = path.trim('/')
        directoryJob?.cancel()
        val request = ++directoryRequest
        if (!force) {
            directories[target]?.let { cached ->
                _state.update { it.copy(files = cached) }
                return
            }
        }
        if (force) directories.remove(target)
        val epoch = refEpoch
        val repoToken = repoEpoch
        _state.update { it.copy(files = RepoDirectoryState(path = target, isLoading = true)) }
        directoryJob = viewModelScope.launch {
            val result = repository.getDirectory(snapshot.owner, snapshot.repo, target, ref.requestRef)
            if (epoch != refEpoch || repoToken != repoEpoch || request != directoryRequest) return@launch
            when (result) {
                is ApiResult.Success -> {
                    val loaded = RepoDirectoryState(path = target, entries = result.value, loaded = true)
                    directories[target] = loaded
                    if (directories.size > 32) directories.remove(directories.keys.first())
                    _state.update { it.copy(files = loaded) }
                    if (target.isEmpty()) discoverReadme(result.value, force)
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(files = RepoDirectoryState(path = target, loaded = true, error = result.error))
                }
            }
        }
    }

    fun navigateUp(): Boolean {
        val current = _state.value
        if (current.tab != RepoDetailTab.Files || current.files.path.isEmpty()) return false
        openDirectory(current.files.path.substringBeforeLast('/', ""))
        return true
    }

    private fun discoverReadme(entries: List<RepoEntry>, force: Boolean) {
        val priorities = listOf("readme.md", "readme", "readme.txt", "readme.markdown")
        val candidate = entries.filter { it.kind == RepoEntryKind.File && it.name.lowercase() in priorities }
            .minByOrNull { priorities.indexOf(it.name.lowercase()) }
        if (candidate == null) {
            readmeRequest++
            readmeJob?.cancel()
            _state.update { it.copy(readme = RepoReadmeState(loaded = true)) }
        } else if (force || !_state.value.readme.loaded && !_state.value.readme.isLoading) {
            loadReadme(candidate.path)
        }
    }

    fun retryReadme() { _state.value.readme.path?.let(::loadReadme) }

    private fun loadReadme(path: String) {
        readmeJob?.cancel()
        val snapshot = _state.value
        val ref = snapshot.selectedRef ?: return
        val epoch = refEpoch
        val repoToken = repoEpoch
        val request = ++readmeRequest
        _state.update { it.copy(readme = RepoReadmeState(path = path, isLoading = true)) }
        readmeJob = viewModelScope.launch {
            val result = contentRepository.getFile(snapshot.owner, snapshot.repo, path, ref.requestRef)
            if (epoch != refEpoch || repoToken != repoEpoch || request != readmeRequest) return@launch
            val next = when (result) {
                is ApiResult.Success -> {
                    val prepared = prepareReadme(
                        owner = snapshot.owner,
                        repo = snapshot.repo,
                        ref = ref.requestRef,
                        path = path,
                        content = result.value.content,
                    )
                    RepoReadmeState(
                        path = path,
                        preview = prepared.markdown,
                        images = prepared.images,
                        loaded = true,
                    )
                }
                is ApiResult.Failure -> if (result.error == ApiError.NotFound) RepoReadmeState(loaded = true)
                    else RepoReadmeState(path = path, loaded = true, error = result.error)
            }
            if (epoch != refEpoch || repoToken != repoEpoch || request != readmeRequest) return@launch
            _state.update { it.copy(readme = next) }
        }
    }

    /// 图片与相对链接要换成仓库地址；图片直接取字节，避免走 raw 域名
    private suspend fun prepareReadme(
        owner: String,
        repo: String,
        ref: String,
        path: String,
        content: String,
    ): PreparedReadme {
        val markdown = content.take(MaxReadmeLength)
        val baseDir = path.substringBeforeLast('/', "")
        val imagePaths = collectReadmeImages(markdown, baseDir).take(MaxReadmeImages)
        val loaded = coroutineScope {
            imagePaths.map { imagePath ->
                async {
                    val bytes = (contentRepository.getFileBytes(owner, repo, imagePath, ref) as? ApiResult.Success)?.value
                    if (bytes == null || bytes.isEmpty() || bytes.size > MaxReadmeImageBytes) null
                    else imagePath to bytes
                }
            }.awaitAll()
        }.filterNotNull()
        val links = loaded.associate { (imagePath, _) -> imagePath to blobLink(imagePath) }
        val images = loaded.associate { (imagePath, bytes) -> blobLink(imagePath) to bytes }
        return PreparedReadme(
            markdown = rewriteReadme(markdown, baseDir, owner, repo, ref, path, links),
            images = images,
        )
    }

    private companion object {
        const val MaxReadmeLength = 64_000
        const val MaxReadmeImages = 10
        const val MaxReadmeImageBytes = 2 * 1024 * 1024
    }

    fun refresh() {
        if (_state.value.detail == null) { retryHeader(); return }
        retryHeader()
        when (_state.value.tab) {
            RepoDetailTab.Files -> openDirectory(_state.value.files.path, force = true)
            RepoDetailTab.Commits -> loadCommits(true)
            RepoDetailTab.Branches -> loadBranches(true)
            RepoDetailTab.Tags -> loadTags(true)
        }
    }

    fun retryCurrent() {
        when (_state.value.tab) {
            RepoDetailTab.Files -> openDirectory(_state.value.files.path, force = true)
            RepoDetailTab.Commits -> loadCommits(false)
            RepoDetailTab.Branches -> loadBranches(false)
            RepoDetailTab.Tags -> loadTags(false)
        }
    }

    fun loadMore() {
        val current = _state.value
        val page = when (current.tab) {
            RepoDetailTab.Files -> return
            RepoDetailTab.Commits -> current.commits
            RepoDetailTab.Branches -> current.branches
            RepoDetailTab.Tags -> current.tags
        }
        if (page.error == null) retryCurrent()
    }

    private fun loadCommits(reset: Boolean) {
        val current = _state.value
        val ref = current.selectedRef ?: return
        loadPage(RepoDetailTab.Commits, reset, { it.commits }, { s, p -> s.copy(commits = p) }, { it.sha }) { page ->
            repository.listCommits(current.owner, current.repo, ref.requestRef, page, 30)
        }
    }

    private fun loadBranches(reset: Boolean) {
        val current = _state.value
        loadPage(RepoDetailTab.Branches, reset, { it.branches }, { s, p -> s.copy(branches = p) }, { it.name }) { page ->
            repository.listBranches(current.owner, current.repo, page, 30)
        }
    }

    private fun loadTags(reset: Boolean) {
        val current = _state.value
        loadPage(RepoDetailTab.Tags, reset, { it.tags }, { s, p -> s.copy(tags = p) }, { it.name }) { page ->
            repository.listTags(current.owner, current.repo, page, 30)
        }
    }

    private fun <T> loadPage(
        tab: RepoDetailTab,
        reset: Boolean,
        get: (RepoDetailUiState) -> RepoPageState<T>,
        set: (RepoDetailUiState, RepoPageState<T>) -> RepoDetailUiState,
        key: (T) -> String,
        fetch: suspend (Int) -> ApiResult<PagedItems<T>>,
    ) {
        val current = get(_state.value)
        if (!reset && (current.isLoading || current.isLoadingMore || current.endReached && current.failedPage == null)) return
        pageJobs[tab]?.cancel()
        val target = if (reset) 1 else current.failedPage ?: (current.page + 1)
        val repoToken = repoEpoch
        val refToken = refEpoch
        val request = (pageRequests[tab] ?: 0L) + 1
        pageRequests[tab] = request
        _state.update {
            set(it, current.copy(
                isLoading = target == 1, isLoadingMore = target > 1, error = null,
            ))
        }
        pageJobs[tab] = viewModelScope.launch {
            val result = fetch(target)
            if (repoToken != repoEpoch || pageRequests[tab] != request || tab == RepoDetailTab.Commits && refToken != refEpoch) return@launch
            _state.update { state ->
                val page = get(state)
                set(state, when (result) {
                    is ApiResult.Success -> page.copy(
                        items = ((if (target == 1) emptyList() else page.items) + result.value.items).distinctBy(key),
                        loaded = true, page = target,
                        failedPage = null,
                        // 总页数缺失时，以空页作为分页终止条件
                        endReached = result.value.items.isEmpty() || result.value.totalPage?.let { target >= it } == true,
                        isLoading = false, isLoadingMore = false, error = null,
                    )
                    is ApiResult.Failure -> page.copy(loaded = true, failedPage = target, isLoading = false, isLoadingMore = false, error = result.error)
                })
            }
        }
    }
}
