package com.hfad.atomgit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

/** 圆形头像；没有地址时用首字兜底。 */
@Composable
fun Avatar(
    name: String,
    avatarUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    val fallback = MaterialTheme.colorScheme.surfaceVariant
    if (avatarUrl.isNullOrBlank()) {
        LetterAvatar(name = name, size = size, modifier = modifier)
        return
    }
    AsyncImage(
        model = avatarUrl,
        contentDescription = name,
        modifier = modifier
            .size(size)
            .clip(CircleShape),
        contentScale = ContentScale.Crop,
        placeholder = ColorPainter(fallback),
        error = ColorPainter(fallback),
    )
}

@Composable
private fun LetterAvatar(
    name: String,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurface),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.take(1).ifBlank { "?" },
            style = if (size >= 64.dp) {
                MaterialTheme.typography.headlineSmall
            } else {
                MaterialTheme.typography.titleMedium
            },
            color = MaterialTheme.colorScheme.surface,
        )
    }
}
