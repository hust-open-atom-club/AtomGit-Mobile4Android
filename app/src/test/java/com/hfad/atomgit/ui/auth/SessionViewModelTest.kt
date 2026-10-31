package com.hfad.atomgit.ui.auth

import com.hfad.atomgit.MainDispatcherRule
import com.hfad.atomgit.data.model.AuthSession
import com.hfad.atomgit.data.repository.LoginFailure
import com.hfad.atomgit.data.repository.LoginResult
import com.hfad.atomgit.data.repository.SessionRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeSessionRepository()

    @Test
    fun showsRestoringUntilRestoreCompletes() = runTest {
        val gate = CompletableDeferred<Unit>()
        repository.restoreGate = gate

        val viewModel = SessionViewModel(repository)

        assertEquals(SessionUiState.Restoring, viewModel.uiState.value)

        gate.complete(Unit)

        assertEquals(SessionUiState.LoggedOut(expired = false), viewModel.uiState.value)
    }

    @Test
    fun loggedOutWhenRestoreFindsNothing() = runTest {
        val viewModel = SessionViewModel(repository)

        assertEquals(SessionUiState.LoggedOut(expired = false), viewModel.uiState.value)
    }

    @Test
    fun blankTokenDoesNotCallRepository() = runTest {
        val viewModel = SessionViewModel(repository)
        viewModel.onTokenChange("   ")

        viewModel.login()

        assertEquals(0, repository.loginCalls)
    }

    @Test
    fun successfulLoginClearsTokenInputAndShowsSession() = runTest {
        repository.result = LoginResult.Success
        val viewModel = SessionViewModel(repository)
        viewModel.onTokenChange("pat-123")

        viewModel.login()

        assertEquals(LoginUiState(), viewModel.loginState.value)
        assertEquals("pat-123", repository.lastToken)
        assertEquals(
            SessionUiState.LoggedIn(AuthSession(accountId = "u1", login = "alice")),
            viewModel.uiState.value,
        )
    }

    @Test
    fun rejectedTokenKeepsInputAndShowsFailure() = runTest {
        repository.result = LoginResult.Failure(LoginFailure.InvalidToken)
        val viewModel = SessionViewModel(repository)
        viewModel.onTokenChange("pat-bad")

        viewModel.login()

        val state = viewModel.loginState.value
        assertEquals("pat-bad", state.token)
        assertEquals(LoginFailure.InvalidToken, state.failure)
        assertFalse(state.submitting)
        assertTrue(state.canSubmit)
        assertEquals(SessionUiState.LoggedOut(expired = false), viewModel.uiState.value)
    }

    @Test
    fun editingTokenClearsPreviousFailure() = runTest {
        repository.result = LoginResult.Failure(LoginFailure.Network)
        val viewModel = SessionViewModel(repository)
        viewModel.onTokenChange("pat-bad")
        viewModel.login()

        viewModel.onTokenChange("pat-bad2")

        assertNull(viewModel.loginState.value.failure)
    }

    @Test
    fun logoutDelegatesToRepository() = runTest {
        val viewModel = SessionViewModel(repository)

        viewModel.logout()

        assertTrue(repository.logoutCalled)
        assertEquals(SessionUiState.LoggedOut(expired = false), viewModel.uiState.value)
    }

    private class FakeSessionRepository : SessionRepository {
        override val session = MutableStateFlow<AuthSession?>(null)
        override val expired: StateFlow<Boolean> = MutableStateFlow(false)

        var result: LoginResult = LoginResult.Success
        var loginCalls: Int = 0
        var lastToken: String? = null
        var logoutCalled: Boolean = false
        var restoreGate: CompletableDeferred<Unit>? = null

        override suspend fun restore() {
            restoreGate?.await()
        }

        override suspend fun login(rawToken: String): LoginResult {
            loginCalls++
            lastToken = rawToken
            val outcome = result
            if (outcome is LoginResult.Success) {
                session.value = AuthSession(accountId = "u1", login = "alice")
            }
            return outcome
        }

        override suspend fun logout() {
            logoutCalled = true
            session.value = null
        }
    }
}
