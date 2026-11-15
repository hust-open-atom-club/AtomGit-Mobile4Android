package com.hfad.atomgit.data.repository

import com.hfad.atomgit.data.model.ThemeMode
import kotlinx.coroutines.flow.StateFlow

/** 外观偏好；默认跟随系统。 */
interface ThemePreferencesRepository {
    val themeMode: StateFlow<ThemeMode>

    suspend fun setThemeMode(mode: ThemeMode)
}
