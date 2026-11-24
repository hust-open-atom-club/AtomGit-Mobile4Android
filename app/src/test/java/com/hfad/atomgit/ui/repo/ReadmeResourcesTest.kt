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

    @Test fun rewritesFileLinksAndPreservesImageSourceForLazyLoading() {
        val markdown = "[指南](CONTRIBUTING.md) ![a](docs/a.png) [章节](#安装)"
        val result = rewriteReadme(
            markdown = markdown,
            baseDir = "",
            owner = "o",
            repo = "r",
            ref = "main",
            filePath = "README.md",
        )
        assertEquals(
            "[指南](https://atomgit.com/o/r/blob/main/CONTRIBUTING.md) " +
                "![a](docs/a.png) " +
                "[章节](https://atomgit.com/o/r/blob/main/README.md#安装)",
            result,
        )
    }

}
