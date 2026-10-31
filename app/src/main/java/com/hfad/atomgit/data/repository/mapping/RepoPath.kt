package com.hfad.atomgit.data.repository.mapping

import com.hfad.atomgit.data.remote.dto.RepoDto

/**
 * 接口路径用 namespace.path + path。
 * 带空格的 full_name（如 `111 / mobile-org-probe`）不能用来拼路径。
 */
fun RepoDto?.toRepoPath(): String? {
    val namespace = this?.namespace?.path?.takeIf { it.isNotBlank() } ?: return null
    val path = this?.path?.takeIf { it.isNotBlank() } ?: return null
    return "$namespace/$path"
}
