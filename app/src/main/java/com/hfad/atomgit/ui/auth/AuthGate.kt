package com.hfad.atomgit.ui.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hfad.atomgit.navigation.AtomGitNavHost

/**
 * 登录门禁放在导航图外层：没有可用会话时只显示登录页，恢复中显示整页加载。
 */
@Composable
fun AuthGate(
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SessionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val loginState by viewModel.loginState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        SessionUiState.Restoring -> Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(modifier = Modifier.testTag("session-loading"))
        }

        is SessionUiState.LoggedOut -> LoginScreen(
            state = loginState,
            expired = state.expired,
            onTokenChange = viewModel::onTokenChange,
            onSubmit = viewModel::login,
            modifier = modifier,
        )

        is SessionUiState.LoggedIn -> AtomGitNavHost(
            onExit = onExit,
            modifier = modifier,
        )
    }
}
