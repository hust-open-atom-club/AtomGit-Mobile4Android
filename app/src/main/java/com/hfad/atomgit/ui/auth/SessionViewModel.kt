package com.hfad.atomgit.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hfad.atomgit.data.repository.LoginFailure
import com.hfad.atomgit.data.repository.LoginResult
import com.hfad.atomgit.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 登录页状态 */
data class LoginUiState(
    val token: String = "",
    val submitting: Boolean = false,
    val failure: LoginFailure? = null,
) {
    val canSubmit: Boolean get() = token.isNotBlank() && !submitting
}

/**
 * 会话门禁的根 ViewModel
 */
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val repository: SessionRepository,
) : ViewModel() {

    private val restored = MutableStateFlow(false)

    private val _loginState = MutableStateFlow(LoginUiState())
    val loginState: StateFlow<LoginUiState> = _loginState.asStateFlow()

    val uiState: StateFlow<SessionUiState> = combine(
        repository.session,
        repository.expired,
        restored,
    ) { session, expired, done ->
        when {
            !done -> SessionUiState.Restoring
            session != null -> SessionUiState.LoggedIn(session)
            else -> SessionUiState.LoggedOut(expired)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = SessionUiState.Restoring,
    )

    init {
        viewModelScope.launch {
            try {
                repository.restore()
            } finally {
                restored.value = true
            }
        }
    }

    fun onTokenChange(token: String) {
        _loginState.update { it.copy(token = token, failure = null) }
    }

    fun login() {
        val current = _loginState.value
        val token = current.token.trim()
        if (token.isEmpty() || current.submitting) return
        _loginState.update { it.copy(submitting = true, failure = null) }
        viewModelScope.launch {
            when (val result = repository.login(token)) {
                // 成功后立即清掉输入框里的令牌。
                LoginResult.Success -> _loginState.value = LoginUiState()
                is LoginResult.Failure -> _loginState.update {
                    it.copy(submitting = false, failure = result.reason)
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch { repository.logout() }
    }
}
