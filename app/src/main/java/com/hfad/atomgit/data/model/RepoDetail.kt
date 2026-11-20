package com.hfad.atomgit.data.model

data class RepoDetail(
    val owner: String,
    val path: String,
    val name: String,
    val description: String? = null,
    val stars: Int = 0,
    val isPrivate: Boolean = false,
    val defaultBranch: String? = null,
    val htmlUrl: String? = null,
)

enum class RepoEntryKind { Directory, File, Unsupported }

data class RepoEntry(
    val name: String,
    val path: String,
    val kind: RepoEntryKind,
)

data class CommitSummary(
    val sha: String,
    val message: String,
    val date: String? = null,
)

enum class RepoRefKind { Branch, Tag }

data class RepoRef(
    val name: String,
    val kind: RepoRefKind,
    val sha: String? = null,
    val isDefault: Boolean = false,
) {
    // 标签使用 SHA，避免与同名分支混淆
    val requestRef: String get() = if (kind == RepoRefKind.Tag) sha ?: name else name
}

fun isKnownBinaryPath(path: String): Boolean = path.substringAfterLast('.', "").lowercase() in setOf(
    "png", "jpg", "jpeg", "gif", "webp", "ico", "bmp", "avif", "heic",
    "pdf", "zip", "gz", "tar", "7z", "rar", "jar", "aar", "apk", "aab",
    "so", "dll", "exe", "class", "dex", "ttf", "otf", "woff", "woff2",
    "mp3", "wav", "ogg", "mp4", "mov", "avi", "webm", "db", "sqlite",
)
