package com.hfad.atomgit.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hfad.atomgit.data.model.ThemeMode
import com.hfad.atomgit.data.repository.SessionRepository
import com.hfad.atomgit.data.repository.ThemePreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val themePreferences: ThemePreferencesRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = themePreferences.themeMode

    fun setThemeMode(mode: ThemeMode) {
        if (themeMode.value == mode) return
        viewModelScope.launch { themePreferences.setThemeMode(mode) }
    }

    fun logout() {
        viewModelScope.launch { sessionRepository.logout() }
    }
}
