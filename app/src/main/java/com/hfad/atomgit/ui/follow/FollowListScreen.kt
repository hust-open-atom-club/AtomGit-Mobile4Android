package com.hfad.atomgit.ui.follow

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hfad.atomgit.data.model.FollowKind
import com.hfad.atomgit.data.remote.error.ApiError

@Composable
fun FollowListScreen(
    login: String,
    kind: FollowKind,
    onBack: () -> Unit,
    onOpenUser: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FollowListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(login, kind) { viewModel.start(login, kind) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("screen-follow"),
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
            Text(
                text = "粉丝与关注",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(vertical = 12.dp),
            )
        }
        PrimaryTabRow(selectedTabIndex = state.kind.ordinal) {
            FollowKind.entries.forEach { tab ->
                Tab(
                    selected = tab == state.kind,
                    onClick = { viewModel.selectKind(tab) },
                    text = { Text(text = tab.label()) },
                    modifier = Modifier.testTag("follow-tab-${tab.name}"),
                )
            }
        }

        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            val error = state.error
            when {
                state.isLoading && state.items.isEmpty() -> Box(modifier = Modifier.fillMaxSize())
                error != null && state.items.isEmpty() -> EmptyOrError(
                    message = error.orEmptyMessage(),
                    onRetry = viewModel::retry,
                )
                state.items.isEmpty() -> EmptyOrError(message = state.kind.emptyMessage())
                else -> FollowList(
                    state = state,
                    listState = listState,
                    onOpenUser = onOpenUser,
                    onLoadMore = viewModel::loadMore,
                    onRetry = viewModel::retry,
                )
            }
        }
    }
}

@Composable
private fun FollowList(
    state: FollowListUiState,
    listState: LazyListState,
    onOpenUser: (String) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
) {
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .testTag("follow-list"),
    ) {
        itemsIndexed(
            items = state.items,
            key = { index, item -> if (item.login.isBlank()) "row-$index" else item.login },
        ) { _, user ->
            UserRow(user = user, onClick = { onOpenUser(user.login) })
        }
        if (!state.endReached && !state.isLoading) {
            item(key = "follow-footer") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    LaunchedEffect(state.items.size) {
                        if (listState.firstVisibleItemIndex > 0 || listState.canScrollForward) {
                            onLoadMore()
                        }
                    }
                    if (state.isLoadingMore) {
                        Text(
                            text = "加载中…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        if (state.error != null) {
            item(key = "follow-error") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    TextButton(onClick = onRetry) {
                        Text(text = "加载失败，点击重试")
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyOrError(
    message: String,
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
                text = message,
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

private fun FollowKind.label(): String = when (this) {
    FollowKind.Followers -> "粉丝"
    FollowKind.Following -> "关注"
}

private fun FollowKind.emptyMessage(): String = when (this) {
    FollowKind.Followers -> "还没有粉丝"
    FollowKind.Following -> "还没有关注任何人"
}

private fun ApiError.orEmptyMessage(): String = when (this) {
    ApiError.NotFound -> "用户不存在"
    ApiError.Unauthorized -> "登录状态已失效"
    ApiError.Forbidden -> "没有查看权限"
    ApiError.Network -> "网络连接失败"
    else -> "加载失败"
}
