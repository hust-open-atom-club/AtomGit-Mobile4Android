package com.hfad.atomgit.ui.repo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReadmeResourcesTest {

    @Test fun resolvesRelativePathAgainstReadmeDirectory() {
        assertEquals("docs/a.png", resolveReadmePath("docs", "./a.png"))
        assertEquals("a.png", resolveReadmePath("", "a.png"))
        assertEquals("img/a.png", resolveReadmePath("", "/img/a.png"))
        assertEquals("a/c.png", resolveReadmePath("a/b", "../c.png"))
    }

    @Test fun ignoresExternalAndAnchorTargets() {
        assertNull(resolveReadmePath("", "https://example.com/a.png"))
        assertNull(resolveReadmePath("", "#install"))
        assertNull(resolveReadmePath("", "mailto:a@b.c"))
    }

    @Test fun collectsRelativeImagesOnce() {
        val markdown = """
            ![a](./docs/a.png)
            ![b](docs/a.png)
            <img src="docs/b.png" width="120">
            ![c](https://example.com/c.png)
        """.trimIndent()
        assertEquals(listOf("docs/a.png", "docs/b.png"), collectReadmeImages(markdown, ""))
    }

    @Test fun rewritesImagesAndLinks() {
        val markdown = "[指南](CONTRIBUTING.md) ![a](docs/a.png) [章节](#安装)"
        val result = rewriteReadme(
            markdown = markdown,
            baseDir = "",
            owner = "o",
            repo = "r",
            ref = "main",
            filePath = "README.md",
            imageUrls = mapOf("docs/a.png" to "https://raw.atomgit.com/o/r/blobs/sha/docs/a.png"),
        )
        assertEquals(
            "[指南](https://atomgit.com/o/r/blob/main/CONTRIBUTING.md) " +
                "![a](https://raw.atomgit.com/o/r/blobs/sha/docs/a.png) " +
                "[章节](https://atomgit.com/o/r/blob/main/README.md#安装)",
            result,
        )
    }

    @Test fun keepsUnresolvedImageReference() {
        val result = rewriteReadme(
            markdown = "![a](docs/a.png)",
            baseDir = "",
            owner = "o",
            repo = "r",
            ref = "main",
            filePath = "README.md",
            imageUrls = emptyMap(),
        )
        assertEquals("![a](docs/a.png)", result)
    }
}
