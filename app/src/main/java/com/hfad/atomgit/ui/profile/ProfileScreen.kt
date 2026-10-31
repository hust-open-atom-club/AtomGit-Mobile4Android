package com.hfad.atomgit.ui.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * 我的入口：当前账号、项目与组织入口，以及退出登录与撤销指引。
 */
@Composable
fun ProfileScreen(
    listState: LazyListState,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    val title = session?.let { it.name?.takeIf(String::isNotBlank) ?: it.login } ?: "未登录"

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("screen-profile"),
    ) {
        Text(
            text = "我的",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            style = MaterialTheme.typography.titleLarge,
        )
        HorizontalDivider()
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            item {
                ListItem(
                    headlineContent = { Text(title) },
                    supportingContent = { Text(session?.login?.let { "@$it" } ?: "本地没有可用会话") },
                )
                HorizontalDivider()
            }
            item {
                ListItem(
                    headlineContent = { Text("我的项目") },
                    supportingContent = { Text("待接入：仓库列表与范围内检索") },
                )
                HorizontalDivider()
            }
            item {
                ListItem(
                    headlineContent = { Text("我的组织") },
                    supportingContent = { Text("待接入：组织与组织下可访问项目") },
                )
                HorizontalDivider()
            }
            item {
                TextButton(
                    onClick = viewModel::logout,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("profile-logout"),
                ) {
                    Text("退出登录", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
