package com.hfad.atomgit.data.local.security

/**
 * 令牌的本地加密。
 */
interface TokenCipher {

    suspend fun encrypt(plaintext: String): ByteArray?

    suspend fun decrypt(payload: ByteArray): String?
}
