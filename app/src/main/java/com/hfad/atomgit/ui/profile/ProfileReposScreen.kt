package com.hfad.atomgit.ui.profile

import android.graphics.Color as AndroidColor
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hfad.atomgit.data.model.RepoSort
import com.hfad.atomgit.data.model.RepoSummary
import com.hfad.atomgit.data.model.RepoVisibility
import com.hfad.atomgit.data.remote.error.ApiError
import com.hfad.atomgit.ui.components.AtomPullRefreshIndicator

private val StarYellow = Color(0xFFF5C518)

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
                error != null && state.items.isEmpty() -> EmptyOrError(
                    message = error.label(),
                    onRetry = viewModel::retry,
                )
                state.items.isEmpty() -> EmptyOrError(message = "没有仓库")
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
private fun FilterPill(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 8.dp, top = 7.dp, bottom = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortSheet(
    selected: RepoSort,
    onSelect: (RepoSort) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        Text(
            text = "排序方式",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        )
        SortSection(
            title = "推送时间",
            options = listOf(RepoSort.PushedDesc, RepoSort.PushedAsc),
            selected = selected,
            onSelect = onSelect,
        )
        SortSection(
            title = "更新时间",
            options = listOf(RepoSort.UpdatedDesc, RepoSort.UpdatedAsc),
            selected = selected,
            onSelect = onSelect,
        )
        SortSection(
            title = "创建时间",
            options = listOf(RepoSort.CreatedDesc, RepoSort.CreatedAsc),
            selected = selected,
            onSelect = onSelect,
        )
        SortSection(
            title = "名称",
            options = listOf(RepoSort.NameAsc, RepoSort.NameDesc),
            selected = selected,
            onSelect = onSelect,
        )
        Box(modifier = Modifier.padding(bottom = 24.dp))
    }
}

@Composable
private fun SortSection(
    title: String,
    options: List<RepoSort>,
    selected: RepoSort,
    onSelect: (RepoSort) -> Unit,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
    )
    options.forEach { option ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelect(option) }
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = option.label(),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            RadioButton(selected = option == selected, onClick = null)
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

@Composable
private fun RepoRow(repo: RepoSummary) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("repo-row"),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = repo.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        repo.description?.let { description ->
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Star,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = StarYellow,
                )
                Text(
                    text = repo.stars.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            repo.language?.let { language ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(languageColor(repo.languageColor), CircleShape),
                    )
                    Text(
                        text = language,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (repo.isPrivate) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = "私有",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** 接口给的是 `#RRGGBB`，解析失败就退回中性色。 */
private fun languageColor(hex: String?): Color {
    if (hex.isNullOrBlank()) return Color.Transparent
    val normalized = if (hex.startsWith("#")) hex else "#$hex"
    return runCatching { Color(AndroidColor.parseColor(normalized)) }
        .getOrDefault(Color.Transparent)
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

private fun RepoVisibility.label(): String = when (this) {
    RepoVisibility.All -> "全部"
    RepoVisibility.Public -> "公开"
    RepoVisibility.Private -> "私有"
}

private fun RepoSort.label(): String = when (this) {
    RepoSort.PushedDesc -> "最近推送"
    RepoSort.PushedAsc -> "最久未推送"
    RepoSort.UpdatedDesc -> "最近更新"
    RepoSort.UpdatedAsc -> "最早更新"
    RepoSort.CreatedDesc -> "最新创建"
    RepoSort.CreatedAsc -> "最早创建"
    RepoSort.NameAsc -> "名称升序"
    RepoSort.NameDesc -> "名称降序"
}

private fun ApiError.label(): String = when (this) {
    ApiError.Unauthorized -> "登录状态已失效"
    ApiError.Forbidden -> "没有查看权限"
    ApiError.Network -> "网络连接失败"
    else -> "加载失败"
}
