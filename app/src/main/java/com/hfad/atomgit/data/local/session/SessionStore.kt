package com.hfad.atomgit.data.local.session

/**
 * 会话密文的存取。
 */
interface SessionStore {

    suspend fun read(): StoredSession?

    suspend fun write(session: StoredSession)

    suspend fun clear()
}
