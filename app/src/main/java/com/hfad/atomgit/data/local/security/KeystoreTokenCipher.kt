package com.hfad.atomgit.data.local.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject

/** Keystore 中的 AES-256-GCM；载荷 = `[版本 1][IV 12][密文]`。 */
class KeystoreTokenCipher @Inject constructor() : TokenCipher {

    override suspend fun encrypt(plaintext: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, loadOrCreateKey())
            val iv = cipher.iv
            check(iv.size == IV_SIZE) { "unexpected GCM iv size: ${iv.size}" }
            val body = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            ByteArray(HEADER_SIZE + IV_SIZE + body.size).also { payload ->
                payload[0] = VERSION
                iv.copyInto(payload, destinationOffset = HEADER_SIZE)
                body.copyInto(payload, destinationOffset = HEADER_SIZE + IV_SIZE)
            }
        } catch (e: GeneralSecurityException) {
            null
        } catch (e: IllegalStateException) {
            null
        }
    }

    override suspend fun decrypt(payload: ByteArray): String? = withContext(Dispatchers.IO) {
        try {
            if (payload.size <= HEADER_SIZE + IV_SIZE) return@withContext null
            if (payload[0] != VERSION) return@withContext null
            val key = loadKey() ?: return@withContext null
            val iv = payload.copyOfRange(HEADER_SIZE, HEADER_SIZE + IV_SIZE)
            val body = payload.copyOfRange(HEADER_SIZE + IV_SIZE, payload.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
            String(cipher.doFinal(body), Charsets.UTF_8)
        } catch (e: GeneralSecurityException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    private fun loadOrCreateKey(): SecretKey {
        loadKey()?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE_BITS)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generator.generateKey()
    }

    private fun loadKey(): SecretKey? {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        return keyStore.getKey(KEY_ALIAS, null) as? SecretKey
    }

    private companion object {
        const val KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "atomgit.session.v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_SIZE_BITS = 256
        const val TAG_BITS = 128
        const val VERSION: Byte = 1
        const val HEADER_SIZE = 1
        const val IV_SIZE = 12
    }
}
