package com.hfad.atomgit.ui.todo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hfad.atomgit.data.model.TodoItem
import com.hfad.atomgit.data.model.TodoKind
import com.hfad.atomgit.ui.components.InboxLayout
import com.hfad.atomgit.ui.components.M3RaisedCard
import com.hfad.atomgit.ui.profile.Pressable
import com.hfad.atomgit.ui.theme.AccentBlue
import com.hfad.atomgit.ui.theme.AccentBlueBright
import com.hfad.atomgit.ui.theme.AccentBlueContainer
import com.hfad.atomgit.ui.theme.AccentBlueContainerDark
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

private val IssueGreen = Color(0xFF1A7F37)
private val IssueGreenContainer = Color(0xFFDCFFE4)
private val IssueGreenDark = Color(0xFF3FB950)
private val IssueGreenContainerDark = Color(0xFF0D2818)
private val PrPurple = Color(0xFF8250DF)
private val PrPurpleContainer = Color(0xFFF0E7FF)
private val PrPurpleDark = Color(0xFFD2A8FF)
private val PrPurpleContainerDark = Color(0xFF2A1A4A)
private val StatusOpenBg = Color(0xFFDCFFE4)
private val StatusOpenFg = Color(0xFF1A7F37)
private val StatusClosedBg = Color(0xFFFFEBE9)
private val StatusClosedFg = Color(0xFFCF222E)
private val StatusMergedBg = Color(0xFFF0E7FF)
private val StatusMergedFg = Color(0xFF8250DF)

@Composable
fun TodoRow(
    item: TodoItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = item.title?.takeIf { it.isNotBlank() } ?: "无标题"
    val status = item.displayStatus()
    val time = formatUpdatedAt(item.updatedAt)
    val colors = MaterialTheme.colorScheme

    Pressable(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("todo-row"),
        showRipple = false,
    ) {
        M3RaisedCard(Modifier.fillMaxWidth()) {
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
                KindBadge(kind = item.kind)

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "#${item.number}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = colors.primary,
                        )
                        if (!item.repoPath.isNullOrBlank()) {
                            Text(
                                text = "·",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant,
                            )
                            Text(
                                text = item.repoPath,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                        }
                    }
                    if (status != null || time != null) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (status != null) {
                                StatusChip(text = status)
                            }
                            if (time != null) {
                                Text(
                                    text = time,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.onSurfaceVariant,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KindBadge(kind: TodoKind) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val (container, content) = when (kind) {
        TodoKind.Issue ->
            if (isDark) IssueGreenContainerDark to IssueGreenDark
            else IssueGreenContainer to IssueGreen
        TodoKind.PullRequest ->
            if (isDark) PrPurpleContainerDark to PrPurpleDark
            else PrPurpleContainer to PrPurple
    }
    Box(
        modifier = Modifier
            .widthIn(min = 40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(container)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = kind.shortLabel(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = content,
        )
    }
}

@Composable
private fun StatusChip(text: String) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val (bg, fg) = statusColors(text, isDark)
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Medium,
        color = fg,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

private fun statusColors(raw: String, isDark: Boolean): Pair<Color, Color> {
    val key = raw.trim().lowercase(Locale.US)
    return when {
        key.contains("merge") || key == "merged" ->
            if (isDark) PrPurpleContainerDark to PrPurpleDark
            else StatusMergedBg to StatusMergedFg
        key.contains("close") || key == "closed" ->
            if (isDark) Color(0xFF3D1215) to Color(0xFFFF7B72)
            else StatusClosedBg to StatusClosedFg
        key.contains("open") || key == "opened" || key == "reopened" ->
            if (isDark) IssueGreenContainerDark to IssueGreenDark
            else StatusOpenBg to StatusOpenFg
        else ->
            if (isDark) AccentBlueContainerDark to AccentBlueBright
            else AccentBlueContainer to AccentBlue
    }
}

private fun TodoKind.shortLabel(): String = when (this) {
    TodoKind.Issue -> "Issue"
    TodoKind.PullRequest -> "PR"
}

private fun TodoItem.displayStatus(): String? =
    issueState?.takeIf { it.isNotBlank() } ?: state?.takeIf { it.isNotBlank() }

/**
 * 将接口 ISO 时间收成本地可读短格式；解析失败则原样截断，避免把整串 Zulu 甩在列表里。
 */
internal fun formatUpdatedAt(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    val normalized = raw.trim()
        .replace(Regex("\\.\\d+"), "")
        .let { if (it.endsWith("Z", ignoreCase = true)) it else "${it}Z" }
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
            isLenient = false
        }
        val date = parser.parse(normalized) ?: return raw.take(16)
        val formatter = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
        formatter.format(date)
    } catch (_: Exception) {
        raw.take(16)
    }
}
