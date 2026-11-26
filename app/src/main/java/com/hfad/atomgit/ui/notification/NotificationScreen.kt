package com.hfad.atomgit.ui.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hfad.atomgit.data.model.NotificationTarget
import com.hfad.atomgit.data.remote.error.ApiError
import com.hfad.atomgit.ui.components.AtomPullRefreshIndicator
import com.hfad.atomgit.ui.components.InboxLayout
import com.hfad.atomgit.ui.components.InboxScreenTitleBar
import kotlin.math.absoluteValue
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun NotificationScreen(
    listState: LazyListState,
    onOpenIssue: (owner: String, repo: String, number: Int) -> Unit,
    onOpenPull: (owner: String, repo: String, number: Int) -> Unit,
    onOpenRepo: (owner: String, repo: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NotificationViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    LaunchedEffect(Unit) { viewModel.start() }
    LaunchedEffect(state.importantOnly, state.kindFilter, state.selectedRepoKey) {
        if (listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0) {
            listState.scrollToItem(0)
        }
    }
    NotificationInbox(
        state = state,
        listState = listState,
        onToggleImportant = viewModel::toggleImportant,
        onClearFilters = viewModel::clearFilters,
        onSelectKind = viewModel::selectKind,
        onSelectRepo = viewModel::selectRepo,
        onRefresh = viewModel::refresh,
        onRetry = viewModel::retry,
        onLoadMore = viewModel::loadMore,
        onToggleRead = viewModel::toggleRead,
        onOpen = { thread ->
            viewModel.markRead(thread)
            when (val target = thread.target) {
                is NotificationTarget.Issue -> onOpenIssue(target.owner, target.repo, target.number)
                is NotificationTarget.PullRequest -> onOpenPull(target.owner, target.repo, target.number)
                is NotificationTarget.Web -> {
                    val opened = runCatching { uriHandler.openUri(target.url) }.isSuccess
                    if (!opened) viewModel.showNotice("无法打开网页")
                }
                is NotificationTarget.Repository -> onOpenRepo(target.owner, target.repo)
            }
        },
        modifier = modifier,
    )
}

@Composable
fun NotificationInbox(
    state: NotificationUiState,
    listState: LazyListState,
    onToggleImportant: () -> Unit,
    onClearFilters: () -> Unit,
    onSelectKind: (NotificationKindFilter) -> Unit,
    onSelectRepo: (String?) -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onToggleRead: (NotificationThread) -> Unit,
    onOpen: (NotificationThread) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val refreshing = state.isLoading && state.threads.isNotEmpty()
    val pullState = rememberPullToRefreshState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surfaceContainerLow)
            .testTag("screen-notification"),
    ) {
        Surface(color = colors.surface) {
            Column(modifier = Modifier.fillMaxWidth()) {
                InboxScreenTitleBar(title = "通知") {
                    Box {
                        var menuExpanded by remember { mutableStateOf(false) }
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Outlined.MoreVert, contentDescription = "更多")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            containerColor = colors.surface,
                        ) {
                            DropdownMenuItem(
                                text = { Text("刷新") },
                                onClick = {
                                    menuExpanded = false
                                    onRefresh()
                                },
                            )
                        }
                    }
                }
                FilterRow(
                    state = state,
                    onToggleImportant = onToggleImportant,
                    onClearFilters = onClearFilters,
                    onSelectKind = onSelectKind,
                    onSelectRepo = onSelectRepo,
                )
                val notice = state.notice
                if (state.partialFailures.isNotEmpty() || notice != null) {
                    Column(
                        modifier = Modifier.padding(
                            horizontal = InboxLayout.screenHorizontal,
                            vertical = 4.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        if (state.partialFailures.isNotEmpty()) {
                            Text(
                                text = "有 ${state.partialFailures.size} 个仓库没有同步",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant,
                            )
                        }
                        if (notice != null) {
                            Text(
                                text = notice,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.error,
                            )
                        }
                    }
                }
            }
        }

        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = onRefresh,
            state = pullState,
            modifier = Modifier.fillMaxSize(),
            indicator = {
                AtomPullRefreshIndicator(
                    state = pullState,
                    isRefreshing = refreshing,
                )
            },
        ) {
            val error = state.error
            when {
                state.isLoading && state.threads.isEmpty() -> LoadingState()
                error != null && state.threads.isEmpty() -> MessageState(
                    message = error.toMessage(),
                    onRetry = onRetry,
                )
                state.threads.isEmpty() -> MessageState(message = state.emptyMessage())
                else -> NotificationList(
                    state = state,
                    listState = listState,
                    onOpen = onOpen,
                    onToggleRead = onToggleRead,
                    onLoadMore = onLoadMore,
                    onRetry = onRetry,
                )
            }
        }
    }
}

@Composable
private fun FilterRow(
    state: NotificationUiState,
    onToggleImportant: () -> Unit,
    onClearFilters: () -> Unit,
    onSelectKind: (NotificationKindFilter) -> Unit,
    onSelectRepo: (String?) -> Unit,
) {
    var sheet by remember { mutableStateOf<FilterSheet?>(null) }
    val repoLabel = state.repos.firstOrNull { it.key == state.selectedRepoKey }?.label ?: "仓库"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(
                horizontal = InboxLayout.screenHorizontal,
                vertical = InboxLayout.filterBarVertical,
            ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (state.hasActiveFilter) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ClearFiltersButton(onClick = onClearFilters)
                InboxChip(
                    label = state.kindFilter.chipLabel(),
                    selected = state.kindFilter != NotificationKindFilter.All,
                    showChevron = true,
                    onClick = { sheet = FilterSheet.Inbox },
                    testTag = "notification-inbox",
                )
            }
        } else {
            InboxChip(
                label = state.kindFilter.chipLabel(),
                selected = false,
                showChevron = true,
                onClick = { sheet = FilterSheet.Inbox },
                testTag = "notification-inbox",
            )
        }
        InboxChip(
            label = "重点",
            selected = state.importantOnly,
            showChevron = false,
            onClick = onToggleImportant,
            testTag = "notification-important",
        )
        InboxChip(
            label = repoLabel,
            selected = state.selectedRepoKey != null,
            showChevron = true,
            onClick = { sheet = FilterSheet.Repo },
            testTag = "notification-repo",
        )
    }
    when (sheet) {
        FilterSheet.Inbox -> ChoiceSheet(
            title = "收件箱",
            options = NotificationKindFilter.entries.map { Choice(it.name, it.chipLabel()) },
            selectedKey = state.kindFilter.name,
            onSelect = { key ->
                onSelectKind(NotificationKindFilter.valueOf(key))
                sheet = null
            },
            onDismiss = { sheet = null },
        )
        FilterSheet.Repo -> ChoiceSheet(
            title = "仓库",
            options = listOf(Choice("", "全部仓库")) + state.repos.map { Choice(it.key, it.label) },
            selectedKey = state.selectedRepoKey.orEmpty(),
            onSelect = { key ->
                onSelectRepo(key.ifBlank { null })
                sheet = null
            },
            onDismiss = { sheet = null },
        )
        null -> Unit
    }
}

private enum class FilterSheet { Inbox, Repo }

private data class Choice(val key: String, val label: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChoiceSheet(
    title: String,
    options: List<Choice>,
    selectedKey: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = MaterialTheme.colorScheme
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        contentColor = colors.onSurface,
        tonalElevation = 0.dp,
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            options.forEach { option ->
                val selected = option.key == selectedKey
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clickable { onSelect(option.key) }
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = option.label,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    RadioButton(
                        selected = selected,
                        onClick = null,
                        colors = RadioButtonDefaults.colors(
                            selectedColor = colors.primary,
                            unselectedColor = colors.onSurfaceVariant,
                        ),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ClearFiltersButton(onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(32.dp)
            .border(width = 1.dp, color = colors.outlineVariant, shape = CircleShape)
            .background(colors.surfaceContainerHigh, CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .testTag("notification-clear-filters"),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Close,
            contentDescription = "清除筛选",
            tint = colors.onSurface,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun InboxChip(
    label: String,
    selected: Boolean,
    showChevron: Boolean,
    onClick: () -> Unit,
    testTag: String,
) {
    val colors = MaterialTheme.colorScheme
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                softWrap = false,
            )
        },
        trailingIcon = if (showChevron) {
            {
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
        } else {
            null
        },
        shape = MaterialTheme.shapes.extraLarge,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = colors.secondaryContainer,
            selectedLabelColor = colors.onSecondaryContainer,
            containerColor = colors.surfaceContainerHigh,
            labelColor = colors.onSurface,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = colors.outlineVariant,
            selectedBorderColor = colors.secondary,
        ),
        modifier = Modifier.testTag(testTag),
    )
}

@Composable
private fun NotificationList(
    state: NotificationUiState,
    listState: LazyListState,
    onOpen: (NotificationThread) -> Unit,
    onToggleRead: (NotificationThread) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
) {
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .testTag("notification-list"),
        contentPadding = PaddingValues(
            top = InboxLayout.listTop,
            bottom = InboxLayout.listBottom,
        ),
    ) {
        items(
            items = state.threads,
            key = { it.key },
        ) { thread ->
            SwipeableNotificationRow(
                thread = thread,
                onOpen = { onOpen(thread) },
                onToggleRead = { onToggleRead(thread) },
            )
        }
        if (!state.endReached && !state.isLoading) {
            item(key = "notification-footer") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    LaunchedEffect(state.threads.size, state.kindFilter, state.importantOnly, state.selectedRepoKey) {
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
            item(key = "notification-error") {
                Box(
                    modifier = Modifier.fillMaxWidth(),
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

private val SwipeCheckStart = Color(0xFFC8E6C9)
private val SwipeCheckEnd = Color(0xFF2E7D32)
private val SwipeUnreadStart = Color(0xFFC5CAE9)
private val SwipeUnreadEnd = Color(0xFF3949AB)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableNotificationRow(
    thread: NotificationThread,
    onOpen: () -> Unit,
    onToggleRead: () -> Unit,
) {
    val density = LocalDensity.current
    val latestToggle = rememberUpdatedState(onToggleRead)
    // 约拖出行宽 28% 松手即结算。
    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { distance -> distance * 0.28f },
    )
    // 背景图标锁定本次滑动意图；onDismiss 必须稳定，否则 settled 在 EndToStart 时会反复回调导致闪烁。
    var showCheck by remember(thread.key) { mutableStateOf(thread.unread) }
    val dismissConsumed = remember(thread.key) { AtomicBoolean(false) }
    val onDismiss = remember(thread.key) {
        { direction: SwipeToDismissBoxValue ->
            if (direction == SwipeToDismissBoxValue.EndToStart &&
                dismissConsumed.compareAndSet(false, true)
            ) {
                latestToggle.value()
            }
        }
    }
    LaunchedEffect(dismissState.settledValue) {
        when (dismissState.settledValue) {
            SwipeToDismissBoxValue.Settled -> {
                showCheck = thread.unread
                dismissConsumed.set(false)
            }
            SwipeToDismissBoxValue.EndToStart -> {
                // 收件箱标已读后条目会从列表移除；若仍留在树上（事件/@我），再弹回。
                kotlinx.coroutines.yield()
                if (dismissState.settledValue == SwipeToDismissBoxValue.EndToStart) {
                    dismissState.reset()
                }
            }
            else -> Unit
        }
    }
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        onDismiss = onDismiss,
        backgroundContent = {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.CenterEnd,
            ) {
                val widthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
                val progress = (dismissState.requireOffset().absoluteValue / (widthPx * 0.36f))
                    .coerceIn(0f, 1f)
                val scale = 0.55f + progress * 0.85f
                val alpha = 0.35f + progress * 0.65f
                Icon(
                    imageVector = if (showCheck) Icons.Outlined.Check else Icons.Outlined.Notifications,
                    contentDescription = if (showCheck) "标为已读" else "标为未读",
                    tint = if (showCheck) {
                        lerp(SwipeCheckStart, SwipeCheckEnd, progress)
                    } else {
                        lerp(SwipeUnreadStart, SwipeUnreadEnd, progress)
                    },
                    modifier = Modifier
                        .padding(end = 22.dp)
                        .size(28.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            this.alpha = alpha
                        },
                )
            }
        },
    ) {
        NotificationRow(
            thread = thread,
            onClick = onOpen,
        )
    }
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 2.dp,
        )
    }
}

@Composable
private fun MessageState(
    message: String,
    onRetry: (() -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
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

private fun NotificationKindFilter.chipLabel(): String = when (this) {
    NotificationKindFilter.All -> "收件箱"
    NotificationKindFilter.Event -> "事件"
    NotificationKindFilter.Mention -> "@我"
    NotificationKindFilter.Read -> "已读"
}

private fun NotificationUiState.emptyMessage(): String = when {
    repos.isEmpty() && !isLoading -> "最近没有可拉取通知的项目"
    selectedRepoKey != null && kindFilter == NotificationKindFilter.Read -> "这个仓库没有已读通知"
    selectedRepoKey != null && kindFilter == NotificationKindFilter.All -> "这个仓库没有未读通知"
    selectedRepoKey != null -> "这个仓库没有通知"
    importantOnly -> "没有重点通知"
    kindFilter == NotificationKindFilter.Read -> "没有已读通知"
    kindFilter == NotificationKindFilter.All -> "没有未读通知"
    kindFilter != NotificationKindFilter.All -> "没有这类通知"
    else -> "没有通知"
}

private fun ApiError.toMessage(): String = when (this) {
    ApiError.Unauthorized -> "登录状态已失效"
    ApiError.Forbidden -> "没有查看权限"
    ApiError.Network -> "网络连接失败"
    else -> "加载失败"
}
