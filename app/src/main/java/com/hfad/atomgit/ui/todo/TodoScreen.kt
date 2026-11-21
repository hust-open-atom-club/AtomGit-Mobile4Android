package com.hfad.atomgit.ui.todo

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hfad.atomgit.data.model.TodoItem
import com.hfad.atomgit.data.model.TodoKind
import com.hfad.atomgit.data.remote.error.ApiError

@Composable
fun TodoScreen(
    listState: LazyListState,
    onOpenIssue: (owner: String, repo: String, number: Int) -> Unit,
    onOpenPull: (owner: String, repo: String, number: Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TodoViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme
    LaunchedEffect(Unit) { viewModel.start() }
    LaunchedEffect(state.source) {
        if (listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0) {
            listState.scrollToItem(0)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surfaceVariant)
            .testTag("screen-todo"),
    ) {
        Surface(
            color = colors.surface,
            shadowElevation = 0.dp,
            tonalElevation = 0.dp,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "待办",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface,
                    )
                    CountPill(label = state.countLabel())
                }

                PrimaryTabRow(
                    selectedTabIndex = state.group.ordinal,
                    containerColor = colors.surface,
                    contentColor = colors.onSurface,
                ) {
                    TodoGroup.entries.forEach { group ->
                        Tab(
                            selected = group == state.group,
                            onClick = { viewModel.selectGroup(group) },
                            text = {
                                Text(
                                    text = group.label(),
                                    fontWeight = if (group == state.group) {
                                        FontWeight.SemiBold
                                    } else {
                                        FontWeight.Medium
                                    },
                                )
                            },
                            modifier = Modifier.testTag("todo-group-${group.name}"),
                        )
                    }
                }

                SourceChipRow(
                    sources = TodoSource.sourcesFor(state.group),
                    selected = state.source,
                    onSelect = viewModel::selectSource,
                )
            }
        }

        PullToRefreshBox(
            isRefreshing = state.isLoading && state.items.isNotEmpty(),
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            val error = state.error
            when {
                state.isLoading && state.items.isEmpty() -> LoadingState()
                error != null && state.items.isEmpty() -> EmptyOrError(
                    message = error.toMessage(),
                    onRetry = viewModel::retry,
                )
                state.items.isEmpty() -> EmptyOrError(message = state.source.emptyMessage())
                else -> TodoList(
                    state = state,
                    listState = listState,
                    onOpenIssue = onOpenIssue,
                    onOpenPull = onOpenPull,
                    onLoadMore = viewModel::loadMore,
                    onRetry = viewModel::retry,
                )
            }
        }
    }
}

@Composable
private fun CountPill(label: String) {
    val colors = MaterialTheme.colorScheme
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = colors.onSurfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(colors.surfaceVariant)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

@Composable
private fun SourceChipRow(
    sources: List<TodoSource>,
    selected: TodoSource,
    onSelect: (TodoSource) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        sources.forEach { source ->
            FilterChip(
                selected = source == selected,
                onClick = { onSelect(source) },
                label = {
                    Text(
                        text = source.chipLabel(),
                        fontWeight = if (source == selected) FontWeight.SemiBold else FontWeight.Medium,
                    )
                },
                shape = RoundedCornerShape(999.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = colors.primary,
                    selectedLabelColor = colors.onPrimary,
                    containerColor = colors.surfaceVariant,
                    labelColor = colors.onSurfaceVariant,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = source == selected,
                    borderColor = colors.outline,
                    selectedBorderColor = colors.primary,
                ),
                modifier = Modifier.testTag("todo-source-${source.name}"),
            )
        }
    }
}

@Composable
private fun TodoList(
    state: TodoUiState,
    listState: LazyListState,
    onOpenIssue: (owner: String, repo: String, number: Int) -> Unit,
    onOpenPull: (owner: String, repo: String, number: Int) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
) {
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .testTag("todo-list"),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        itemsIndexed(
            items = state.items,
            key = { _, item -> item.dedupeKey },
        ) { _, item ->
            TodoRow(
                item = item,
                onClick = { openTodo(item, onOpenIssue, onOpenPull) },
            )
        }
        if (!state.endReached && !state.isLoading) {
            item(key = "todo-footer") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    LaunchedEffect(state.items.size, state.source) {
                        if (listState.firstVisibleItemIndex > 0 || listState.canScrollForward) {
                            onLoadMore()
                        }
                    }
                    if (state.isLoadingMore) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(8.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
        if (state.error != null) {
            item(key = "todo-error") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
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
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 2.dp,
        )
    }
}

@Composable
private fun EmptyOrError(
    message: String,
    onRetry: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.surface)
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = colors.primary,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
            if (onRetry != null) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onRetry) {
                    Text(text = "重试")
                }
            }
        }
    }
}

private fun openTodo(
    item: TodoItem,
    onOpenIssue: (owner: String, repo: String, number: Int) -> Unit,
    onOpenPull: (owner: String, repo: String, number: Int) -> Unit,
) {
    val (owner, repo) = item.repoPath.splitOwnerRepo() ?: return
    when (item.kind) {
        TodoKind.Issue -> onOpenIssue(owner, repo, item.number)
        TodoKind.PullRequest -> onOpenPull(owner, repo, item.number)
    }
}

/** `owner/repo`；路径非法时返回 null。 */
internal fun String?.splitOwnerRepo(): Pair<String, String>? {
    if (this.isNullOrBlank()) return null
    val slash = indexOf('/')
    if (slash <= 0 || slash == lastIndex) return null
    val owner = substring(0, slash)
    val repo = substring(slash + 1)
    if (owner.isBlank() || repo.isBlank() || repo.contains('/')) return null
    return owner to repo
}

private fun TodoUiState.countLabel(): String =
    if (totalCount != null) {
        "$totalCount · ${items.size}"
    } else {
        "${items.size} 条"
    }

private fun TodoGroup.label(): String = when (this) {
    TodoGroup.Pending -> "待处理"
    TodoGroup.Following -> "跟进"
}

private fun TodoSource.chipLabel(): String = when (this) {
    TodoSource.AssignedIssues -> "指派给我"
    TodoSource.NeedMyApprove -> "待批准"
    TodoSource.NeedMyReview -> "待评审"
    TodoSource.CreatedIssues -> "我创建的"
    TodoSource.CreatedPulls -> "我发起的"
}

private fun TodoSource.emptyMessage(): String = when (this) {
    TodoSource.AssignedIssues -> "没有指派给你的 Issue"
    TodoSource.NeedMyApprove -> "没有待你批准的 PR"
    TodoSource.NeedMyReview -> "没有待你评审的 PR"
    TodoSource.CreatedIssues -> "你还没有创建过 Issue"
    TodoSource.CreatedPulls -> "你还没有发起过 PR"
}

private fun ApiError.toMessage(): String = when (this) {
    ApiError.Unauthorized -> "登录状态已失效"
    ApiError.Forbidden -> "没有查看权限"
    ApiError.Network -> "网络连接失败"
    else -> "加载失败"
}
