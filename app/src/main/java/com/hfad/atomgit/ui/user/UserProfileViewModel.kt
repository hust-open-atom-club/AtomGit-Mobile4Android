package com.hfad.atomgit.ui.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hfad.atomgit.data.model.ProfileCounts
import com.hfad.atomgit.data.model.UserProfile
import com.hfad.atomgit.data.remote.error.ApiError
import com.hfad.atomgit.data.remote.error.ApiResult
import com.hfad.atomgit.data.repository.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserProfileUiState(
    val login: String = "",
    val profile: UserProfile? = null,
    val counts: ProfileCounts = ProfileCounts(),
    val isLoading: Boolean = false,
    val error: ApiError? = null,
)

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val repository: UserProfileRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(UserProfileUiState())
    val state: StateFlow<UserProfileUiState> = _state.asStateFlow()

    private var job: Job? = null

    fun start(login: String) {
        if (_state.value.login == login) return
        _state.value = UserProfileUiState(login = login)
        load()
    }

    fun retry() = load()

    private fun load() {
        val login = _state.value.login
        if (login.isBlank()) return
        job?.cancel()
        _state.value = _state.value.copy(isLoading = true, error = null)
        job = viewModelScope.launch {
            when (val result = repository.get(login)) {
                is ApiResult.Success -> {
                    val counts = repository.counts(login)
                    _state.value = _state.value.copy(
                        profile = result.value,
                        counts = counts,
                        isLoading = false,
                    )
                }
                is ApiResult.Failure -> _state.value = _state.value.copy(
                    isLoading = false,
                    error = result.error,
                )
            }
        }
    }
}
