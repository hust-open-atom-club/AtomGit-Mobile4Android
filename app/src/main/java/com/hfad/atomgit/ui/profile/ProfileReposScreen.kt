package com.hfad.atomgit.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hfad.atomgit.data.model.RepoVisibility
import com.hfad.atomgit.data.remote.error.ApiError
import com.hfad.atomgit.ui.components.AtomPullRefreshIndicator
import com.hfad.atomgit.ui.components.CenteredMessage
import com.hfad.atomgit.ui.components.LoadMoreFooter

@Composable
fun ProfileReposScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileReposViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var showSortSheet by remember { mutableStateOf(false) }

    if (showSortSheet) {
        SortSheet(
            selected = state.sort,
            onSelect = {
                viewModel.selectSort(it)
                showSortSheet = false
            },
            onDismiss = { showSortSheet = false },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("screen-profile-repos"),
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
                text = "仓库",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(vertical = 12.dp),
            )
        }
        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("repos-search"),
            placeholder = { Text("搜索仓库") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box {
                var visibilityExpanded by remember { mutableStateOf(false) }
                FilterPill(
                    label = state.visibility.label(),
                    onClick = { visibilityExpanded = true },
                    modifier = Modifier.testTag("repos-filter"),
                )
                DropdownMenu(
                    expanded = visibilityExpanded,
                    onDismissRequest = { visibilityExpanded = false },
                    shape = RoundedCornerShape(12.dp),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                    shadowElevation = 6.dp,
                ) {
                    RepoVisibility.entries.forEach { visibility ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = visibility.label(),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (visibility == state.visibility) {
                                        FontWeight.SemiBold
                                    } else {
                                        FontWeight.Normal
                                    },
                                )
                            },
                            onClick = {
                                viewModel.selectVisibility(visibility)
                                visibilityExpanded = false
                            },
                        )
                    }
                }
            }
            FilterPill(
                label = "排序方式：${state.sort.label()}",
                onClick = { showSortSheet = true },
                modifier = Modifier.testTag("repos-sort"),
            )
        }

        val pullRefreshState = rememberPullToRefreshState()
        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize(),
            state = pullRefreshState,
            indicator = {
                AtomPullRefreshIndicator(
                    state = pullRefreshState,
                    isRefreshing = state.isLoading,
                )
            },
        ) {
            val error = state.error
            when {
                state.isLoading && state.items.isEmpty() -> Box(modifier = Modifier.fillMaxSize())
                error != null && state.items.isEmpty() -> CenteredMessage(
                    message = error.label(),
                    onRetry = viewModel::retry,
                )
                state.items.isEmpty() -> CenteredMessage(message = "没有仓库")
                else -> RepoList(
                    state = state,
                    listState = listState,
                    onLoadMore = viewModel::loadMore,
                    onRetry = viewModel::retry,
                )
            }
        }
    }
}

@Composable
private fun RepoList(
    state: ProfileReposUiState,
    listState: LazyListState,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
) {
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .testTag("repos-list"),
        contentPadding = PaddingValues(bottom = 8.dp),
    ) {
        itemsIndexed(
            items = state.items,
            key = { _, repo -> "${repo.ownerPath}/${repo.path}" },
        ) { index, repo ->
            if (index > 0) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            RepoRow(repo = repo)
        }
        if (!state.endReached && !state.isLoading) {
            item(key = "repos-footer") {
                LoadMoreFooter(
                    itemCount = state.items.size,
                    listState = listState,
                    isLoadingMore = state.isLoadingMore,
                    onLoadMore = onLoadMore,
                )
            }
        }
        if (state.error != null) {
            item(key = "repos-error") {
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

private fun ApiError.label(): String = when (this) {
    ApiError.Unauthorized -> "登录状态已失效"
    ApiError.Forbidden -> "没有查看权限"
    ApiError.Network -> "网络连接失败"
    else -> "加载失败"
}
