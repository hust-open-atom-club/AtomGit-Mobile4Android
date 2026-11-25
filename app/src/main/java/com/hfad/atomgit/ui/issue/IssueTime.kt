package com.hfad.atomgit.ui.issue

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.hfad.atomgit.data.repository.mapping.parseNotificationTime
import com.hfad.atomgit.ui.notification.formatNotificationAge
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun formatIssueAbsoluteTime(raw: String?): String? {
    val millis = parseNotificationTime(raw) ?: return null
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return formatter.format(Date(millis))
}

/** 绝对时间优先；能解析时再附上相对时间。 */
internal fun formatIssueDisplayTime(raw: String?, nowMillis: Long): String? {
    val absolute = formatIssueAbsoluteTime(raw)
    val relative = formatNotificationAge(raw, nowMillis)
    return when {
        absolute != null && relative != null -> "$absolute（$relative）"
        absolute != null -> absolute
        relative != null -> relative
        else -> raw?.trim()?.takeIf { it.isNotEmpty() }?.take(19)
    }
}

@Composable
internal fun IssueTimeText(
    raw: String?,
    nowMillis: Long,
    modifier: Modifier = Modifier,
) {
    val label = formatIssueDisplayTime(raw, nowMillis) ?: return
    Text(
        text = label,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}