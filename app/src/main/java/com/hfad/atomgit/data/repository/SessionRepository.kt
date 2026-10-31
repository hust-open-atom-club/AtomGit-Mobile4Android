package com.hfad.atomgit.data.repository

import com.hfad.atomgit.data.model.AuthSession
import kotlinx.coroutines.flow.StateFlow

/** 单账号 PAT 会话；本地只存密文，401 时结束会话。 */
interface SessionRepository {

    /** `null` = 未登录或尚未恢复完成。 */
    val session: StateFlow<AuthSession?>

    /** 因 401 被动结束会话。 */
    val expired: StateFlow<Boolean>

    suspend fun restore()

    suspend fun login(rawToken: String): LoginResult

    suspend fun logout()
}

/** [SessionRepository.login] 的返回契约。 */
sealed interface LoginResult {
    data object Success : LoginResult
    data class Failure(val reason: LoginFailure) : LoginResult
}

/** 登录失败分类 */
enum class LoginFailure {
    InvalidToken,
    Network,
    Unexpected,
}
