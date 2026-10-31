package com.hfad.atomgit.data.local.session

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 当前会话文件的格式版本。 */
const val SESSION_STORAGE_VERSION = 1

/** 会话落盘形态；令牌只以 [ciphertext] 出现。 */
@Serializable
data class StoredSession(
    val version: Int,
    @SerialName("account_id") val accountId: String,
    val login: String,
    val name: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val ciphertext: String,
    @SerialName("saved_at") val savedAt: Long,
)
