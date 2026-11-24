package com.hfad.atomgit.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AtomGitLinksTest {

    @Test fun parsesRepoUrl() {
        assertEquals(AtomGitLink.Repo("potato-security", "PotatoTool"), parseAtomGitLink("https://atomgit.com/potato-security/PotatoTool"))
    }

    @Test fun stripsGitSuffixAndQuery() {
        assertEquals(AtomGitLink.Repo("o", "r"), parseAtomGitLink("https://atomgit.com/o/r.git?x=1"))
    }

    @Test fun parsesFileUrl() {
        assertEquals(
            AtomGitLink.File("o", "r", "main", "src/main/kotlin/A.kt"),
            parseAtomGitLink("https://atomgit.com/o/r/blob/main/src/main/kotlin/A.kt"),
        )
    }

    @Test fun decodesEncodedPath() {
        assertEquals(
            AtomGitLink.File("o", "r", "dev", "docs/使用说明.md"),
            parseAtomGitLink("https://atomgit.com/o/r/blob/dev/docs/%E4%BD%BF%E7%94%A8%E8%AF%B4%E6%98%8E.md"),
        )
    }

    @Test fun parsesTreeUrl() {
        assertEquals(
            AtomGitLink.Directory("o", "r", "main", "docs"),
            parseAtomGitLink("https://atomgit.com/o/r/tree/main/docs"),
        )
    }

    @Test fun unknownRepositoryPagesFallBackToBrowser() {
        assertNull(parseAtomGitLink("https://atomgit.com/o/r/issues/1"))
        assertNull(parseAtomGitLink("https://atomgit.com/o/r/settings"))
    }

    @Test fun ignoresOtherHostsAndSchemes() {
        assertNull(parseAtomGitLink("https://example.com/o/r"))
        assertNull(parseAtomGitLink("ftp://atomgit.com/o/r"))
        assertNull(parseAtomGitLink("not a url"))
    }
}
