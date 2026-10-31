package com.hfad.atomgit.data.local.session

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class FileSessionStoreTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val json = Json { ignoreUnknownKeys = true }

    private fun store(): FileSessionStore = FileSessionStore(tempFolder.root, json)

    private fun sessionFile(): File = File(tempFolder.root, "session.json")

    @Test
    fun writesAndReadsBackStoredSession() = runTest {
        val session = StoredSession(
            version = SESSION_STORAGE_VERSION,
            accountId = "u1",
            login = "alice",
            name = "Alice",
            ciphertext = "AAAA",
            savedAt = 1_700_000_000_000,
        )

        store().write(session)

        assertEquals(session, store().read())
    }

    @Test
    fun rewritingExistingSessionReplacesContentAndLeavesNoTempFile() = runTest {
        store().write(
            StoredSession(
                version = SESSION_STORAGE_VERSION,
                accountId = "u1",
                login = "alice",
                ciphertext = "AAAA",
                savedAt = 1,
            ),
        )
        val second = StoredSession(
            version = SESSION_STORAGE_VERSION,
            accountId = "u2",
            login = "bob",
            ciphertext = "BBBB",
            savedAt = 2,
        )

        store().write(second)

        assertEquals(second, store().read())
        assertFalse(File(tempFolder.root, "session.json.tmp").exists())
    }

    @Test
    fun readReturnsNullWhenNothingWasWritten() = runTest {
        assertNull(store().read())
    }

    @Test
    fun corruptedFileIsDeletedInsteadOfFailingEveryStart() = runTest {
        sessionFile().writeText("{ not a session }")

        assertNull(store().read())
        assertFalse(sessionFile().exists())
    }

    @Test
    fun clearRemovesStoredSession() = runTest {
        store().write(
            StoredSession(
                version = SESSION_STORAGE_VERSION,
                accountId = "u1",
                login = "alice",
                ciphertext = "AAAA",
                savedAt = 1,
            ),
        )

        store().clear()

        assertNull(store().read())
        assertFalse(sessionFile().exists())
    }
}
