package com.hfad.atomgit.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.hfad.atomgit.data.model.ThemeMode
import com.hfad.atomgit.di.ApplicationScope
import com.hfad.atomgit.di.ThemePreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.io.IOException
import javax.inject.Inject

class DefaultThemePreferencesRepository @Inject constructor(
    @param:ThemePreferences private val dataStore: DataStore<Preferences>,
    @param:ApplicationScope private val scope: CoroutineScope,
) : ThemePreferencesRepository {

    override val themeMode: StateFlow<ThemeMode> = dataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { prefs -> ThemeMode.fromStorage(prefs[KEY_THEME_MODE]) }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = ThemeMode.System,
        )

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode.name
        }
    }

    private companion object {
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
    }
}
