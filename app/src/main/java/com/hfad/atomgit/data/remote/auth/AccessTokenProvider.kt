package com.hfad.atomgit.data.remote.auth

import javax.inject.Inject
import javax.inject.Singleton

/**
 * 进程内持有当前 PAT，供登录校验在写入 Keystore 之前先放上令牌。
 * 不落盘；会话加密由后续会话层负责。
 */
@Singleton
class AccessTokenProvider @Inject constructor() {
    @Volatile
    private var token: String? = null

    fun getToken(): String? = token

    fun setToken(value: String?) {
        token = value
    }

    fun clear() {
        token = null
    }
}
