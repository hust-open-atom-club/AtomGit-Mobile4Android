package com.hfad.atomgit.ui.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hfad.atomgit.data.model.NotificationItem
import com.hfad.atomgit.data.model.RepoSort
import com.hfad.atomgit.data.model.RepoVisibility
import com.hfad.atomgit.data.remote.error.ApiError
import com.hfad.atomgit.data.remote.error.ApiResult
import com.hfad.atomgit.data.remote.error.SourceFailure
import com.hfad.atomgit.data.repository.NotificationRepository
import com.hfad.atomgit.data.repository.RepoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

/**
 * 通知没有跨仓库接口。初版没有选定项目，只拉最近推送的若干仓库，并限制并发，避免一次打开打满限流。
 * 列表一律用 unread=false 拉取；「已读」是收件箱里的客户端筛选。
 */
private const val RepoCap = 8
private const val FetchConcurrency = 3
private const val PerPage = 20

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notifications: NotificationRepository,
    private val repos: RepoRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationUiState())
    val state: StateFlow<NotificationUiState> = _state

    private var loaded: List<NotificationItem> = emptyList()
    private val cursors = mutableMapOf<String, RepoCursor>()
    private val locallyRead = mutableSetOf<String>()
    private val locallyUnread = mutableSetOf<String>()
    private var knownRepos: List<NotificationRepoOption> = emptyList()
    private var job: Job? = null
    private var started = false

    fun start() {
        if (started) return
        started = true
        load(reset = true, reloadRepos = true)
    }

    fun refresh() = load(reset = true, reloadRepos = true)

    fun toggleImportant() {
        _state.value = _state.value.copy(importantOnly = !_state.value.importantOnly)
        publish()
    }

    fun selectKind(filter: NotificationKindFilter) {
        if (filter == _state.value.kindFilter) return
        _state.value = _state.value.copy(kindFilter = filter)
        publish()
    }

    fun selectRepo(key: String?) {
        if (key == _state.value.selectedRepoKey) return
        _state.value = _state.value.copy(selectedRepoKey = key)
        publish()
    }

    fun clearFilters() {
        val current = _state.value
        if (!current.hasActiveFilter) return
        _state.value = current.copy(
            importantOnly = false,
            kindFilter = NotificationKindFilter.All,
            selectedRepoKey = null,
        )
        publish()
    }

    fun loadMore() {
        val current = _state.value
        if (current.isLoading || current.isLoadingMore || current.endReached || current.error != null) return
        load(reset = false, reloadRepos = false)
    }

    fun retry() = load(reset = _state.value.threads.isEmpty(), reloadRepos = true)

    fun markRead(thread: NotificationThread) {
        val ids = thread.unreadIds.ifEmpty { return }
        viewModelScope.launch {
            when (notifications.markRead(thread.owner, thread.repo, ids)) {
                is ApiResult.Success -> {
                    ids.forEach { id ->
                        val key = "${thread.owner}/${thread.repo}/$id"
                        locallyRead += key
                        locallyUnread -= key
                    }
                    publish()
                }
                is ApiResult.Failure -> {
                    _state.value = _state.value.copy(notice = "标为已读失败，仍保留为未读")
                }
            }
        }
    }

    /**
     * 接口只有标已读。标未读先改本地状态，刷新后以服务端为准。
     */
    fun markUnread(thread: NotificationThread) {
        val ids = thread.ids
        if (ids.isEmpty()) return
        ids.forEach { id ->
            val key = "${thread.owner}/${thread.repo}/$id"
            locallyUnread += key
            locallyRead -= key
        }
        loaded = loaded.map { item ->
            if (item.owner == thread.owner && item.repo == thread.repo && item.id in ids) {
                item.copy(unread = true)
            } else {
                item
            }
        }
        publish()
    }

    fun toggleRead(thread: NotificationThread) {
        if (thread.unread) markRead(thread) else markUnread(thread)
    }

    fun showNotice(message: String) {
        _state.value = _state.value.copy(notice = message)
    }

    private fun load(reset: Boolean, reloadRepos: Boolean) {
        job?.cancel()
        _state.value = _state.value.copy(
            isLoading = reset,
            isLoadingMore = !reset,
            error = null,
            notice = null,
        )
        job = viewModelScope.launch {
            val repoList = if (reloadRepos || knownRepos.isEmpty()) {
                when (val listed = repos.listMine(
                    query = "",
                    visibility = RepoVisibility.All,
                    sort = RepoSort.PushedDesc,
                    page = 1,
                    perPage = RepoCap,
                )) {
                    is ApiResult.Failure -> {
                        publish(
                            isLoading = false,
                            isLoadingMore = false,
                            error = if (loaded.isEmpty()) listed.error else null,
                            notice = if (loaded.isEmpty()) null else "仓库列表加载失败",
                        )
                        return@launch
                    }
                    is ApiResult.Success -> listed.value.items.mapNotNull { item ->
                        val owner = item.ownerPath
                        val path = item.path
                        if (owner.isBlank() || path.isBlank()) null else NotificationRepoOption(owner, path)
                    }.distinctBy { it.key }
                }
            } else {
                knownRepos
            }
            knownRepos = repoList
            if (reset) {
                cursors.clear()
                if (reloadRepos) {
                    locallyRead.clear()
                    locallyUnread.clear()
                }
            }
            if (repoList.isEmpty()) {
                loaded = emptyList()
                publish(isLoading = false, isLoadingMore = false, error = null, partialFailures = emptyList())
                return@launch
            }
            val targets = repoList.filter { cursors[it.key]?.endReached != true }
            if (targets.isEmpty()) {
                publish(isLoading = false, isLoadingMore = false, error = null)
                return@launch
            }
            val pages = fetch(targets)
            val failures = mutableListOf<SourceFailure>()
            var accumulated = if (reset) emptyList() else loaded
            pages.forEach { (repo, result) ->
                when (result) {
                    is ApiResult.Success -> {
                        val pageNumber = (cursors[repo.key]?.page ?: 0) + 1
                        val knownIds = accumulated
                            .filter { it.repoKey == repo.key }
                            .mapTo(mutableSetOf()) { it.id }
                        val fresh = result.value.filter { it.id !in knownIds }
                        val endReached = fresh.isEmpty() || result.value.size < PerPage
                        accumulated = accumulated + fresh
                        cursors[repo.key] = RepoCursor(page = pageNumber, endReached = endReached)
                    }
                    is ApiResult.Failure -> failures += SourceFailure(repo.key, result.error)
                }
            }
            val succeeded = pages.size - failures.size
            coroutineContext.ensureActive()
            if (succeeded == 0 && accumulated.isEmpty()) {
                publish(
                    isLoading = false,
                    isLoadingMore = false,
                    error = failures.firstOrNull()?.error ?: ApiError.Unexpected(message = "empty"),
                    partialFailures = emptyList(),
                )
                return@launch
            }
            loaded = accumulated
            publish(
                isLoading = false,
                isLoadingMore = false,
                error = null,
                partialFailures = failures,
            )
        }
    }

    private suspend fun fetch(
        targets: List<NotificationRepoOption>,
    ): List<Pair<NotificationRepoOption, ApiResult<List<NotificationItem>>>> = coroutineScope {
        val gate = Semaphore(FetchConcurrency)
        targets.map { repo ->
            async {
                gate.withPermit {
                    val page = (cursors[repo.key]?.page ?: 0) + 1
                    repo to notifications.listRepo(
                        owner = repo.owner,
                        repo = repo.path,
                        unread = false,
                        page = page,
                        perPage = PerPage,
                    )
                }
            }
        }.awaitAll()
    }

    private fun publish(
        isLoading: Boolean = _state.value.isLoading,
        isLoadingMore: Boolean = _state.value.isLoadingMore,
        error: ApiError? = _state.value.error,
        partialFailures: List<SourceFailure> = _state.value.partialFailures,
        notice: String? = _state.value.notice,
    ) {
        val current = _state.value
        val adjusted = loaded.map { item ->
            when {
                item.readKey in locallyUnread -> item.copy(unread = true)
                item.readKey in locallyRead -> item.copy(unread = false)
                else -> item
            }
        }
        val shown = adjusted
            .filter { !current.importantOnly || it.isImportant() }
            .filter { it.matches(current.kindFilter) }
        val threads = shown.toThreads().filter { thread ->
            current.selectedRepoKey == null || thread.repoKey == current.selectedRepoKey
        }
        val endReached = knownRepos.isEmpty() || knownRepos.all { cursors[it.key]?.endReached == true }
        _state.value = current.copy(
            repos = knownRepos,
            threads = threads,
            isLoading = isLoading,
            isLoadingMore = isLoadingMore,
            endReached = endReached,
            error = error,
            partialFailures = partialFailures,
            notice = notice,
        )
    }

    private data class RepoCursor(val page: Int, val endReached: Boolean)
}
