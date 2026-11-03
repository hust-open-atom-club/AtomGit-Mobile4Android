package com.hfad.atomgit.data.repository.mapping

import com.hfad.atomgit.data.remote.dto.RepoDto

/**
 * 接口路径用 namespace.path + path。
 * 带空格的 full_name（如 `111 / mobile-org-probe`）不能用来拼路径。
 */
fun RepoDto?.toRepoPath(): String? {
    if (this == null) return null
    val namespace = namespace?.path?.takeIf { it.isNotBlank() }
    val repoPath = path?.takeIf { it.isNotBlank() }
    if (namespace != null && repoPath != null) return "$namespace/$repoPath"
    val fullName = fullName?.takeIf { it.isNotBlank() } ?: return null
    // `111 / mobile-org-probe` 是展示名，不能拼路径。`owner/repo` 可以。
    if (fullName.contains(' ') || !fullName.contains('/')) return null
    return fullName
}
