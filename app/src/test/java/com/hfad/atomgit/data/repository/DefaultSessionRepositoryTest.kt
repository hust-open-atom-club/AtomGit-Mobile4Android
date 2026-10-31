package com.hfad.atomgit.data.repository

import com.hfad.atomgit.data.local.security.TokenCipher
import com.hfad.atomgit.data.local.session.SESSION_STORAGE_VERSION
import com.hfad.atomgit.data.local.session.SessionStore
import com.hfad.atomgit.data.local.session.StoredSession
import com.hfad.atomgit.data.model.AuthSession
import com.hfad.atomgit.data.remote.api.CurrentUserApi
import com.hfad.atomgit.data.remote.auth.AccessTokenProvider
import com.hfad.atomgit.data.remote.auth.SessionExpiryNotifier
import com.hfad.atomgit.data.remote.dto.UserDto
import com.hfad.atomgit.data.session.CurrentAccount
import com.hfad.atomgit.data.session.CurrentAccountSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import kotlin.io.encoding.Base64

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultSessionRepositoryTest {

    private val api = FakeCurrentUserApi()
    private val cipher = FakeTokenCipher()
    private val store = InMemorySessionStore()
    private val tokenProvider = AccessTokenProvider()
    private val accountSource = CurrentAccountSource()
    private val notifier = SessionExpiryNotifier()

    @Test
    fun restoreWithoutStoredSessionLeavesSessionEmpty() = runTest {
        val repository = repository()

        repository.restore()

        assertNull(repository.session.value)
        assertNull(tokenProvider.getToken())
        assertNull(accountSource.get())
    }

    @Test
    fun restoreLoadsPersistedCiphertextIntoMemoryToken() = runTest {
        store.session = storedSession(token = "pat-123")
        val repository = repository()

        repository.restore()

        assertEquals(
            AuthSession(accountId = "u1", login = "alice", name = "Alice"),
            repository.session.value,
        )
        assertEquals("pat-123", tokenProvider.getToken())
        assertEquals(CurrentAccount(id = "u1", login = "alice"), accountSource.get())
    }

    @Test
    fun restoreDeletesSessionWhenCiphertextCannotBeDecrypted() = runTest {
        store.session = storedSession(token = "").copy(
            ciphertext = Base64.encode("not-a-ciphertext".toByteArray()),
        )
        val repository = repository()

        repository.restore()

        assertNull(repository.session.value)
        assertNull(store.session)
        assertNull(tokenProvider.getToken())
    }

    @Test
    fun restoreDeletesSessionWithUnknownFormatVersion() = runTest {
        store.session = storedSession(token = "pat-123").copy(version = SESSION_STORAGE_VERSION + 1)
        val repository = repository()

        repository.restore()

        assertNull(repository.session.value)
        assertNull(store.session)
    }

    @Test
    fun loginPersistsOnlyCiphertextAndCreatesSession() = runTest {
        api.user = UserDto(id = "u1", login = "alice", name = "Alice")
        val repository = repository()

        val result = repository.login("  pat-123  ")

        assertEquals(LoginResult.Success, result)
        assertEquals(
            AuthSession(accountId = "u1", login = "alice", name = "Alice"),
            repository.session.value,
        )
        assertEquals("pat-123", tokenProvider.getToken())
        val persisted = requireNotNull(store.session)
        assertEquals(SESSION_STORAGE_VERSION, persisted.version)
        assertEquals("u1", persisted.accountId)
        assertFalse(persisted.ciphertext.contains("pat-123"))
        assertEquals("pat-123", cipher.decrypt(Base64.decode(persisted.ciphertext)))
    }

    @Test
    fun loginRejectsUnauthorizedTokenWithoutPersisting() = runTest {
        api.error = HttpException(
            Response.error<Any>(401, "".toResponseBody("application/json".toMediaType())),
        )
        val repository = repository()

        val result = repository.login("pat-bad")

        assertEquals(LoginResult.Failure(LoginFailure.InvalidToken), result)
        assertNull(repository.session.value)
        assertNull(store.session)
        assertNull(tokenProvider.getToken())
    }

    @Test
    fun loginReportsNetworkFailureWithoutPersisting() = runTest {
        api.error = IOException("timeout")
        val repository = repository()

        val result = repository.login("pat-123")

        assertEquals(LoginResult.Failure(LoginFailure.Network), result)
        assertNull(store.session)
        assertNull(tokenProvider.getToken())
    }

    @Test
    fun loginRejectsBlankTokenWithoutCallingApi() = runTest {
        val repository = repository()

        val result = repository.login("   ")

        assertEquals(LoginResult.Failure(LoginFailure.InvalidToken), result)
        assertEquals(0, api.calls)
    }

    @Test
    fun unauthorizedBroadcastEndsSession() = runTest {
        api.user = UserDto(id = "u1", login = "alice")
        val repository = repository()
        repository.login("pat-123")

        notifier.notifyExpired()

        assertNull(repository.session.value)
        assertTrue(repository.expired.value)
        assertNull(store.session)
        assertNull(tokenProvider.getToken())
    }

    @Test
    fun unauthorizedDuringLoginAttemptIsIgnored() = runTest {
        api.error = HttpException(
            Response.error<Any>(401, "".toResponseBody("application/json".toMediaType())),
        )
        val repository = repository()

        repository.login("pat-bad")

        assertFalse(repository.expired.value)
    }

    @Test
    fun logoutClearsSessionAndExpiryFlag() = runTest {
        api.user = UserDto(id = "u1", login = "alice")
        val repository = repository()
        repository.login("pat-123")

        repository.logout()

        assertNull(repository.session.value)
        assertFalse(repository.expired.value)
        assertNull(store.session)
        assertNull(tokenProvider.getToken())
    }

    private fun TestScope.repository(): DefaultSessionRepository = DefaultSessionRepository(
        currentUserApi = api,
        cipher = cipher,
        store = store,
        tokenProvider = tokenProvider,
        accountSource = accountSource,
        expiryNotifier = notifier,
        scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler)),
    )

    private fun storedSession(token: String): StoredSession = StoredSession(
        version = SESSION_STORAGE_VERSION,
        accountId = "u1",
        login = "alice",
        name = "Alice",
        ciphertext = Base64.encode(cipher.encryptSync(token)),
        savedAt = 1_700_000_000_000,
    )

    private class FakeCurrentUserApi : CurrentUserApi {
        var user: UserDto = UserDto(id = "u1", login = "alice")
        var error: Throwable? = null
        var calls: Int = 0

        override suspend fun currentUser(): UserDto {
            calls++
            error?.let { throw it }
            return user
        }
    }

    /**
     * 只是把明文字符反转，避免测试断言「密文里没有明文」时被假实现误导；
     * 真实实现见 [com.hfad.atomgit.data.local.security.KeystoreTokenCipher]。
     */
    private class FakeTokenCipher : TokenCipher {
        override suspend fun encrypt(plaintext: String): ByteArray = encryptSync(plaintext)

        override suspend fun decrypt(payload: ByteArray): String? {
            val text = payload.decodeToString()
            if (!text.startsWith(PREFIX)) return null
            return text.removePrefix(PREFIX).reversed()
        }

        fun encryptSync(plaintext: String): ByteArray = "$PREFIX${plaintext.reversed()}".toByteArray()

        private companion object {
            const val PREFIX = "enc:"
        }
    }

    private class InMemorySessionStore : SessionStore {
        var session: StoredSession? = null

        override suspend fun read(): StoredSession? = session

        override suspend fun write(session: StoredSession) {
            this.session = session
        }

        override suspend fun clear() {
            session = null
        }
    }
}
