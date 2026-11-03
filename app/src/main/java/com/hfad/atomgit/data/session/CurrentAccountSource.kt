package com.hfad.atomgit.data.session

import javax.inject.Inject
import javax.inject.Singleton

/** 当前账号标识；[id] 是 `GET /user` 的字符串 id，供缓存键与去重使用。 */
data class CurrentAccount(
    val id: String,
    val login: String,
)

/**
 * 进程内持有当前账号。由会话层在登录 / 恢复时写入，退出或 401 时清空。
 * 不读 Keystore，也不自己请求 `/user`。
 */
@Singleton
class CurrentAccountSource @Inject constructor() {
    @Volatile
    private var account: CurrentAccount? = null

    fun get(): CurrentAccount? = account

    fun require(): CurrentAccount =
        account ?: throw IllegalStateException("Current account unavailable")

    fun set(id: String, login: String) {
        account = CurrentAccount(id = id, login = login)
    }

    fun clear() {
        account = null
    }
}
