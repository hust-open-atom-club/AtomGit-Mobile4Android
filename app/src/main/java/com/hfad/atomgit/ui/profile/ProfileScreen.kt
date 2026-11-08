package com.hfad.atomgit.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * 我的入口。资料、项目和组织用本页示例数据；除退出登录外，点击只做按压或展开。
 * 上板块贴顶，以「热门」为界拆成上下两块白底面板。
 */
@Composable
fun ProfileScreen(
    listState: LazyListState,
    onOpenRepos: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val displayName = session?.name?.takeIf { it.isNotBlank() } ?: session?.login ?: "未登录"
    val pageBackground = MaterialTheme.colorScheme.surfaceVariant

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = viewModel::refresh,
        modifier = modifier
            .fillMaxSize()
            .background(pageBackground)
            .testTag("screen-profile"),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                ProfilePanel {
                    ProfileHeader(
                        displayName = displayName,
                        login = session?.login.orEmpty(),
                        avatarUrl = session?.avatarUrl,
                        bio = session?.bio,
                        followers = session?.followers ?: 0,
                        following = session?.following ?: 0,
                        onSettingsClick = {},
                    )
                }
            }
            item {
                ProfilePanel {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SectionLabel("热门")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(ProfileSample.repos, key = { "${it.owner}/${it.name}" }) { repo ->
                                RepoCard(
                                    repo = repo,
                                    onClick = {},
                                    modifier = Modifier.fillParentMaxWidth(0.62f),
                                )
                            }
                        }
                        ProfileMenuRow(
                            iconBackground = Color(0xFF2F363D),
                            title = "仓库",
                            count = "15",
                            onClick = onOpenRepos,
                            icon = { MenuGlyph(Color.White) },
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        ProfileMenuRow(
                            iconBackground = Color(0xFFFF9F1A),
                            title = "组织",
                            count = "0",
                            icon = { OrgGlyph(Color.White) },
                        )
                        ProfileMenuRow(
                            iconBackground = Color(0xFFF5C518),
                            title = "已加星标",
                            count = "94",
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.Star,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                        )
                        TextButton(
                            onClick = viewModel::logout,
                            modifier = Modifier
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("profile-logout"),
                        ) {
                            Text("退出登录", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfilePanel(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        tonalElevation = 0.dp,
    ) {
        content()
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 8.dp),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
}
