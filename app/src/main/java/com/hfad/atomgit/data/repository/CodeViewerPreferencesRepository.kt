package com.hfad.atomgit.data.repository

import com.hfad.atomgit.data.model.CodeViewerPreferences
import kotlinx.coroutines.flow.StateFlow

/** 代码查看显示偏好。 */
interface CodeViewerPreferencesRepository {
    val preferences: StateFlow<CodeViewerPreferences>

    suspend fun setShowLineNumbers(enabled: Boolean)

    suspend fun setAlwaysDarkTheme(enabled: Boolean)

    suspend fun setOverrideSystemFontSize(enabled: Boolean)

    suspend fun setFontSizeSp(sizeSp: Int)

    suspend fun setWordWrap(enabled: Boolean)
}
