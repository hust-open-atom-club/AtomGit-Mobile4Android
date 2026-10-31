package com.hfad.atomgit.data.model

/** 当前登录账号；[accountId] 供后续缓存键隔离。 */
data class AuthSession(
    val accountId: String,
    val login: String,
    val name: String? = null,
    val avatarUrl: String? = null,
)
