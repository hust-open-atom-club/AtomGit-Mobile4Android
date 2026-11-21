package com.hfad.atomgit.ui.repo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hfad.atomgit.R
import com.hfad.atomgit.data.model.RepoDetail
import com.hfad.atomgit.ui.components.Avatar

@Composable
internal fun RepoDetailHeader(detail: RepoDetail, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val ownerName = detail.ownerName ?: detail.owner
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .testTag("repo-detail-header"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Avatar(name = ownerName, avatarUrl = detail.ownerAvatarUrl, size = 22.dp)
            Text(
                text = ownerName,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("repo-owner-name"),
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = detail.name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (detail.isPrivate) {
                Icon(Icons.Outlined.Lock, "私有项目", Modifier.size(18.dp), tint = colors.onSurfaceVariant)
            }
        }
        detail.description?.let { description ->
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            RepoStatistic(painterResource(R.drawable.ic_repo_star), detail.stars, "个星标")
            RepoStatistic(painterResource(R.drawable.ic_repo_fork), detail.forks, "个复刻")
        }
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RepoHeaderAction(rememberVectorPainter(Icons.Filled.Star), "星标", Modifier.weight(1f), highlighted = true)
            RepoHeaderAction(painterResource(R.drawable.ic_repo_fork), "复刻", Modifier.weight(1f))
            RepoHeaderAction(rememberVectorPainter(Icons.Outlined.Notifications), "通知", Modifier.weight(1f))
        }
    }
}

@Composable
private fun RepoStatistic(icon: Painter, count: Int, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(count.toString(), style = MaterialTheme.typography.bodyLarge)
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RepoHeaderAction(
    icon: Painter,
    description: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    highlighted: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val dark = colors.background.luminance() < 0.5f
    val background = when {
        dark && highlighted -> Color(0xFF222226)
        dark -> Color(0xFF363A43)
        else -> colors.surfaceVariant
    }
    Surface(
        onClick = {},
        modifier = modifier.fillMaxHeight().testTag("repo-action-$description"),
        shape = RoundedCornerShape(6.dp),
        color = background,
        border = BorderStroke(1.dp, colors.onSurface.copy(alpha = 0.08f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            Icon(
                painter = icon,
                contentDescription = if (label == null) description else null,
                modifier = Modifier.size(22.dp),
                tint = if (highlighted) Color(0xFFF5C518) else colors.onSurfaceVariant,
            )
            if (label != null) {
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}


