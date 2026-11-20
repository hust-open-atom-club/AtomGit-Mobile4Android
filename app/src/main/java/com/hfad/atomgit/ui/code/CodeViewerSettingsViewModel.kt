package com.hfad.atomgit.ui.code

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hfad.atomgit.data.model.CodeViewerPreferences
import com.hfad.atomgit.data.repository.CodeViewerPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CodeViewerSettingsViewModel @Inject constructor(
    private val preferencesRepository: CodeViewerPreferencesRepository,
) : ViewModel() {

    val preferences: StateFlow<CodeViewerPreferences> = preferencesRepository.preferences

    fun setShowLineNumbers(enabled: Boolean) {
        if (preferences.value.showLineNumbers == enabled) return
        viewModelScope.launch { preferencesRepository.setShowLineNumbers(enabled) }
    }

    fun setAlwaysDarkTheme(enabled: Boolean) {
        if (preferences.value.alwaysDarkTheme == enabled) return
        viewModelScope.launch { preferencesRepository.setAlwaysDarkTheme(enabled) }
    }

    fun setOverrideSystemFontSize(enabled: Boolean) {
        if (preferences.value.overrideSystemFontSize == enabled) return
        viewModelScope.launch { preferencesRepository.setOverrideSystemFontSize(enabled) }
    }

    fun setFontSizeSp(sizeSp: Int) {
        if (preferences.value.fontSizeSp == sizeSp) return
        viewModelScope.launch { preferencesRepository.setFontSizeSp(sizeSp) }
    }

    fun setWordWrap(enabled: Boolean) {
        if (preferences.value.wordWrap == enabled) return
        viewModelScope.launch { preferencesRepository.setWordWrap(enabled) }
    }
}
