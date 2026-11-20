package com.hfad.atomgit.ui.repo

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hfad.atomgit.data.model.CommitSummary
import com.hfad.atomgit.data.model.RepoEntryKind
import com.hfad.atomgit.data.model.RepoRef
import com.hfad.atomgit.data.model.RepoRefKind
import com.hfad.atomgit.data.remote.error.ApiError
import com.hfad.atomgit.ui.theme.RepoFilterMenuDark

@Composable
fun RepoDetailScreen(
    owner: String,
    repo: String,
    onBack: () -> Unit,
    onOpenFile: (path: String, ref: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RepoDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(owner, repo) { viewModel.start(owner, repo) }
    BackHandler(state.tab == RepoDetailTab.Files && state.files.path.isNotEmpty()) { viewModel.navigateUp() }
    RepoDetailContent(
        state = state,
        onBack = { if (!viewModel.navigateUp()) onBack() },
        onTab = viewModel::selectTab,
        onDirectory = { viewModel.openDirectory(it) },
        onRef = viewModel::selectRef,
        onRefresh = viewModel::refresh,
        onRetry = viewModel::retryCurrent,
        onRetryHeader = viewModel::retryHeader,
        onRetryReadme = viewModel::retryReadme,
        onLoadMore = viewModel::loadMore,
        onOpenFile = { path -> state.selectedRef?.let { onOpenFile(path, it.requestRef) } },
        modifier = modifier,
    )
}

@Composable
internal fun RepoDetailContent(
    state: RepoDetailUiState,
    onBack: () -> Unit,
    onTab: (RepoDetailTab) -> Unit,
    onDirectory: (String) -> Unit,
    onRef: (RepoRef) -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onRetryHeader: () -> Unit,
    onRetryReadme: () -> Unit,
    onLoadMore: () -> Unit,
    onOpenFile: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    var menuExpanded by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    val menuColor = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) RepoFilterMenuDark else MaterialTheme.colorScheme.surface
    Surface(modifier = modifier.fillMaxSize().testTag("screen-repo-detail")) {
        Column {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("repo-detail-back")) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, "返回")
                }
                Text(
                    "${state.owner}/${state.repo}",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Box {
                    IconButton(onClick = { menuExpanded = true }) { Icon(Icons.Outlined.MoreVert, "更多") }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }, containerColor = menuColor) {
                        DropdownMenuItem(text = { Text("刷新") }, onClick = { menuExpanded = false; onRefresh() })
                        DropdownMenuItem(
                            text = { Text("在网页打开") }, enabled = state.detail?.htmlUrl != null,
                            onClick = {
                                menuExpanded = false
                                state.detail?.htmlUrl?.let { url ->
                                    if (url.startsWith("https://") || url.startsWith("http://")) {
                                        runCatching { uriHandler.openUri(url) }.onFailure { notice = "无法打开网页" }
                                    } else notice = "仓库网页地址不可用"
                                }
                            },
                        )
                    }
                }
            }
            val detail = state.detail
            when {
                detail == null && state.isLoading -> LoadingPane()
                detail == null && state.error != null -> ErrorPane(state.error.message("仓库不存在或无权访问"), onRetryHeader)
                detail != null -> {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(detail.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (detail.isPrivate) Icon(Icons.Outlined.Lock, "私有仓库", modifier = Modifier.size(18.dp))
                        }
                        detail.description?.let {
                            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Outlined.Star, null, Modifier.size(16.dp), tint = Color(0xFFF5C518))
                            Text("${detail.stars}", style = MaterialTheme.typography.bodySmall)
                            detail.defaultBranch?.let { Text("默认分支：$it", modifier = Modifier.padding(start = 12.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        }
                        if (state.error != null) InlineError(state.error.message("仓库信息暂时不可用"), onRetryHeader)
                    }
                    SecondaryTabRow(selectedTabIndex = state.tab.ordinal, containerColor = Color.Transparent) {
                        RepoDetailTab.entries.forEach { tab ->
                            Tab(selected = state.tab == tab, onClick = { onTab(tab) }, text = { Text(tab.label) }, modifier = Modifier.testTag("repo-tab-${tab.name}"))
                        }
                    }
                    val holder = rememberSaveableStateHolder()
                    val revision = state.selectedRef
                    val versionKey = "${revision?.kind}:${revision?.name}"
                    val listKey = when (state.tab) {
                        RepoDetailTab.Files -> "files:$versionKey:${state.files.path}"
                        RepoDetailTab.Commits -> "commits:$versionKey"
                        else -> state.tab.name
                    }
                    Box(Modifier.weight(1f)) {
                        key(listKey) {
                            holder.SaveableStateProvider(listKey) {
                                val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
                                when (state.tab) {
                                    RepoDetailTab.Files -> FilesPane(state, listState, onDirectory, onOpenFile, onRetry, onRetryReadme, { onTab(RepoDetailTab.Branches) }, { notice = "暂不支持预览此类型，请在网页查看" })
                                    RepoDetailTab.Commits -> PagedPane(state.commits, listState, "暂无提交", onRetry, onLoadMore, { it.sha }) { CommitRow(it) }
                                    RepoDetailTab.Branches -> PagedPane(state.branches, listState, "暂无分支", onRetry, onLoadMore, { it.name }) { RefRow(it, state, onRef) }
                                    RepoDetailTab.Tags -> PagedPane(state.tags, listState, "暂无标签", onRetry, onLoadMore, { it.name }) { RefRow(it, state, onRef) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    notice?.let { text ->
        AlertDialog(onDismissRequest = { notice = null }, text = { Text(text) }, confirmButton = { TextButton(onClick = { notice = null }) { Text("知道了") } })
    }
}

@Composable
private fun FilesPane(
    state: RepoDetailUiState,
    listState: LazyListState,
    onDirectory: (String) -> Unit,
    onFile: (String) -> Unit,
    onRetry: () -> Unit,
    onRetryReadme: () -> Unit,
    onChooseRef: () -> Unit,
    onUnsupported: () -> Unit,
) {
    Column {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onChooseRef, modifier = Modifier.testTag("repo-choose-ref")) {
                Text("${if (state.selectedRef?.kind == RepoRefKind.Tag) "标签" else "分支"}：${state.selectedRef?.name.orEmpty()}", maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                Icon(Icons.Outlined.KeyboardArrowDown, null, Modifier.size(18.dp))
            }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { onDirectory("") }) { Text("根目录") }
            val segments = state.files.path.split('/').filter { it.isNotEmpty() }
            segments.forEachIndexed { index, segment ->
                Text("/", color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { onDirectory(segments.take(index + 1).joinToString("/")) }) { Text(segment) }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        when {
            state.files.isLoading -> LoadingPane()
            state.files.error != null -> ErrorPane(state.files.error.message("目录或版本不存在"), onRetry)
            else -> LazyColumn(state = listState, modifier = Modifier.fillMaxSize().testTag("repo-files"), contentPadding = PaddingValues(bottom = 16.dp)) {
                if (state.files.entries.isEmpty()) item { EmptyPane("当前目录为空") }
                items(state.files.entries, key = { it.path }) { entry ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            when (entry.kind) {
                                RepoEntryKind.Directory -> onDirectory(entry.path)
                                RepoEntryKind.File -> onFile(entry.path)
                                RepoEntryKind.Unsupported -> onUnsupported()
                            }
                        }.padding(horizontal = 16.dp, vertical = 16.dp).testTag("repo-entry:${entry.path}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(if (entry.kind == RepoEntryKind.Directory) FolderIcon else FileIcon, null, Modifier.size(22.dp), tint = if (entry.kind == RepoEntryKind.Directory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(entry.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (entry.kind == RepoEntryKind.Directory) Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                if (state.files.path.isEmpty()) {
                    val readme = state.readme
                    if (readme.isLoading || readme.preview != null || readme.error != null) item(key = "readme-preview") {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(readme.path?.substringAfterLast('/') ?: "README", style = MaterialTheme.typography.titleMedium)
                            when {
                                readme.isLoading -> CircularProgressIndicator(Modifier.size(24.dp))
                                readme.error != null -> InlineError(readme.error.message("README 不存在"), onRetryReadme)
                                else -> {
                                    Text(readme.preview.orEmpty().ifEmpty { "文件内容为空" }, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, maxLines = 30, overflow = TextOverflow.Ellipsis)
                                    readme.path?.let { path -> TextButton(onClick = { onFile(path) }) { Text("查看全文") } }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun <T> PagedPane(
    page: RepoPageState<T>,
    listState: LazyListState,
    empty: String,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    itemKey: (T) -> String,
    row: @Composable (T) -> Unit,
) {
    when {
        page.isLoading && page.items.isEmpty() -> LoadingPane()
        page.error != null && page.items.isEmpty() -> ErrorPane(page.error.message("仓库或版本不存在"), onRetry)
        page.loaded && page.items.isEmpty() -> EmptyPane(empty)
        else -> LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
            items(page.items, key = itemKey) {
                row(it)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            item(key = "paging-footer") {
                when {
                    page.error != null -> InlineError(page.error.message("仓库或版本不存在"), onRetry)
                    page.isLoading || page.isLoadingMore -> Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(24.dp)) }
                    !page.endReached -> TextButton(onClick = onLoadMore, modifier = Modifier.fillMaxWidth().testTag("repo-load-more")) { Text("加载更多") }
                }
            }
        }
    }
}

@Composable
private fun CommitRow(commit: CommitSummary) {
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(commit.message, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(commit.sha.take(7), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            commit.date?.let { Text(it.substringBefore('T'), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
private fun RefRow(ref: RepoRef, state: RepoDetailUiState, onRef: (RepoRef) -> Unit) {
    val selected = state.selectedRef?.let { it.kind == ref.kind && it.name == ref.name && it.requestRef == ref.requestRef } == true
    Row(Modifier.fillMaxWidth().clickable { onRef(ref) }.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(ref.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (ref.isDefault || ref.kind == RepoRefKind.Branch && ref.name == state.detail?.defaultBranch) Text("默认分支", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (selected) Icon(Icons.Outlined.Check, "当前版本", tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun LoadingPane() { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }

@Composable
private fun EmptyPane(text: String) { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant) } }

@Composable
private fun ErrorPane(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { InlineError(message, onRetry) }
}

@Composable
private fun InlineError(message: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(message, style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = onRetry) { Text("重试") }
    }
}

private fun ApiError.message(notFound: String): String = when (this) {
    ApiError.NotFound -> notFound
    ApiError.Unauthorized -> "登录状态已失效"
    ApiError.Forbidden -> "没有权限访问"
    ApiError.Network -> "网络连接失败"
    ApiError.Parse -> "数据解析失败"
    is ApiError.RateLimited -> "请求过于频繁，请稍后重试"
    else -> "加载失败，请稍后重试"
}

private val FolderIcon = ImageVector.Builder("RepoFolder", 24.dp, 24.dp, 24f, 24f).apply {
    path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.6f) {
        moveTo(3f, 6f); lineTo(9f, 6f); lineTo(11f, 8f); lineTo(21f, 8f)
        lineTo(21f, 19f); lineTo(3f, 19f); close()
    }
}.build()

private val FileIcon = ImageVector.Builder("RepoFile", 24.dp, 24.dp, 24f, 24f).apply {
    path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.6f) {
        moveTo(5f, 3f); lineTo(14f, 3f); lineTo(19f, 8f); lineTo(19f, 21f)
        lineTo(5f, 21f); close(); moveTo(14f, 3f); lineTo(14f, 8f); lineTo(19f, 8f)
    }
}.build()
