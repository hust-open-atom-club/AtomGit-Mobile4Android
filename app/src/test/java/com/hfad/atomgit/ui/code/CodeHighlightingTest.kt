package com.hfad.atomgit.ui.code

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Test

class CodeHighlightingTest {

    @Test fun splitByLinesKeepsPerLineStyles() {
        val styled = buildAnnotatedString {
            append("val a\nval b")
            addStyle(SpanStyle(color = androidx.compose.ui.graphics.Color.Red), 0, 2)
        }
        val lines = styled.splitByLines()
        assertEquals(2, lines.size)
        assertEquals("val a", lines[0].text)
        assertEquals("val b", lines[1].text)
        assertEquals(1, lines[0].spanStyles.size)
        assertEquals(0, lines[1].spanStyles.size)
    }

    @Test fun splitByLinesHandlesTrailingNewline() {
        val lines = AnnotatedString("a\nb\n").splitByLines()
        assertEquals(listOf("a", "b", ""), lines.map { it.text })
    }

    @Test fun genericLanguageProxiesPlainCode() {
        val annotated = highlightCodeText("plain text", CodeLanguage.Generic, dark = true)
        assertEquals("plain text", annotated.text)
    }
}
