package com.hfad.atomgit.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeModeTest {

    @Test
    fun fromStorageParsesKnownValues() {
        assertEquals(ThemeMode.System, ThemeMode.fromStorage("System"))
        assertEquals(ThemeMode.Light, ThemeMode.fromStorage("Light"))
        assertEquals(ThemeMode.Dark, ThemeMode.fromStorage("Dark"))
    }

    @Test
    fun fromStorageFallsBackToSystem() {
        assertEquals(ThemeMode.System, ThemeMode.fromStorage(null))
        assertEquals(ThemeMode.System, ThemeMode.fromStorage(""))
        assertEquals(ThemeMode.System, ThemeMode.fromStorage("unknown"))
    }
}
