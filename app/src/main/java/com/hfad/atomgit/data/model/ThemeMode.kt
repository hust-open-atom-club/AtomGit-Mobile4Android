package com.hfad.atomgit.data.model

/** 应用外观偏好。 */
enum class ThemeMode {
    System,
    Light,
    Dark,
    ;

    companion object {
        fun fromStorage(raw: String?): ThemeMode =
            entries.firstOrNull { it.name == raw } ?: System
    }
}
