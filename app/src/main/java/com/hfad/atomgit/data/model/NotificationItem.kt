package com.hfad.atomgit.data.model

/**
 * 一条仓库通知。
 * [owner] 和 [repo] 用请求路径，不用可能带空格的 `full_name`。
 */
data class NotificationItem(
    val id: String,
    val owner: String,
    val repo: String,
    val content: String?,
    val type: String?,
    val unread: Boolean,
    val updatedAt: String?,
    val updatedAtMillis: Long?,
    val htmlUrl: String?,
    val actorLogin: String?,
    val actorName: String?,
    val actorAvatarUrl: String?,
    val target: NotificationTarget,
) {
    val repoKey: String get() = "$owner/$repo"

    val readKey: String get() = "$owner/$repo/$id"

    /** 同一 Issue / PR 合成一行；对不上编号的不合并。 */
    val threadKey: String
        get() = when (val target = target) {
            is NotificationTarget.Issue -> "$repoKey|issue|${target.number}"
            is NotificationTarget.PullRequest -> "$repoKey|pull|${target.number}"
            is NotificationTarget.Web -> "$repoKey|web|${target.url}"
            is NotificationTarget.Repository -> "$repoKey|id|$id"
        }
}

sealed interface NotificationTarget {
    data class Issue(val owner: String, val repo: String, val number: Int) : NotificationTarget

    data class PullRequest(val owner: String, val repo: String, val number: Int) : NotificationTarget

    data class Web(val url: String) : NotificationTarget

    data class Repository(val owner: String, val repo: String) : NotificationTarget
}
