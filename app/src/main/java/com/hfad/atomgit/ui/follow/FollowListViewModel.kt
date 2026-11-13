package com.hfad.atomgit.ui.follow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hfad.atomgit.data.model.FollowKind
import com.hfad.atomgit.data.model.UserSummary
import com.hfad.atomgit.data.remote.error.ApiError
import com.hfad.atomgit.data.remote.error.ApiResult
import com.hfad.atomgit.data.repository.FollowRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val PerPage = 30

data class FollowListUiState(
    val login: String = "",
    val kind: FollowKind = FollowKind.Followers,
    val items: List<UserSummary> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    /** 上一页返回空数组，没有下一页了。 */
    val endReached: Boolean = false,
    val error: ApiError? = null,
)

@HiltViewModel
class FollowListViewModel @Inject constructor(
    private val repository: FollowRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(FollowListUiState())
    val state: StateFlow<FollowListUiState> = _state.asStateFlow()

    /** 已成功加载到的页码，失败时不推进。 */
    private var page = 0
    private var job: Job? = null

    fun start(login: String, kind: FollowKind) {
        if (_state.value.login == login && _state.value.kind == kind) return
        _state.value = FollowListUiState(login = login, kind = kind)
        load(reset = true)
    }

    fun selectKind(kind: FollowKind) {
        if (kind == _state.value.kind) return
        _state.value = _state.value.copy(
            kind = kind,
            items = emptyList(),
            endReached = false,
            error = null,
        )
        load(reset = true)
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

    private fun load(reset: Boolean) {
        val snapshot = _state.value
        if (snapshot.login.isBlank()) return

        job?.cancel()
        val targetPage = if (reset) 1 else page + 1
        _state.value = if (reset) {
            snapshot.copy(isLoading = true, isLoadingMore = false, error = null)
        } else {
            snapshot.copy(isLoadingMore = true, error = null)
        }

        job = viewModelScope.launch {
            when (val result = repository.list(snapshot.login, snapshot.kind, targetPage, PerPage)) {
                is ApiResult.Success -> {
                    page = targetPage
                    val known = if (reset) {
                        emptySet()
                    } else {
                        _state.value.items.mapTo(mutableSetOf()) { it.login }
                    }
                    // 关注关系会变，页码分页可能重复返回同一人
                    val fresh = result.value.items.filterNot { it.login in known }
                    _state.value = _state.value.copy(
                        items = if (reset) fresh else _state.value.items + fresh,
                        isLoading = false,
                        isLoadingMore = false,
                        endReached = result.value.items.isEmpty(),
                        error = null,
                    )
                }
                is ApiResult.Failure -> _state.value = _state.value.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    error = result.error,
                )
            }
        }
    }
}
