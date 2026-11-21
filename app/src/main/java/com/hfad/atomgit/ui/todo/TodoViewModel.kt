package com.hfad.atomgit.ui.todo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hfad.atomgit.data.model.TodoItem
import com.hfad.atomgit.data.model.TodoPage
import com.hfad.atomgit.data.remote.api.IssueState
import com.hfad.atomgit.data.remote.api.PullState
import com.hfad.atomgit.data.remote.error.ApiResult
import com.hfad.atomgit.data.repository.TodoRepository
import com.hfad.atomgit.data.session.CurrentAccountSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val PerPage = 30

@HiltViewModel
class TodoViewModel @Inject constructor(
    private val repository: TodoRepository,
    private val accountSource: CurrentAccountSource,
) : ViewModel() {

    private val _state = MutableStateFlow(TodoUiState())
    val state: StateFlow<TodoUiState> = _state.asStateFlow()

    private val caches = mutableMapOf<TodoSource, TodoSourceCache>()
    private var job: Job? = null
    private var started = false

    fun start() {
        if (started) return
        started = true
        load(reset = true)
    }

    fun selectGroup(group: TodoGroup) {
        if (group == _state.value.group) return
        val source = TodoSource.defaultsFor(group)
        showCachedOrLoad(group = group, source = source)
    }

    fun selectSource(source: TodoSource) {
        if (source == _state.value.source) return
        showCachedOrLoad(group = source.group, source = source)
    }

    fun refresh() = load(reset = true)

    fun loadMore() {
        val current = _state.value
        if (current.isLoading || current.isLoadingMore || current.endReached) return
        if (current.error != null) return
        load(reset = false)
    }

    /** 首屏失败重来一次；翻页失败重试下一页。 */
    fun retry() = load(reset = _state.value.items.isEmpty())

    private fun showCachedOrLoad(group: TodoGroup, source: TodoSource) {
        job?.cancel()
        val cached = caches[source]
        if (cached != null && cached.page > 0) {
            _state.value = TodoUiState(
                group = group,
                source = source,
                items = cached.items,
                totalCount = cached.totalCount,
                endReached = cached.endReached,
                error = cached.error,
            )
            return
        }
        _state.value = TodoUiState(group = group, source = source)
        load(reset = true)
    }

    private fun load(reset: Boolean) {
        val snapshot = _state.value
        val source = snapshot.source
        job?.cancel()

        val cached = caches[source] ?: TodoSourceCache()
        val targetPage = if (reset) 1 else cached.page + 1
        _state.value = if (reset) {
            snapshot.copy(isLoading = true, isLoadingMore = false, error = null)
        } else {
            snapshot.copy(isLoadingMore = true, error = null)
        }

        job = viewModelScope.launch {
            var pageNumber = targetPage
            var accumulated = if (reset) emptyList() else snapshot.items
            var lastTotalCount: Long? = if (reset) null else cached.totalCount

            while (true) {
                when (val result = fetch(source, pageNumber)) {
                    is ApiResult.Success -> {
                        val page = result.value
                        val login = accountSource.get()?.login
                        val filtered = page.items
                            .mapNotNull { item -> item.takeIfNeedsAction(source, login) }
                        val known = accumulated.mapTo(mutableSetOf()) { it.dedupeKey }
                        val fresh = filtered.filterNot { it.dedupeKey in known }
                        accumulated = accumulated + fresh
                        lastTotalCount = page.totalCount ?: lastTotalCount
                        val endReached = page.items.isEmpty()
                        caches[source] = TodoSourceCache(
                            items = accumulated,
                            totalCount = lastTotalCount,
                            page = pageNumber,
                            endReached = endReached,
                            error = null,
                        )
                        // 本页被客户端滤空但接口还有数据时，继续翻页，避免误显示空列表。
                        if (fresh.isEmpty() && !endReached && page.items.isNotEmpty()) {
                            pageNumber += 1
                            continue
                        }
                        _state.value = _state.value.copy(
                            items = accumulated,
                            totalCount = lastTotalCount,
                            isLoading = false,
                            isLoadingMore = false,
                            endReached = endReached,
                            error = null,
                        )
                        return@launch
                    }
                    is ApiResult.Failure -> {
                        val error = result.error
                        // 失败不推进页码：保留上一成功页，便于 retry 重打同一页。
                        val lastGoodPage = pageNumber - 1
                        if (lastGoodPage > 0) {
                            caches[source] = TodoSourceCache(
                                items = accumulated,
                                totalCount = lastTotalCount,
                                page = lastGoodPage,
                                endReached = false,
                                error = error,
                            )
                        }
                        _state.value = _state.value.copy(
                            items = accumulated,
                            totalCount = lastTotalCount,
                            isLoading = false,
                            isLoadingMore = false,
                            error = error,
                        )
                        return@launch
                    }
                }
            }
        }
    }

    private suspend fun fetch(source: TodoSource, page: Int): ApiResult<TodoPage> =
        when (source) {
            TodoSource.AssignedIssues ->
                repository.listAssignedOpenIssues(page = page, perPage = PerPage)
            TodoSource.NeedMyApprove ->
                repository.listNeedMyApprovePulls(PullState.Open, page = page, perPage = PerPage)
            TodoSource.NeedMyReview ->
                repository.listNeedMyReviewPulls(PullState.Open, page = page, perPage = PerPage)
            TodoSource.CreatedIssues ->
                repository.listCreatedIssues(IssueState.Open, page = page, perPage = PerPage)
            TodoSource.CreatedPulls ->
                repository.listCreatedByMePulls(PullState.Open, page = page, perPage = PerPage)
        }
}

/**
 * 批准 / 评审源在客户端再筛仍需处理的项。
 * 空页终止仍看接口原始数组，过滤后为空不代表没有下一页。
 */
internal fun TodoItem.takeIfNeedsAction(source: TodoSource, login: String?): TodoItem? {
    if (login.isNullOrBlank()) return this
    return when (source) {
        TodoSource.NeedMyApprove -> {
            val mine = assignees.firstOrNull { it.login.equals(login, ignoreCase = true) }
            if (mine == null || mine.accept == true) null else this
        }
        TodoSource.NeedMyReview -> {
            val mine = approvalReviewers.firstOrNull { it.login.equals(login, ignoreCase = true) }
            when {
                mine == null -> null
                mine.accept == true -> null
                mine.acceptStatus.equals("approved", ignoreCase = true) -> null
                else -> this
            }
        }
        else -> this
    }
}
