package com.hfad.atomgit.data.repository

import com.hfad.atomgit.data.local.security.TokenCipher
import com.hfad.atomgit.data.local.session.SESSION_STORAGE_VERSION
import com.hfad.atomgit.data.local.session.SessionStore
import com.hfad.atomgit.data.local.session.StoredSession
import com.hfad.atomgit.data.model.AuthSession
import com.hfad.atomgit.data.remote.api.CurrentUserApi
import com.hfad.atomgit.data.remote.auth.AccessTokenProvider
import com.hfad.atomgit.data.remote.auth.SessionExpiryNotifier
import com.hfad.atomgit.di.ApplicationScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import kotlin.io.encoding.Base64

/** [SessionRepository] 实现；登录、退出、401 失效都在 [mutex] 内串行处理。 */
class DefaultSessionRepository @Inject constructor(
    private val currentUserApi: CurrentUserApi,
    private val cipher: TokenCipher,
    private val store: SessionStore,
    private val tokenProvider: AccessTokenProvider,
    private val expiryNotifier: SessionExpiryNotifier,
    @param:ApplicationScope private val scope: CoroutineScope,
) : SessionRepository {

    private val mutex = Mutex()

    private val _session = MutableStateFlow<AuthSession?>(null)
    override val session: StateFlow<AuthSession?> = _session.asStateFlow()

    private val _expired = MutableStateFlow(false)
    override val expired: StateFlow<Boolean> = _expired.asStateFlow()

    init {
        scope.launch {
            expiryNotifier.events.collect { expireSession() }
        }
    }

    override suspend fun restore() = mutex.withLock {
        if (_session.value != null) return@withLock
        val stored = store.read() ?: return@withLock
        if (stored.version != SESSION_STORAGE_VERSION) {
            store.clear()
            return@withLock
        }
        val token = decodeCiphertext(stored.ciphertext)?.let { cipher.decrypt(it) }
        if (token == null) {
            // 密钥失效或密文损坏：清掉密文，要求重新登录
            store.clear()
            tokenProvider.clear()
            return@withLock
        }
        tokenProvider.setToken(token)
        _session.value = stored.toAuthSession()
    }

    override suspend fun login(rawToken: String): LoginResult {
        val token = rawToken.trim()
        if (token.isEmpty()) return LoginResult.Failure(LoginFailure.InvalidToken)
        return mutex.withLock {
            tokenProvider.setToken(token)
            try {
                val user = currentUserApi.currentUser()
                val encrypted = cipher.encrypt(token)
                if (encrypted == null) {
                    tokenProvider.clear()
                    return@withLock LoginResult.Failure(LoginFailure.Unexpected)
                }
                val persisted = try {
                    store.write(
                        StoredSession(
                            version = SESSION_STORAGE_VERSION,
                            accountId = user.id,
                            login = user.login,
                            name = user.name,
                            avatarUrl = user.avatarUrl,
                            ciphertext = encodeCiphertext(encrypted),
                            savedAt = System.currentTimeMillis(),
                        ),
                    )
                    true
                } catch (e: IOException) {
                    false
                }
                if (!persisted) {
                    tokenProvider.clear()
                    return@withLock LoginResult.Failure(LoginFailure.Unexpected)
                }
                _expired.value = false
                _session.value = AuthSession(
                    accountId = user.id,
                    login = user.login,
                    name = user.name,
                    avatarUrl = user.avatarUrl,
                )
                LoginResult.Success
            } catch (e: CancellationException) {
                tokenProvider.clear()
                throw e
            } catch (e: HttpException) {
                tokenProvider.clear()
                LoginResult.Failure(
                    if (e.code() == 401) LoginFailure.InvalidToken else LoginFailure.Unexpected,
                )
            } catch (e: IOException) {
                tokenProvider.clear()
                LoginResult.Failure(LoginFailure.Network)
            } catch (e: Exception) {
                tokenProvider.clear()
                LoginResult.Failure(LoginFailure.Unexpected)
            }
        }
    }

    override suspend fun logout() = mutex.withLock {
        store.clear()
        tokenProvider.clear()
        _expired.value = false
        _session.value = null
    }

    /** 401 收集器；无会话时忽略，重复触发幂等。 */
    private suspend fun expireSession() {
        mutex.withLock {
            if (_session.value == null) return@withLock
            store.clear()
            tokenProvider.clear()
            _expired.value = true
            _session.value = null
        }
    }

    private fun encodeCiphertext(payload: ByteArray): String = Base64.encode(payload)

    private fun decodeCiphertext(text: String): ByteArray? =
        runCatching { Base64.decode(text) }.getOrNull()

    private fun StoredSession.toAuthSession() = AuthSession(
        accountId = accountId,
        login = login,
        name = name,
        avatarUrl = avatarUrl,
    )
}
