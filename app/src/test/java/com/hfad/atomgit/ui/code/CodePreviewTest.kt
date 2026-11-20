package com.hfad.atomgit.ui.code

import com.hfad.atomgit.data.model.isKnownBinaryPath
import org.junit.Assert.*
import org.junit.Test

class CodePreviewTest {
    @Test fun binaryFilesAreRecognizedCaseInsensitively() {
        assertTrue(isKnownBinaryPath("docs/image.PNG"))
        assertFalse(isKnownBinaryPath("src/main.kt"))
        assertFalse(prepareCodePreview("abc\u0000def").supported)
    }

    @Test fun normalTextIsUnchanged() {
        val text = "fun main() {\n    println(\"你好\")\n}\n"
        assertEquals(text, prepareCodePreview(text).text)
        assertNull(prepareCodePreview(text).message)
    }

    @Test fun hugeSingleLineIsBounded() {
        val preview = prepareCodePreview("a".repeat(250_000))
        assertEquals(1000, preview.text.length)
        assertNotNull(preview.message)
    }

    @Test fun windowsLineEndingsArePreservedForNormalText() {
        val text = "first\r\nsecond\r\n"
        assertEquals(text, prepareCodePreview(text).text)
        assertNull(prepareCodePreview(text).message)
    }

    @Test fun hugeLineCountIsBounded() {
        val preview = prepareCodePreview((1..4000).joinToString("\n"))
        assertEquals(3000, preview.text.lines().size)
        assertNotNull(preview.message)
    }
}
