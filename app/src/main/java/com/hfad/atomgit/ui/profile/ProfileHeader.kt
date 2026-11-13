package com.hfad.atomgit.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.hfad.atomgit.ui.components.Avatar

@Composable
fun ProfileHeader(
    displayName: String,
    login: String,
    avatarUrl: String?,
    bio: String?,
    followers: Int,
    following: Int,
    onOpenFollowers: () -> Unit,
    onOpenFollowing: () -> Unit,
    modifier: Modifier = Modifier,
    onSettingsClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Avatar(name = displayName, avatarUrl = avatarUrl, size = 80.dp)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                if (login.isNotBlank()) {
                    Text(
                        text = login,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            onSettingsClick?.let { openSettings ->
                Pressable(onClick = openSettings, showRipple = false) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "设置",
                        modifier = Modifier
                            .size(40.dp)
                            .padding(8.dp),
                    )
                }
            }
        }
        val intro = bio?.takeIf { it.isNotBlank() }
        if (intro != null) {
            Text(
                text = intro,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        FollowerLine(
            followers = followers,
            following = following,
            onOpenFollowers = onOpenFollowers,
            onOpenFollowing = onOpenFollowing,
        )
    }
}

/** 列表和关注行 */
@Composable
private fun FollowerLine(
    followers: Int,
    following: Int,
    onOpenFollowers: () -> Unit,
    onOpenFollowing: () -> Unit,
) {
    val label = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Person,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = label,
        )
        CountLink(count = followers, label = "个关注者", onClick = onOpenFollowers)
        Text(
            text = "·",
            style = MaterialTheme.typography.bodyMedium,
            color = label,
        )
        CountLink(count = following, label = "关注", onClick = onOpenFollowing)
    }
}

@Composable
private fun CountLink(
    count: Int,
    label: String,
    onClick: () -> Unit,
) {
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    Pressable(onClick = onClick, showRipple = false) {
        Text(
            text = buildAnnotatedString {
                withStyle(
                    SpanStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    ),
                ) {
                    append(count.toString())
                }
                withStyle(SpanStyle(color = labelColor)) { append(" $label") }
            },
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 2.dp, vertical = 4.dp),
        )
    }
}
