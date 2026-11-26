package com.hfad.atomgit.data.repository.mapping

import com.hfad.atomgit.data.model.NotificationItem
import com.hfad.atomgit.data.model.NotificationTarget
import com.hfad.atomgit.data.remote.dto.NotificationDto
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

private val IssueNumber = Regex("""/issues/(\d+)(?:[/?#]|$)""", RegexOption.IGNORE_CASE)
private val PullNumber = Regex("""/(?:pulls|pull|merge_requests)/(\d+)(?:[/?#]|$)""", RegexOption.IGNORE_CASE)

internal fun parseNotificationTime(raw: String?): Long? {
    if (raw.isNullOrBlank()) return null
    val normalized = raw.trim()
        .replace(Regex("\\.\\d+"), "")
        .let { if (it.endsWith("Z", ignoreCase = true)) it else "${it}Z" }
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
            isLenient = false
        }
        parser.parse(normalized)?.time
    } catch (_: Exception) {
        null
    }
}

internal fun NotificationDto.toNotificationItem(owner: String, repo: String): NotificationItem? {
    if (id.isBlank() || owner.isBlank() || repo.isBlank()) return null
    val url = htmlUrl?.trim()?.takeIf { it.isNotEmpty() }
    return NotificationItem(
        id = id,
        owner = owner,
        repo = repo,
        content = content?.trim()?.takeIf { it.isNotEmpty() },
        type = type?.trim()?.takeIf { it.isNotEmpty() },
        unread = unread == true,
        updatedAt = updateAt,
        updatedAtMillis = parseNotificationTime(updateAt),
        htmlUrl = url,
        actorLogin = actor?.login?.trim()?.takeIf { it.isNotEmpty() },
        actorName = actor?.name?.trim()?.takeIf { it.isNotEmpty() },
        actorAvatarUrl = actor?.avatarUrl?.trim()?.takeIf { it.isNotEmpty() },
        target = notificationTarget(owner, repo, url),
    )
}

internal fun notificationTarget(owner: String, repo: String, htmlUrl: String?): NotificationTarget {
    val url = htmlUrl?.trim().orEmpty()
    IssueNumber.find(url)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let { number ->
        return NotificationTarget.Issue(owner, repo, number)
    }
    PullNumber.find(url)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let { number ->
        return NotificationTarget.PullRequest(owner, repo, number)
    }
    if (url.startsWith("https://") || url.startsWith("http://")) {
        return NotificationTarget.Web(url)
    }
    return NotificationTarget.Repository(owner, repo)
}
