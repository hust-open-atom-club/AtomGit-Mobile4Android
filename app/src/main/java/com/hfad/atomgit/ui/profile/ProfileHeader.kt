package com.hfad.atomgit.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

@Composable
fun ProfileHeader(
    displayName: String,
    login: String,
    avatarUrl: String?,
    bio: String?,
    followers: Int,
    following: Int,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
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
            ProfileAvatar(name = displayName, avatarUrl = avatarUrl)
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
            Pressable(onClick = onSettingsClick, showRipple = false) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "设置",
                    modifier = Modifier
                        .size(40.dp)
                        .padding(8.dp),
                )
            }
        }
        val intro = bio?.takeIf { it.isNotBlank() }
        if (intro != null) {
            Text(
                text = intro,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        FollowerLine(followers = followers, following = following)
    }
}

@Composable
private fun FollowerLine(
    followers: Int,
    following: Int,
) {
    val label = MaterialTheme.colorScheme.onSurfaceVariant
    val number = SpanStyle(
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.SemiBold,
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Person,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = label,
        )
        Text(
            text = buildAnnotatedString {
                withStyle(number) { append(followers.toString()) }
                withStyle(SpanStyle(color = label)) { append(" 个关注者 · ") }
                withStyle(number) { append(following.toString()) }
                withStyle(SpanStyle(color = label)) { append(" 关注") }
            },
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun ProfileAvatar(
    name: String,
    avatarUrl: String?,
) {
    val fallback = MaterialTheme.colorScheme.surfaceVariant
    if (avatarUrl.isNullOrBlank()) {
        LetterAvatar(name)
        return
    }
    AsyncImage(
        model = avatarUrl,
        contentDescription = name,
        modifier = Modifier
            .size(80.dp)
            .clip(CircleShape),
        contentScale = ContentScale.Crop,
        placeholder = ColorPainter(fallback),
        error = ColorPainter(fallback),
    )
}

@Composable
private fun LetterAvatar(name: String) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurface),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.take(1).ifBlank { "?" },
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.surface,
        )
    }
}
