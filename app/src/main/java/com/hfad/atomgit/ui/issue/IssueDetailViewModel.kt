package com.hfad.atomgit.ui.issue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hfad.atomgit.data.model.IssueComment
import com.hfad.atomgit.data.model.IssueDetail
import com.hfad.atomgit.data.remote.error.ApiError
import com.hfad.atomgit.data.remote.error.ApiResult
import com.hfad.atomgit.data.repository.IssueRepository
import com.hfad.atomgit.data.session.CurrentAccountSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal const val IssueCommentPageSize = 30

private const val SyncRereadFailed = "Issue 回读失败，完成状态尚待同步"
private const val SyncCommentUncertain = "评论结果不确定，请刷新确认后再发送"

data class IssueDetailUiState(
    val owner: String = "",
    val repo: String = "",
    val number: Int = 0,
    val issue: IssueDetail? = null,
    val comments: List<IssueComment> = emptyList(),
    val commentPage: Int = 0,
    val commentsEndReached: Boolean = false,
    val draft: String = "",
    val isLoading: Boolean = false,
    val isLoadingComments: Boolean = false,
    val isPosting: Boolean = false,
    val error: ApiError? = null,
    val commentsError: ApiError? = null,
    val postError: ApiError? = null,
    /** 写成功后的回读没能确认时保留原内容，并说明尚待同步。 */
    val syncMessage: String? = null,
)

@HiltViewModel
class IssueDetailViewModel @Inject constructor(
    private val repository: IssueRepository,
    private val accountSource: CurrentAccountSource,
) : ViewModel() {

    private val _state = MutableStateFlow(IssueDetailUiState())
    val state: StateFlow<IssueDetailUiState> = _state.asStateFlow()

    private var routeKey: String? = null
    private var loadJob: Job? = null

    fun start(owner: String, repo: String, number: Int) {
        val key = "$owner/$repo#$number"
        if (routeKey == key) return
        routeKey = key
        loadJob?.cancel()
        _state.value = IssueDetailUiState(owner = owner, repo = repo, number = number)
        refresh()
    }

    fun refresh() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch { load() }
    }

    fun retry() {
        if (_state.value.issue == null) refresh() else retryComments()
    }

    fun updateDraft(value: String) {
        _state.update { it.copy(draft = value, postError = null) }
    }

    fun loadMoreComments() {
        val current = _state.value
        if (current.isLoading || current.isLoadingComments || current.isPosting) return
        if (current.commentsEndReached || current.commentsError != null) return
        if (current.commentPage <= 0) return
        val page = current.commentPage + 1
        _state.update { it.copy(isLoadingComments = true) }
        viewModelScope.launch {
            when (val result = repository.listComments(current.owner, current.repo, current.number, page, IssueCommentPageSize)) {
                is ApiResult.Success -> _state.update {
                    it.copy(
                        isLoadingComments = false,
                        comments = mergeComments(it.comments, result.value),
                        commentPage = page,
                        commentsEndReached = result.value.size < IssueCommentPageSize,
                        commentsError = null,
                    )
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(isLoadingComments = false, commentsError = result.error)
                }
            }
        }
    }

    fun retryComments() {
        val current = _state.value
        if (current.comments.isEmpty()) {
            viewModelScope.launch { reloadFirstComments() }
        } else {
            _state.update { it.copy(commentsError = null) }
            loadMoreComments()
        }
    }

    /**
     * 评论 2xx 只把新评论并入列表，不把 Issue 标成已关闭。
     * 随后回读 Issue；回读失败则保留原详情并标记尚待同步。
     * 写入结果不确定时不自动重发，先回读评论确认。
     */
    fun postComment() {
        val current = _state.value
        val body = current.draft.trim()
        if (body.isEmpty() || current.isPosting || current.issue == null) return
        _state.update { it.copy(isPosting = true, postError = null) }
        viewModelScope.launch {
            when (val result = repository.createComment(current.owner, current.repo, current.number, body)) {
                is ApiResult.Success -> {
                    _state.update {
                        it.copy(
                            isPosting = false,
                            draft = "",
                            comments = mergeComments(it.comments, listOf(result.value)),
                            commentsError = null,
                            postError = null,
                        )
                    }
                    rereadIssue()
                }
                is ApiResult.Failure -> if (result.error == ApiError.WriteUncertain) {
                    confirmUncertainComment(body)
                } else {
                    _state.update { it.copy(isPosting = false, postError = result.error) }
                }
            }
        }
    }

    private suspend fun load() {
        val current = _state.value
        _state.update { it.copy(isLoading = true, error = null, commentsError = null) }
        coroutineScope {
            val issueDeferred = async { repository.get(current.owner, current.repo, current.number) }
            val commentsDeferred = async {
                repository.listComments(current.owner, current.repo, current.number, page = 1, perPage = IssueCommentPageSize)
            }
            val issueResult = issueDeferred.await()
            val commentsResult = commentsDeferred.await()
            _state.update { state ->
                val withIssue = when (issueResult) {
                    is ApiResult.Success -> state.copy(issue = issueResult.value, error = null, syncMessage = null)
                    is ApiResult.Failure -> state.copy(error = issueResult.error)
                }
                when (commentsResult) {
                    is ApiResult.Success -> withIssue.copy(
                        isLoading = false,
                        comments = commentsResult.value,
                        commentPage = 1,
                        commentsEndReached = commentsResult.value.size < IssueCommentPageSize,
                        commentsError = null,
                    )
                    is ApiResult.Failure -> withIssue.copy(
                        isLoading = false,
                        commentsError = commentsResult.error,
                    )
                }
            }
        }
    }

    private suspend fun reloadFirstComments() {
        val current = _state.value
        _state.update { it.copy(isLoadingComments = true, commentsError = null) }
        when (val result = repository.listComments(current.owner, current.repo, current.number, page = 1, perPage = IssueCommentPageSize)) {
            is ApiResult.Success -> _state.update {
                it.copy(
                    isLoadingComments = false,
                    comments = result.value,
                    commentPage = 1,
                    commentsEndReached = result.value.size < IssueCommentPageSize,
                    commentsError = null,
                )
            }
            is ApiResult.Failure -> _state.update {
                it.copy(isLoadingComments = false, commentsError = result.error)
            }
        }
    }

    private suspend fun rereadIssue() {
        val current = _state.value
        when (val result = repository.get(current.owner, current.repo, current.number)) {
            is ApiResult.Success -> _state.update {
                it.copy(issue = result.value, error = null, syncMessage = null)
            }
            is ApiResult.Failure -> _state.update { it.copy(syncMessage = SyncRereadFailed) }
        }
    }

    private suspend fun confirmUncertainComment(body: String) {
        val current = _state.value
        val reloaded = repository.listComments(
            current.owner,
            current.repo,
            current.number,
            page = 1,
            perPage = IssueCommentPageSize,
        )
        val login = accountSource.get()?.login
        when (reloaded) {
            is ApiResult.Success -> {
                val matched = reloaded.value.any { comment ->
                    comment.body?.trim() == body && (login.isNullOrBlank() || comment.authorLogin == login)
                }
                if (matched) {
                    _state.update {
                        it.copy(
                            isPosting = false,
                            comments = reloaded.value,
                            commentPage = 1,
                            commentsEndReached = reloaded.value.size < IssueCommentPageSize,
                            commentsError = null,
                            draft = "",
                            postError = null,
                        )
                    }
                    rereadIssue()
                } else {
                    _state.update {
                        it.copy(
                            isPosting = false,
                            comments = reloaded.value,
                            commentPage = 1,
                            commentsEndReached = reloaded.value.size < IssueCommentPageSize,
                            commentsError = null,
                            syncMessage = SyncCommentUncertain,
                            postError = null,
                        )
                    }
                }
            }
            is ApiResult.Failure -> _state.update {
                it.copy(
                    isPosting = false,
                    commentsError = reloaded.error,
                    syncMessage = SyncCommentUncertain,
                    postError = null,
                )
            }
        }
    }
}

private fun mergeComments(existing: List<IssueComment>, incoming: List<IssueComment>): List<IssueComment> {
    if (incoming.isEmpty()) return existing
    val knownIds = existing.mapNotNull { it.id }.toMutableSet()
    val appended = incoming.filter { comment ->
        val id = comment.id
        id == null || knownIds.add(id)
    }
    return existing + appended
}
