package com.hfad.atomgit.navigation

import java.net.URI
import java.net.URLDecoder

/** AtomGit 网页地址在 App 内对应的目标 */
sealed interface AtomGitLink {
    val owner: String
    val repo: String

    data class Repo(override val owner: String, override val repo: String) : AtomGitLink
    data class File(
        override val owner: String,
        override val repo: String,
        val ref: String,
        val path: String,
    ) : AtomGitLink
    data class Directory(
        override val owner: String,
        override val repo: String,
        val ref: String,
        val path: String,
    ) : AtomGitLink
}

private val AtomGitHosts = setOf("atomgit.com", "www.atomgit.com")

/** 解析 `https://atomgit.com/{owner}/{repo}[/blob|tree/{ref}/{path}]`，其它地址返回 null */
fun parseAtomGitLink(url: String): AtomGitLink? {
    val uri = runCatching { URI(url) }.getOrNull() ?: return null
    val scheme = uri.scheme?.lowercase() ?: return null
    if (scheme != "https" && scheme != "http") return null
    val host = uri.host?.lowercase() ?: return null
    if (host !in AtomGitHosts) return null
    val segments = (uri.rawPath ?: return null)
        .split('/')
        .filter { it.isNotEmpty() }
        .map { decodeSegment(it) }
    if (segments.size < 2) return null
    val owner = segments[0]
    val repo = segments[1].removeSuffix(".git")
    if (segments.size == 2) return AtomGitLink.Repo(owner, repo)
    val kind = segments[2]
    // issues、pulls 等页面还没有对应实现，交回浏览器
    if (kind != "blob" && kind != "tree") return null
    if (segments.size < 4) return AtomGitLink.Repo(owner, repo)
    val ref = segments[3]
    val path = segments.drop(4).joinToString("/")
    return when (kind) {
        "blob" -> if (path.isEmpty()) AtomGitLink.Repo(owner, repo) else AtomGitLink.File(owner, repo, ref, path)
        else -> AtomGitLink.Directory(owner, repo, ref, path)
    }
}

private fun decodeSegment(segment: String): String =
    runCatching { URLDecoder.decode(segment, "UTF-8") }.getOrDefault(segment)
