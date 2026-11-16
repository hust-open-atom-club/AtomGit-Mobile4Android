package com.hfad.atomgit.ui.code

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyntaxHighlighterTest {

    @Test
    fun languageFromPath() {
        assertEquals(CodeLanguage.Kotlin, codeLanguageFromPath("a/b/Main.kt"))
        assertEquals(CodeLanguage.Java, codeLanguageFromPath("Foo.java"))
        assertEquals(CodeLanguage.Generic, codeLanguageFromPath("README.md"))
    }

    @Test
    fun highlightProducesNonEmptyAnnotatedString() {
        val colors = syntaxColorsFor(dark = false)
        val result = highlightCode(
            code = "fun greet(name: String) = \"hi\" // comment",
            language = CodeLanguage.Kotlin,
            colors = colors,
        )
        assertTrue(result.length > 0)
        assertEquals(
            "fun greet(name: String) = \"hi\" // comment",
            result.text,
        )
        assertTrue(result.spanStyles.isNotEmpty())
    }

    @Test
    fun emptyCodeReturnsEmpty() {
        val result = highlightCode("", CodeLanguage.Generic, syntaxColorsFor(true))
        assertEquals("", result.text)
        assertEquals(Color(0xFFE8E8E8), syntaxColorsFor(true).plain)
    }
}
