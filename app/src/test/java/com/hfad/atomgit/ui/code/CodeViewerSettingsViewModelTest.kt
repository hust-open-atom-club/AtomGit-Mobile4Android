package com.hfad.atomgit.ui.code

import com.hfad.atomgit.MainDispatcherRule
import com.hfad.atomgit.data.model.CodeViewerPreferences
import com.hfad.atomgit.data.repository.CodeViewerPreferencesRepository
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
class CodeViewerSettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val preferencesRepository = FakeCodeViewerPreferencesRepository()

    @Test
    fun exposesDefaultPreferences() {
        val viewModel = CodeViewerSettingsViewModel(preferencesRepository)

        assertEquals(CodeViewerPreferences(), viewModel.preferences.value)
    }

    @Test
    fun togglesWritePreferences() = runTest {
        val viewModel = CodeViewerSettingsViewModel(preferencesRepository)

        viewModel.setShowLineNumbers(false)
        viewModel.setAlwaysDarkTheme(true)
        viewModel.setOverrideSystemFontSize(true)
        viewModel.setFontSizeSp(16)
        viewModel.setWordWrap(true)
        advanceUntilIdle()

        assertEquals(
            CodeViewerPreferences(
                showLineNumbers = false,
                alwaysDarkTheme = true,
                overrideSystemFontSize = true,
                fontSizeSp = 16,
                wordWrap = true,
            ),
            preferencesRepository.preferences.value,
        )
    }

    @Test
    fun setSkipsWhenUnchanged() = runTest {
        val viewModel = CodeViewerSettingsViewModel(preferencesRepository)

        viewModel.setShowLineNumbers(true)
        viewModel.setFontSizeSp(13)
        advanceUntilIdle()

        assertTrue(preferencesRepository.writeCount == 0)
    }

    private class FakeCodeViewerPreferencesRepository : CodeViewerPreferencesRepository {
        private val _preferences = MutableStateFlow(CodeViewerPreferences())
        override val preferences: StateFlow<CodeViewerPreferences> = _preferences
        var writeCount: Int = 0

        override suspend fun setShowLineNumbers(enabled: Boolean) {
            writeCount++
            _preferences.value = _preferences.value.copy(showLineNumbers = enabled)
        }

        override suspend fun setAlwaysDarkTheme(enabled: Boolean) {
            writeCount++
            _preferences.value = _preferences.value.copy(alwaysDarkTheme = enabled)
        }

        override suspend fun setOverrideSystemFontSize(enabled: Boolean) {
            writeCount++
            _preferences.value = _preferences.value.copy(overrideSystemFontSize = enabled)
        }

        override suspend fun setFontSizeSp(sizeSp: Int) {
            writeCount++
            _preferences.value = _preferences.value.copy(fontSizeSp = sizeSp)
        }

        override suspend fun setWordWrap(enabled: Boolean) {
            writeCount++
            _preferences.value = _preferences.value.copy(wordWrap = enabled)
        }
    }
}
