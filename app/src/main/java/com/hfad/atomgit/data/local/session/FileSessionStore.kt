package com.hfad.atomgit.data.local.session

import com.hfad.atomgit.di.SessionDirectory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import javax.inject.Inject

/**
 * 单文件会话存储
 */
class FileSessionStore @Inject constructor(
    @param:SessionDirectory private val directory: File,
    private val json: Json,
) : SessionStore {

    override suspend fun read(): StoredSession? = withContext(Dispatchers.IO) {
        val target = sessionFile()
        if (!target.exists()) return@withContext null
        runCatching { json.decodeFromString(StoredSession.serializer(), target.readText()) }
            .getOrElse {
                target.delete()
                null
            }
    }

    override suspend fun write(session: StoredSession) = withContext(Dispatchers.IO) {
        if (!directory.exists() && !directory.mkdirs()) {
            throw IOException("session directory unavailable: ${directory.path}")
        }
        val payload = json.encodeToString(StoredSession.serializer(), session)
        val target = sessionFile()
        val temp = File(directory, TEMP_FILE_NAME)
        temp.writeText(payload)
        if (!temp.renameTo(target)) {
            target.writeText(payload)
            temp.delete()
        }
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        sessionFile().delete()
        File(directory, TEMP_FILE_NAME).delete()
        Unit
    }

    private fun sessionFile(): File = File(directory, FILE_NAME)

    private companion object {
        const val FILE_NAME = "session.json"
        const val TEMP_FILE_NAME = "session.json.tmp"
    }
}
