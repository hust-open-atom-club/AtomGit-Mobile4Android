package com.hfad.atomgit.ui.settings

import com.hfad.atomgit.MainDispatcherRule
import com.hfad.atomgit.data.model.AuthSession
import com.hfad.atomgit.data.model.ThemeMode
import com.hfad.atomgit.data.repository.LoginResult
import com.hfad.atomgit.data.repository.SessionRepository
import com.hfad.atomgit.data.repository.ThemePreferencesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val themePreferences = FakeThemePreferencesRepository()
    private val sessionRepository = FakeSessionRepository()

    @Test
    fun exposesDefaultThemeMode() {
        val viewModel = SettingsViewModel(themePreferences, sessionRepository)

        assertEquals(ThemeMode.System, viewModel.themeMode.value)
    }

    @Test
    fun setThemeModeWritesPreference() = runTest {
        val viewModel = SettingsViewModel(themePreferences, sessionRepository)

        viewModel.setThemeMode(ThemeMode.Dark)
        advanceUntilIdle()

        assertEquals(ThemeMode.Dark, themePreferences.themeMode.value)
        assertEquals(listOf(ThemeMode.Dark), themePreferences.writes)
    }

    @Test
    fun setThemeModeSkipsWhenUnchanged() = runTest {
        val viewModel = SettingsViewModel(themePreferences, sessionRepository)

        viewModel.setThemeMode(ThemeMode.System)
        advanceUntilIdle()

        assertTrue(themePreferences.writes.isEmpty())
    }

    @Test
    fun logoutDelegatesToSessionRepository() = runTest {
        val viewModel = SettingsViewModel(themePreferences, sessionRepository)

        viewModel.logout()
        advanceUntilIdle()

        assertTrue(sessionRepository.logoutCalled)
    }

    private class FakeThemePreferencesRepository : ThemePreferencesRepository {
        private val _themeMode = MutableStateFlow(ThemeMode.System)
        override val themeMode: StateFlow<ThemeMode> = _themeMode
        val writes = mutableListOf<ThemeMode>()

        override suspend fun setThemeMode(mode: ThemeMode) {
            writes += mode
            _themeMode.value = mode
        }
    }

    private class FakeSessionRepository : SessionRepository {
        override val session = MutableStateFlow<AuthSession?>(null)
        override val expired: StateFlow<Boolean> = MutableStateFlow(false)
        var logoutCalled: Boolean = false

        override suspend fun restore() = Unit

        override suspend fun login(rawToken: String): LoginResult = LoginResult.Success

        override suspend fun refreshProfile(): Boolean = true

        override suspend fun logout() {
            logoutCalled = true
        }
    }
}
