package com.hfad.atomgit.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hfad.atomgit.data.model.RepoSort
import com.hfad.atomgit.data.model.RepoSummary
import com.hfad.atomgit.data.model.RepoVisibility
import com.hfad.atomgit.data.remote.error.ApiError
import com.hfad.atomgit.data.remote.error.ApiResult
import com.hfad.atomgit.data.repository.RepoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val PerPage = 30
private const val SearchDebounceMillis = 300L

data class ProfileReposUiState(
    val query: String = "",
    val visibility: RepoVisibility = RepoVisibility.All,
    val sort: RepoSort = RepoSort.UpdatedDesc,
    val items: List<RepoSummary> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val endReached: Boolean = false,
    val error: ApiError? = null,
)

@HiltViewModel
class ProfileReposViewModel @Inject constructor(
    private val repository: RepoRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileReposUiState())
    val state: StateFlow<ProfileReposUiState> = _state.asStateFlow()

    private var page = 0
    private var job: Job? = null
    private var queryJob: Job? = null

    init {
        load(reset = true)
    }

    fun onQueryChange(query: String) {
        if (query == _state.value.query) return
        _state.value = _state.value.copy(query = query)
        queryJob?.cancel()
        queryJob = viewModelScope.launch {
            delay(SearchDebounceMillis)
            load(reset = true)
        }
    }

    fun selectVisibility(visibility: RepoVisibility) {
        if (visibility == _state.value.visibility) return
        _state.value = _state.value.copy(visibility = visibility)
        load(reset = true)
    }

    fun selectSort(sort: RepoSort) {
        if (sort == _state.value.sort) return
        _state.value = _state.value.copy(sort = sort)
        load(reset = true)
    }

    fun refresh() = load(reset = true)

    fun loadMore() {
        val current = _state.value
        if (current.isLoading || current.isLoadingMore || current.endReached) return
        if (current.error != null) return
        load(reset = false)
    }

    fun retry() = load(reset = _state.value.items.isEmpty())

    private fun load(reset: Boolean) {
        val snapshot = _state.value
        job?.cancel()
        val targetPage = if (reset) 1 else page + 1
        _state.value = if (reset) {
            snapshot.copy(isLoading = true, isLoadingMore = false, error = null)
        } else {
            snapshot.copy(isLoadingMore = true, error = null)
        }

        job = viewModelScope.launch {
            val result = repository.listMine(
                query = snapshot.query,
                visibility = snapshot.visibility,
                sort = snapshot.sort,
                page = targetPage,
                perPage = PerPage,
            )
            when (result) {
                is ApiResult.Success -> {
                    page = targetPage
                    val known = if (reset) {
                        emptySet()
                    } else {
                        _state.value.items.mapTo(mutableSetOf()) { it.key }
                    }
                    val fresh = result.value.items.filterNot { it.key in known }
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

private val RepoSummary.key: String get() = "$ownerPath/$path"
