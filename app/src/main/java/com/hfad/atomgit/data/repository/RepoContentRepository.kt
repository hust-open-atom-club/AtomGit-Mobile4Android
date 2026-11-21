package com.hfad.atomgit.data.repository

import com.hfad.atomgit.data.model.RepoFileContent
import com.hfad.atomgit.data.remote.api.AtomGitV5Service
import com.hfad.atomgit.data.remote.error.ApiResult
import com.hfad.atomgit.data.remote.error.apiCall
import okio.ByteString.Companion.decodeBase64
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

/** 仓库文件内容。 */
interface RepoContentRepository {
    suspend fun getFile(
        owner: String,
        repo: String,
        path: String,
        ref: String,
    ): ApiResult<RepoFileContent>
}

@Singleton
class DefaultRepoContentRepository @Inject constructor(
    private val api: AtomGitV5Service,
) : RepoContentRepository {

    override suspend fun getFile(
        owner: String,
        repo: String,
        path: String,
        ref: String,
    ): ApiResult<RepoFileContent> = apiCall {
        val dto = api.getContentFile(
            owner = owner,
            repo = repo,
            path = encodeContentPath(path),
            ref = ref,
        )
        val raw = dto.content.orEmpty()
        val text = decodeFileContent(raw, dto.encoding)
        RepoFileContent(
            path = dto.path ?: path,
            name = dto.name ?: path.substringAfterLast('/'),
            content = text,
            encoding = dto.encoding,
            size = dto.size,
            htmlUrl = dto.htmlUrl,
            downloadUrl = dto.downloadUrl,
        )
    }
}

/** 路径段分别 URL 编码，保留 `/` 分隔。 */
internal fun encodeContentPath(path: String): String =
    path.trim('/')
        .split('/')
        .filter { it.isNotEmpty() }
        .joinToString("/") { segment ->
            java.net.URLEncoder.encode(segment, StandardCharsets.UTF_8.name())
                .replace("+", "%20")
        }

/** 按 encoding 解码；未知或空 encoding 时原样返回。 */
internal fun decodeFileContent(raw: String, encoding: String?): String {
    if (raw.isEmpty()) return ""
    val normalized = encoding?.lowercase()?.trim()
    if (normalized != null && normalized != "base64") return raw
    val compact = raw.replace("\\s".toRegex(), "")
    // 用 Okio，避免 java.util.Base64（API 26+）在 minSdk 24 上触发 Lint NewApi
    return compact.decodeBase64()?.utf8() ?: raw
}
