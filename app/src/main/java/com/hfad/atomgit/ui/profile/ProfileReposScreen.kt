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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
import com.hfad.atomgit.ui.theme.RepoFilterMenuDark

@Composable
fun ProfileReposScreen(
    onBack: () -> Unit,
    onOpenRepo: (owner: String, repo: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileReposViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var showSortSheet by remember { mutableStateOf(false) }
    var searching by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val filterMenuColor = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) {
        RepoFilterMenuDark
    } else {
        MaterialTheme.colorScheme.surface
    }

    LaunchedEffect(searching) {
        if (searching) focusRequester.requestFocus()
    }

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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = {
                    if (searching) {
                        viewModel.onQueryChange("")
                        searching = false
                        keyboard?.hide()
                    } else {
                        onBack()
                    }
                },
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = if (searching) "退出搜索" else "返回",
                )
            }
            if (searching) {
                TextField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .testTag("repos-search"),
                    placeholder = { Text("搜索项目") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                )
                IconButton(onClick = { viewModel.onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "清空",
                    )
                }
            } else {
                Text(
                    text = "项目",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp),
                )
                IconButton(
                    onClick = { searching = true },
                    modifier = Modifier.testTag("repos-search-open"),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "搜索",
                    )
                }
            }
        }
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
                    containerColor = filterMenuColor,
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
                state.items.isEmpty() -> CenteredMessage(
                    message = if (state.query.isBlank()) "没有项目" else "没有找到匹配的项目",
                )
                else -> RepoList(
                    state = state,
                    listState = listState,
                    onLoadMore = viewModel::loadMore,
                    onRetry = viewModel::retry,
                    onOpenRepo = onOpenRepo,
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
    onOpenRepo: (String, String) -> Unit,
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
            RepoRow(repo = repo, onClick = { onOpenRepo(repo.ownerPath, repo.path) })
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
