package com.hfad.atomgit.ui.notification

import com.hfad.atomgit.data.model.NotificationItem
import com.hfad.atomgit.data.model.NotificationTarget

/** 收件箱一行。同一 Issue / PR 的多条通知合成一条，角标是条数。 */
data class NotificationThread(
    val key: String,
    val owner: String,
    val repo: String,
    val title: String,
    val actorLogin: String?,
    val actorName: String?,
    val actorAvatarUrl: String?,
    val updatedAt: String?,
    val updatedAtMillis: Long?,
    val htmlUrl: String?,
    val count: Int,
    val unread: Boolean,
    val ids: List<String>,
    val unreadIds: List<String>,
    val target: NotificationTarget,
) {
    val repoKey: String get() = "$owner/$repo"

    val repoLine: String
        get() {
            val number = when (val target = target) {
                is NotificationTarget.Issue -> target.number
                is NotificationTarget.PullRequest -> target.number
                else -> null
            }
            return if (number != null) "$owner / $repo #$number" else "$owner / $repo"
        }

    val actorLine: String?
        get() = actorLogin?.let { "@$it" } ?: actorName?.takeIf { it.isNotBlank() }
}

enum class NotificationKindFilter {
    All,
    Event,
    Mention,
    /** 只看已读；类型上仍是收件箱选项，不是独立芯片。 */
    Read,
}

internal fun NotificationItem.matches(filter: NotificationKindFilter): Boolean = when (filter) {
    /** 收件箱默认只看未读，标已读后从列表消失。 */
    NotificationKindFilter.All -> unread
    NotificationKindFilter.Event -> type.equals("event", ignoreCase = true)
    NotificationKindFilter.Mention -> type.equals("referer", ignoreCase = true)
    NotificationKindFilter.Read -> !unread
}

internal fun NotificationItem.isImportant(): Boolean =
    type.equals("referer", ignoreCase = true)

internal fun List<NotificationItem>.toThreads(): List<NotificationThread> {
    if (isEmpty()) return emptyList()
    return groupBy { it.threadKey }
        .map { (key, group) ->
            val newest = group.maxWith(
                compareBy<NotificationItem> { it.updatedAtMillis ?: Long.MIN_VALUE }
                    .thenBy { it.id },
            )
            NotificationThread(
                key = key,
                owner = newest.owner,
                repo = newest.repo,
                title = newest.content?.takeIf { it.isNotBlank() } ?: "通知",
                actorLogin = newest.actorLogin,
                actorName = newest.actorName,
                actorAvatarUrl = newest.actorAvatarUrl,
                updatedAt = newest.updatedAt,
                updatedAtMillis = newest.updatedAtMillis,
                htmlUrl = newest.htmlUrl,
                count = group.size,
                unread = group.any { it.unread },
                ids = group.map { it.id },
                unreadIds = group.filter { it.unread }.map { it.id },
                target = newest.target,
            )
        }
        .sortedWith(
            compareByDescending<NotificationThread> { it.updatedAtMillis ?: Long.MIN_VALUE }
                .thenByDescending { it.key },
        )
}
