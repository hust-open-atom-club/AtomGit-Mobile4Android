package com.hfad.atomgit.ui.theme

import androidx.lifecycle.ViewModel
import com.hfad.atomgit.data.model.ThemeMode
import com.hfad.atomgit.data.repository.ThemePreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    repository: ThemePreferencesRepository,
) : ViewModel() {
    val themeMode: StateFlow<ThemeMode> = repository.themeMode
}
