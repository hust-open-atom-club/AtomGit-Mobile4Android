package com.hfad.atomgit.ui.notification

import com.hfad.atomgit.data.repository.mapping.parseNotificationTime
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 与收件箱稿一致：`2 小时`，不带「前」。 */
internal fun formatNotificationAge(raw: String?, nowMillis: Long): String? {
    val millis = parseNotificationTime(raw) ?: return null
    val delta = (nowMillis - millis).coerceAtLeast(0L)
    val minute = 60_000L
    val hour = 60 * minute
    val day = 24 * hour
    return when {
        delta < minute -> "刚刚"
        delta < hour -> "${delta / minute} 分钟"
        delta < day -> "${delta / hour} 小时"
        delta < 30 * day -> "${delta / day} 天"
        else -> SimpleDateFormat("M-d", Locale.getDefault()).format(Date(millis))
    }
}
