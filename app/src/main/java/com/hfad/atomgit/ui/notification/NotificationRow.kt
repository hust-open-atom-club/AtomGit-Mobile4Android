package com.hfad.atomgit.ui.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hfad.atomgit.R
import com.hfad.atomgit.data.model.NotificationTarget
import com.hfad.atomgit.ui.components.Avatar
import com.hfad.atomgit.ui.components.InboxLayout
import com.hfad.atomgit.ui.theme.AccentBlue
import com.hfad.atomgit.ui.theme.AccentBlueBright
import com.hfad.atomgit.ui.theme.AccentBlueContainer
import com.hfad.atomgit.ui.theme.AccentBlueContainerDark

private val IconPurple = Color(0xFF8250DF)
private val IconPurpleDark = Color(0xFFD2A8FF)

@Composable
fun NotificationRow(
    thread: NotificationThread,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    nowMillis: Long = System.currentTimeMillis(),
) {
    val colors = MaterialTheme.colorScheme
    val isDark = colors.background.luminance() < 0.5f
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface)
            .clickable(onClick = onClick)
            .testTag("notification-row"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = InboxLayout.rowMinHeight)
                .padding(
                    horizontal = InboxLayout.rowHorizontal,
                    vertical = InboxLayout.rowVertical,
                ),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            NotificationKindIcon(
                target = thread.target,
                isDark = isDark,
                modifier = Modifier.padding(top = 2.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = thread.repoLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    val age = formatNotificationAge(thread.updatedAt, nowMillis)
                    if (age != null) {
                        Text(
                            text = age,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = thread.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (thread.unread) FontWeight.SemiBold else FontWeight.Normal,
                        color = colors.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    CountBadge(count = thread.count, isDark = isDark)
                }
                val actor = thread.actorLine
                if (actor != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        ActorMark(
                            name = actor,
                            avatarUrl = thread.actorAvatarUrl,
                        )
                        Text(
                            text = actor,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
        HorizontalDivider(color = colors.outline)
    }
}

@Composable
private fun NotificationKindIcon(
    target: NotificationTarget,
    isDark: Boolean,
    modifier: Modifier = Modifier,
) {
    val tint = if (isDark) IconPurpleDark else IconPurple
    when (target) {
        is NotificationTarget.PullRequest -> Icon(
            painter = painterResource(R.drawable.ic_repo_fork),
            contentDescription = "PR",
            tint = tint,
            modifier = modifier.size(InboxLayout.leadingIcon),
        )
        is NotificationTarget.Issue -> Icon(
            imageVector = Icons.Outlined.CheckCircle,
            contentDescription = "Issue",
            tint = tint,
            modifier = modifier.size(InboxLayout.leadingIcon),
        )
        else -> Icon(
            imageVector = Icons.Outlined.Notifications,
            contentDescription = "通知",
            tint = tint,
            modifier = modifier.size(InboxLayout.leadingIcon),
        )
    }
}

@Composable
private fun CountBadge(count: Int, isDark: Boolean) {
    val background = if (isDark) AccentBlueContainerDark else AccentBlueContainer
    val foreground = if (isDark) AccentBlueBright else AccentBlue
    Box(
        modifier = Modifier
            .widthIn(min = 22.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = count.toString(),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = foreground,
        )
    }
}

@Composable
private fun ActorMark(name: String, avatarUrl: String?) {
    if (!avatarUrl.isNullOrBlank()) {
        Avatar(name = name, avatarUrl = avatarUrl, size = 16.dp)
        return
    }
    Box(
        modifier = Modifier
            .size(16.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurface),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.removePrefix("@").take(1).ifBlank { "?" },
            fontSize = 9.sp,
            lineHeight = 10.sp,
            color = MaterialTheme.colorScheme.surface,
        )
    }
}
