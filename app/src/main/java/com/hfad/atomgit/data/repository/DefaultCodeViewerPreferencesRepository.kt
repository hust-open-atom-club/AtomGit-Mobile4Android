package com.hfad.atomgit.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import com.hfad.atomgit.data.model.CodeFontSizeSteps
import com.hfad.atomgit.data.model.CodeViewerPreferences
import com.hfad.atomgit.data.model.DefaultCodeFontSizeSp
import com.hfad.atomgit.di.ApplicationScope
import com.hfad.atomgit.di.CodeViewerPreferencesStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.io.IOException
import javax.inject.Inject

class DefaultCodeViewerPreferencesRepository @Inject constructor(
    @param:CodeViewerPreferencesStore private val dataStore: DataStore<Preferences>,
    @param:ApplicationScope private val scope: CoroutineScope,
) : CodeViewerPreferencesRepository {

    override val preferences: StateFlow<CodeViewerPreferences> = dataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { prefs ->
            val rawSize = prefs[KEY_FONT_SIZE_SP] ?: DefaultCodeFontSizeSp
            CodeViewerPreferences(
                showLineNumbers = prefs[KEY_SHOW_LINE_NUMBERS] ?: true,
                alwaysDarkTheme = prefs[KEY_ALWAYS_DARK] ?: false,
                overrideSystemFontSize = prefs[KEY_OVERRIDE_FONT] ?: false,
                fontSizeSp = nearestFontSizeStep(rawSize),
                wordWrap = prefs[KEY_WORD_WRAP] ?: false,
            )
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = CodeViewerPreferences(),
        )

    override suspend fun setShowLineNumbers(enabled: Boolean) {
        dataStore.edit { it[KEY_SHOW_LINE_NUMBERS] = enabled }
    }

    override suspend fun setAlwaysDarkTheme(enabled: Boolean) {
        dataStore.edit { it[KEY_ALWAYS_DARK] = enabled }
    }

    override suspend fun setOverrideSystemFontSize(enabled: Boolean) {
        dataStore.edit { it[KEY_OVERRIDE_FONT] = enabled }
    }

    override suspend fun setFontSizeSp(sizeSp: Int) {
        dataStore.edit { it[KEY_FONT_SIZE_SP] = nearestFontSizeStep(sizeSp) }
    }

    override suspend fun setWordWrap(enabled: Boolean) {
        dataStore.edit { it[KEY_WORD_WRAP] = enabled }
    }

    private companion object {
        val KEY_SHOW_LINE_NUMBERS = booleanPreferencesKey("show_line_numbers")
        val KEY_ALWAYS_DARK = booleanPreferencesKey("always_dark_theme")
        val KEY_OVERRIDE_FONT = booleanPreferencesKey("override_system_font_size")
        val KEY_FONT_SIZE_SP = intPreferencesKey("font_size_sp")
        val KEY_WORD_WRAP = booleanPreferencesKey("word_wrap")

        fun nearestFontSizeStep(sizeSp: Int): Int =
            CodeFontSizeSteps.minBy { kotlin.math.abs(it - sizeSp) }
    }
}
