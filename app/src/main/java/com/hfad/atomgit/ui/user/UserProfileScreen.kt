package com.hfad.atomgit.ui.user

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hfad.atomgit.data.model.FollowKind
import com.hfad.atomgit.data.model.ProfileCounts
import com.hfad.atomgit.data.model.UserProfile
import com.hfad.atomgit.data.remote.error.ApiError
import com.hfad.atomgit.ui.profile.MenuGlyph
import com.hfad.atomgit.ui.profile.OrgGlyph
import com.hfad.atomgit.ui.profile.ProfileHeader
import com.hfad.atomgit.ui.profile.ProfileMenuRow

@Composable
fun UserProfileScreen(
    login: String,
    onBack: () -> Unit,
    onOpenFollow: (login: String, kind: FollowKind) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: UserProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(login) { viewModel.start(login) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .testTag("screen-user-profile"),
    ) {
        // 顶栏和下面的头部面板同色、无阴影，连成一整块白色
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 0.dp,
            tonalElevation = 0.dp,
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.CenterStart),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "返回",
                    )
                }
            }
        }
        val profile = state.profile
        val error = state.error
        when {
            profile != null -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Panel(elevated = false) {
                        ProfileHeader(
                            displayName = profile.name ?: profile.login,
                            login = profile.login,
                            avatarUrl = profile.avatarUrl,
                            bio = profile.bio,
                            followers = profile.followers,
                            following = profile.following,
                            onOpenFollowers = {
                                onOpenFollow(profile.login, FollowKind.Followers)
                            },
                            onOpenFollowing = {
                                onOpenFollow(profile.login, FollowKind.Following)
                            },
                        )
                    }
                }
                item {
                    Panel {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            ProfileFacts(profile)
                            ProfileMenu(counts = state.counts)
                        }
                    }
                }
            }
            error != null -> CenteredHint(
                text = error.orEmptyMessage(),
                onRetry = viewModel::retry,
            )
            else -> CenteredProgress()
        }
    }
}

/** 仓库 / 组织 / 已加星标，和「我的」页同一套行样式。 */
@Composable
private fun ProfileMenu(counts: ProfileCounts) {
    Column(modifier = Modifier.fillMaxWidth()) {
        ProfileMenuRow(
            iconBackground = Color(0xFF2F363D),
            title = "仓库",
            count = counts.repos?.toString().orEmpty(),
            icon = { MenuGlyph(Color.White) },
        )
        ProfileMenuRow(
            iconBackground = Color(0xFFFF9F1A),
            title = "组织",
            count = counts.orgs?.toString().orEmpty(),
            icon = { OrgGlyph(Color.White) },
        )
        ProfileMenuRow(
            iconBackground = Color(0xFFF5C518),
            title = "已加星标",
            count = counts.starred?.toString().orEmpty(),
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Star,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
            },
        )
    }
}

@Composable
private fun ProfileFacts(profile: UserProfile) {
    if (profile.company == null && profile.location == null && profile.website == null) return

    val uriHandler = LocalUriHandler.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        profile.company?.let { FactRow(label = "公司", value = it) }
        profile.location?.let { FactRow(label = "地区", value = it) }
        profile.website?.let { site ->
            FactRow(
                label = "主页",
                value = site,
                onClick = { runCatching { uriHandler.openUri(site.asBrowserUrl()) } },
            )
        }
    }
}

@Composable
private fun FactRow(
    label: String,
    value: String,
    onClick: (() -> Unit)? = null,
) {
    Row {
        Text(
            text = label,
            modifier = Modifier.width(72.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textDecoration = if (onClick != null) TextDecoration.Underline else null,
            modifier = Modifier
                .weight(1f)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        )
    }
}

/** 用户填的可能是 `example.com`，补成 https 才能交给浏览器。 */
private fun String.asBrowserUrl(): String =
    if (startsWith("http://", ignoreCase = true) || startsWith("https://", ignoreCase = true)) {
        this
    } else {
        "https://$this"
    }

@Composable
private fun Panel(
    elevated: Boolean = true,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = if (elevated) 2.dp else 0.dp,
        tonalElevation = 0.dp,
        content = content,
    )
}

@Composable
private fun CenteredProgress() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .size(40.dp)
                .testTag("user-profile-loading"),
            strokeWidth = 4.dp,
        )
    }
}

@Composable
private fun CenteredHint(
    text: String,
    onRetry: (() -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (onRetry != null) {
                TextButton(onClick = onRetry) {
                    Text(text = "重试")
                }
            }
        }
    }
}

private fun ApiError.orEmptyMessage(): String = when (this) {
    ApiError.NotFound -> "用户不存在"
    ApiError.Unauthorized -> "登录状态已失效"
    ApiError.Forbidden -> "没有查看权限"
    ApiError.Network -> "网络连接失败"
    else -> "加载失败"
}
