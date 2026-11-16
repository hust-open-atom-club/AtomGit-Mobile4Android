package com.hfad.atomgit.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Base64

class RepoContentDecodingTest {

    @Test
    fun encodeContentPathKeepsSlashesAndEncodesSegments() {
        assertEquals(
            "src/main/Hello%20World.kt",
            encodeContentPath("src/main/Hello World.kt"),
        )
        assertEquals(
            "a/b",
            encodeContentPath("/a/b/"),
        )
    }

    @Test
    fun decodeBase64Content() {
        val plain = "fun main() {}\n"
        val encoded = Base64.getEncoder().encodeToString(plain.toByteArray())
        val wrapped = encoded.chunked(4).joinToString("\n")

        assertEquals(plain, decodeFileContent(wrapped, "base64"))
        assertEquals(plain, decodeFileContent(wrapped, null))
    }

    @Test
    fun decodeLeavesNonBase64AsIs() {
        val raw = "plain text"
        assertEquals(raw, decodeFileContent(raw, "utf-8"))
    }
}
