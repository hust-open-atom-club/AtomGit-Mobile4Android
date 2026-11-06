package com.hfad.atomgit.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Star
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

/**
 * 我的入口。资料、项目和组织用本页示例数据；除退出登录外，点击只做按压或展开。
 */
@Composable
fun ProfileScreen(
    listState: LazyListState,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    var query by rememberSaveable { mutableStateOf("") }
    val visibleRepos = ProfileSample.repos.filter { repo ->
        query.isBlank() ||
            repo.name.contains(query, ignoreCase = true) ||
            repo.owner.contains(query, ignoreCase = true)
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .testTag("screen-profile"),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            ProfileHeader(
                onSettingsClick = {},
                onStatusClick = {},
            )
        }
        item { SectionLabel("热门") }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(ProfileSample.repos, key = { "${it.owner}/${it.name}" }) { repo ->
                    RepoCard(
                        repo = repo,
                        onClick = {},
                        modifier = Modifier.fillParentMaxWidth(0.72f),
                    )
                }
            }
        }
        item { SectionLabel("我的项目") }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                label = { Text("当前示例") },
                placeholder = { Text("检索已展示的仓库") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            )
        }
        if (visibleRepos.isEmpty()) {
            item {
                Text(
                    text = "没有匹配的示例仓库",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(visibleRepos, key = { "mine/${it.owner}/${it.name}" }) { repo ->
                RepoCard(
                    repo = repo,
                    onClick = {},
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
        item {
            TextButton(
                onClick = viewModel::logout,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("profile-logout"),
            ) {
                Text("退出登录", color = MaterialTheme.colorScheme.error)
            }
        }
        item {
            ProfileMenuRow(
                iconBackground = Color(0xFF2F363D),
                title = "仓库",
                count = "15",
                icon = { MenuGlyph(Color.White) },
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
                        modifier = Modifier.size(18.dp),
                    )
                },
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
        style = MaterialTheme.typography.titleMedium,
    )
}
