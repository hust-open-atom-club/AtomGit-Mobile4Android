package com.hfad.atomgit.ui.auth

import com.hfad.atomgit.data.model.AuthSession

/**
 * 登录门禁的界面状态
 */
sealed interface SessionUiState {
    data object Restoring : SessionUiState

    data class LoggedOut(val expired: Boolean) : SessionUiState

    data class LoggedIn(val session: AuthSession) : SessionUiState
}
