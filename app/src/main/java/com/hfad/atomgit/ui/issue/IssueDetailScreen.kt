package com.hfad.atomgit.ui.issue

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hfad.atomgit.data.model.IssueComment
import com.hfad.atomgit.data.model.IssueDetail
import com.hfad.atomgit.data.model.IssueLabel
import com.hfad.atomgit.data.remote.error.ApiError
import com.hfad.atomgit.navigation.IssueDetailRoute
import com.hfad.atomgit.ui.components.Avatar
import com.hfad.atomgit.ui.components.M3CardPadding
import com.hfad.atomgit.ui.components.M3LowCard
import com.hfad.atomgit.ui.components.M3RaisedCard
import com.hfad.atomgit.data.repository.mapping.parseNotificationTime
import com.hfad.atomgit.ui.theme.AccentBlue
import com.hfad.atomgit.ui.theme.AccentBlueBright
import com.hfad.atomgit.ui.theme.AccentBlueContainer
import com.hfad.atomgit.ui.theme.AccentBlueContainerDark
import java.util.Locale

private val ClosedPurple = Color(0xFF8250DF)
private val ClosedPurpleContainer = Color(0xFFF3EEFF)

@Composable
fun IssueDetailScreen(
    route: IssueDetailRoute,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: IssueDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(route.owner, route.repo, route.number) {
        viewModel.start(route.owner, route.repo, route.number)
    }
    IssueDetailContent(
        state = state,
        onBack = onBack,
        onRefresh = viewModel::refresh,
        onRetry = viewModel::retry,
        onRetryComments = viewModel::retryComments,
        onLoadMoreComments = viewModel::loadMoreComments,
        onDraftChange = viewModel::updateDraft,
        onPostComment = viewModel::postComment,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun IssueDetailContent(
    state: IssueDetailUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onRetryComments: () -> Unit,
    onLoadMoreComments: () -> Unit,
    onDraftChange: (String) -> Unit,
    onPostComment: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    var menuExpanded by remember { mutableStateOf(false) }
    var commentSheetOpen by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    val commentSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val pageUrl = issuePageUrl(state.issue?.htmlUrl, state.owner, state.repo, state.number)
    var wasPosting by remember { mutableStateOf(false) }
    LaunchedEffect(state.isPosting) {
        if (wasPosting && !state.isPosting && state.postError == null && state.draft.isEmpty()) {
            commentSheetOpen = false
        }
        wasPosting = state.isPosting
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("screen-issue-detail"),
    ) {
        IssueTopBar(
            menuExpanded = menuExpanded,
            onBack = onBack,
            onShare = { openIssuePage(uriHandler, pageUrl) { notice = it } },
            onMenuToggle = { menuExpanded = !menuExpanded },
            onDismissMenu = { menuExpanded = false },
            onRefresh = onRefresh,
            onOpenWeb = {
                menuExpanded = false
                openIssuePage(uriHandler, pageUrl) { notice = it }
            },
        )
        when {
            state.issue == null && state.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            state.issue == null && state.error != null -> {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(state.error.issueMessage())
                    TextButton(onClick = onRetry) { Text("重试") }
                    TextButton(
                        onClick = { openIssuePage(uriHandler, pageUrl) { notice = it } },
                        modifier = Modifier.testTag("issue-open-web"),
                    ) {
                        Text("在网页打开")
                    }
                }
            }
            state.issue != null -> {
                val issue = state.issue
                val nowMillis = remember { System.currentTimeMillis() }
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 96.dp),
                    ) {
                        state.syncMessage?.let { message ->
                            item(key = "sync") {
                                Text(
                                    text = message,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                        .testTag("issue-sync"),
                                )
                            }
                        }
                        state.error?.let { error ->
                            item(key = "issue-error") {
                                InlineMessage(error.issueMessage(), "重试", onRetry)
                            }
                        }
                        item(key = "header") {
                            IssueHeader(
                                issue = issue,
                                owner = state.owner,
                                repo = state.repo,
                                nowMillis = nowMillis,
                            )
                        }
                        item(key = "comments-divider") {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.padding(top = 16.dp),
                            )
                            IssueCommentsSectionTitle(count = state.comments.size)
                        }
                        itemsIndexed(
                            items = state.comments,
                            key = { index, comment ->
                                comment.id?.let { "comment-$it" } ?: "comment-$index"
                            },
                        ) { index, comment ->
                            CommentTimelineRow(
                                comment = comment,
                                nowMillis = nowMillis,
                                showConnector = index < state.comments.lastIndex || !state.commentsEndReached,
                            )
                        }
                        if (state.comments.isEmpty() && state.commentsError == null && !state.isLoadingComments) {
                            item(key = "comments-empty") {
                                Text(
                                    text = "还没有评论",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                )
                            }
                        }
                        if (state.commentsError != null) {
                            item(key = "comments-error") {
                                InlineMessage(state.commentsError.commentsMessage(), "重试", onRetryComments)
                            }
                        } else if (!state.commentsEndReached && state.comments.isNotEmpty()) {
                            item(key = "comments-more") {
                                Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                                    if (state.isLoadingComments) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                            strokeWidth = 2.dp,
                                        )
                                    } else {
                                        TextButton(onClick = onLoadMoreComments) { Text("更多评论") }
                                    }
                                }
                            }
                        }
                        item(key = "issue-journey") {
                            IssueJourneyPanel(issue = issue, comments = state.comments, nowMillis = nowMillis)
                        }
                    }
                    CommentFab(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 20.dp),
                        onClick = { commentSheetOpen = true },
                    )
                }
            }
        }
    }
    if (commentSheetOpen && state.issue != null) {
        ModalBottomSheet(
            onDismissRequest = { commentSheetOpen = false },
            sheetState = commentSheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                Text(
                    text = "发表评论",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                )
                CommentComposer(
                    draft = state.draft,
                    posting = state.isPosting,
                    error = state.postError?.commentPostMessage(),
                    onDraftChange = onDraftChange,
                    onPost = onPostComment,
                )
            }
        }
    }
    notice?.let { text ->
        AlertDialog(
            onDismissRequest = { notice = null },
            text = { Text(text) },
            confirmButton = {
                TextButton(onClick = { notice = null }) { Text("知道了") }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IssueTopBar(
    menuExpanded: Boolean,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onMenuToggle: () -> Unit,
    onDismissMenu: () -> Unit,
    onRefresh: () -> Unit,
    onOpenWeb: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    TopAppBar(
        // Scaffold 已预留状态栏高度，避免 TopAppBar 再叠一层出现顶部白条
        windowInsets = WindowInsets(0, 0, 0, 0),
        title = {},
        navigationIcon = {
            IconButton(onClick = onBack, modifier = Modifier.testTag("issue-detail-back")) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
            }
        },
        actions = {
            IconButton(onClick = onShare) {
                Icon(Icons.Outlined.Share, contentDescription = "分享")
            }
            Box {
                IconButton(onClick = onMenuToggle) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "更多")
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = onDismissMenu,
                    containerColor = colors.surfaceContainerHigh,
                ) {
                    DropdownMenuItem(
                        text = { Text("刷新") },
                        onClick = {
                            onDismissMenu()
                            onRefresh()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("在网页打开") },
                        onClick = onOpenWeb,
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = colors.surface,
        ),
    )
}

@Composable
private fun IssueHeader(
    issue: IssueDetail,
    owner: String,
    repo: String,
    nowMillis: Long,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IssueRepoRow(issue, owner, repo)
        Text(
            text = issueNumberedTitle(issue),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        IssueStatusRow(issue)
        IssueAuthorRow(issue, nowMillis)
        if (issue.assignees.isNotEmpty()) {
            Text(
                text = "指派给 ${issue.assignees.joinToString("、")}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IssueBodyPanel(body = issue.body)
    }
}

@Composable
private fun IssueBodyPanel(body: String?) {
    M3LowCard(Modifier.fillMaxWidth()) {
        IssueMarkdownContent(
            content = body,
            placeholder = "（无描述）",
            modifier = Modifier.padding(M3CardPadding),
        )
    }
}

@Composable
private fun IssueCommentsSectionTitle(count: Int) {
    val label = if (count > 0) "评论（$count）" else "评论"
    Text(
        text = label,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IssueStatusRow(issue: IssueDetail) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        issue.displayStateLabel()?.let { label ->
            StatusChip(
                text = label,
                icon = if (label.contains("关闭")) Icons.Outlined.CheckCircle else null,
                background = if (label.contains("关闭")) {
                    if (dark) Color(0xFF3D2D5C) else ClosedPurpleContainer
                } else {
                    statusColors(label, dark).first
                },
                foreground = if (label.contains("关闭")) {
                    if (dark) Color(0xFFCEBCFD) else ClosedPurple
                } else {
                    statusColors(label, dark).second
                },
            )
        }
        issue.issueState?.takeIf { it.isNotBlank() }?.let { business ->
            val (bg, fg) = statusColors(business, dark)
            StatusChip(text = business, background = bg, foreground = fg)
        }
        issue.labels.forEach { label ->
            IssueLabelChip(label, dark)
        }
    }
}

@Composable
private fun IssueRepoRow(issue: IssueDetail, owner: String, repo: String) {
    val repoLabel = formatRepoLabel(issue.repoPath, owner, repo)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.testTag("issue-repo-row"),
    ) {
        Avatar(name = repoLabel, avatarUrl = issue.repoAvatarUrl, size = 22.dp)
        Text(
            text = repoLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun IssueLabelChip(label: IssueLabel, dark: Boolean) {
    val parsed = parseLabelColor(label.color, dark)
    val (bg, fg) = parsed ?: if (dark) {
        AccentBlueContainerDark to AccentBlueBright
    } else {
        AccentBlueContainer to AccentBlue
    }
    StatusChip(text = label.name, background = bg, foreground = fg)
}

private fun issueNumberedTitle(issue: IssueDetail): String {
    val title = issue.title?.trim().orEmpty()
    return if (title.isEmpty()) "#${issue.number}" else "$title #${issue.number}"
}

private enum class IssueJourneyKind {
    Created,
    LabelsAdded,
    Commented,
    Updated,
    Closed,
}

private data class IssueJourneyEvent(
    val millis: Long,
    val rawAt: String?,
    val kind: IssueJourneyKind,
    val actorLogin: String,
    val labels: List<IssueLabel> = emptyList(),
    val cardText: String? = null,
    val detailLine: String? = null,
    val issueNumber: Int? = null,
)

private fun buildIssueJourney(issue: IssueDetail, comments: List<IssueComment>): List<IssueJourneyEvent> {
    val events = mutableListOf<IssueJourneyEvent>()
    val created = parseNotificationTime(issue.createdAt)
    val createdAuthor = issue.authorLogin?.takeIf { it.isNotBlank() } ?: issue.authorLabel
    if (created != null) {
        events += IssueJourneyEvent(
            millis = created,
            rawAt = issue.createdAt,
            kind = IssueJourneyKind.Created,
            actorLogin = createdAuthor,
        )
        if (issue.labels.isNotEmpty()) {
            events += IssueJourneyEvent(
                millis = created + 1,
                rawAt = issue.createdAt,
                kind = IssueJourneyKind.LabelsAdded,
                actorLogin = createdAuthor,
                labels = issue.labels,
            )
        }
    }
    comments.forEach { comment ->
        val millis = parseNotificationTime(comment.createdAt) ?: return@forEach
        val author = comment.authorLogin?.takeIf { it.isNotBlank() } ?: comment.authorLabel
        val preview = comment.body
            ?.lineSequence()
            ?.firstOrNull()
            ?.trim()
            ?.take(160)
            ?.takeIf { it.isNotEmpty() }
        events += IssueJourneyEvent(
            millis = millis,
            rawAt = comment.createdAt,
            kind = IssueJourneyKind.Commented,
            actorLogin = author,
            cardText = preview,
        )
    }
    val updated = parseNotificationTime(issue.updatedAt)
    if (updated != null && updated != created) {
        val isClosed = issue.state?.trim()?.lowercase(Locale.US) == "closed"
        val author = issue.authorLogin?.takeIf { it.isNotBlank() } ?: issue.authorLabel
        if (isClosed) {
            events += IssueJourneyEvent(
                millis = updated,
                rawAt = issue.updatedAt,
                kind = IssueJourneyKind.Closed,
                actorLogin = author,
                issueNumber = issue.number,
            )
        } else {
            val pieces = buildList {
                issue.displayStateLabel()?.let { add("状态：$it") }
                issue.issueState?.takeIf { it.isNotBlank() }?.let { add(it) }
            }
            events += IssueJourneyEvent(
                millis = updated,
                rawAt = issue.updatedAt,
                kind = IssueJourneyKind.Updated,
                actorLogin = author,
                detailLine = pieces.takeIf { it.isNotEmpty() }?.joinToString(" · "),
            )
        }
    }
    return events.sortedBy { it.millis }
}

@Composable
private fun IssueJourneyPanel(
    issue: IssueDetail,
    comments: List<IssueComment>,
    nowMillis: Long,
) {
    val events = remember(issue, comments) { buildIssueJourney(issue, comments) }
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val panelBackground = MaterialTheme.colorScheme.surfaceContainerLow
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .background(panelBackground)
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .testTag("issue-journey"),
    ) {
        Text(
            text = "历程",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(12.dp))
        if (events.isEmpty()) {
            Text(
                text = "暂无历程记录",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            events.forEachIndexed { index, event ->
                JourneyTimelineRow(
                    event = event,
                    nowMillis = nowMillis,
                    showConnector = index < events.lastIndex,
                    dark = dark,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun JourneyTimelineRow(
    event: IssueJourneyEvent,
    nowMillis: Long,
    showConnector: Boolean,
    dark: Boolean,
) {
    val lineColor = MaterialTheme.colorScheme.outlineVariant
    val connectorHeight = when {
        event.cardText != null -> 72.dp
        event.kind == IssueJourneyKind.LabelsAdded && event.labels.size > 1 -> 56.dp
        event.detailLine != null -> 56.dp
        else -> 36.dp
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(32.dp),
        ) {
            JourneyEventIcon(kind = event.kind, dark = dark)
            if (showConnector) {
                Box(
                    Modifier
                        .padding(top = 4.dp)
                        .width(2.dp)
                        .height(connectorHeight)
                        .background(lineColor),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(
            Modifier
                .weight(1f)
                .padding(bottom = if (showConnector) 8.dp else 0.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            when (event.kind) {
                IssueJourneyKind.LabelsAdded -> {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        JourneyActorActionText(
                            actorLogin = event.actorLogin,
                            action = "添加了",
                        )
                        event.labels.forEach { label ->
                            IssueLabelChip(label, dark)
                        }
                    }
                }
                else -> {
                    JourneyActorActionText(
                        actorLogin = event.actorLogin,
                        action = journeyActionSuffix(event),
                    )
                }
            }
            event.detailLine?.let { detail ->
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            event.cardText?.let { preview ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = if (dark) 0.dp else 1.dp,
                    tonalElevation = if (dark) 2.dp else 0.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = preview,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Text(
                text = formatIssueDisplayTime(event.rawAt, nowMillis)
                    ?: formatJourneyTimestamp(event.millis),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun JourneyActorActionText(actorLogin: String, action: String) {
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                append(actorLogin)
            }
            append(" ")
            append(action)
        },
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun JourneyEventIcon(kind: IssueJourneyKind, dark: Boolean) {
    val purpleClose = kind == IssueJourneyKind.Closed
    val (icon, tint) = when (kind) {
        IssueJourneyKind.Created -> Icons.Outlined.Add to MaterialTheme.colorScheme.onSurfaceVariant
        IssueJourneyKind.LabelsAdded -> Icons.Outlined.Star to MaterialTheme.colorScheme.onSurfaceVariant
        IssueJourneyKind.Commented -> Icons.Outlined.Create to MaterialTheme.colorScheme.onSurfaceVariant
        IssueJourneyKind.Updated -> Icons.Outlined.Create to MaterialTheme.colorScheme.onSurfaceVariant
        IssueJourneyKind.Closed -> Icons.Outlined.CheckCircle to Color.White
    }
    val background = when {
        purpleClose -> ClosedPurple
        dark -> MaterialTheme.colorScheme.surface
        else -> Color.White
    }
    Box(
        Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(background)
            .then(
                if (!purpleClose) {
                    Modifier.border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = CircleShape,
                    )
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(14.dp),
        )
    }
}

private fun journeyActionSuffix(event: IssueJourneyEvent): String = when (event.kind) {
    IssueJourneyKind.Created -> "创建了 Issue"
    IssueJourneyKind.LabelsAdded -> "添加了"
    IssueJourneyKind.Commented -> "发表了评论"
    IssueJourneyKind.Updated -> "更新了 Issue"
    IssueJourneyKind.Closed -> {
        val n = event.issueNumber
        if (n != null) "已在 #$n 中关闭了这个" else "关闭了这个"
    }
}

private fun formatJourneyTimestamp(millis: Long): String {
    val formatter = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
    return formatter.format(java.util.Date(millis))
}

private fun formatRepoLabel(repoPath: String?, owner: String, repo: String): String {
    val raw = repoPath?.takeIf { it.isNotBlank() } ?: "$owner/$repo"
    return raw.split('/').joinToString(" / ")
}

private fun parseLabelColor(raw: String?, dark: Boolean): Pair<Color, Color>? {
    if (raw.isNullOrBlank()) return null
    val hex = raw.trim().removePrefix("#")
    val rgb = when (hex.length) {
        6 -> hex.toLongOrNull(16)
        3 -> hex.map { "$it$it" }.joinToString("").toLongOrNull(16)
        else -> null
    } ?: return null
    val base = Color(0xFF000000L or rgb)
    val background = base.copy(alpha = if (dark) 0.28f else 0.14f)
    val foreground = if (dark) base.copy(alpha = 0.95f) else base
    return background to foreground
}

@Composable
private fun IssueAuthorRow(issue: IssueDetail, nowMillis: Long) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Avatar(
            name = issue.authorLabel,
            avatarUrl = issue.authorAvatarUrl,
            size = 36.dp,
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = issue.authorLogin ?: issue.authorLabel,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "发起了 Issue",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            IssueTimeText(
                raw = issue.createdAt,
                nowMillis = nowMillis,
                modifier = Modifier.testTag("issue-created-at"),
            )
            issue.authorName?.takeIf { it.isNotBlank() && it != issue.authorLogin }?.let { name ->
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CommentTimelineRow(
    comment: IssueComment,
    nowMillis: Long,
    showConnector: Boolean,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Avatar(
                name = comment.authorLabel,
                avatarUrl = comment.authorAvatarUrl,
                size = 36.dp,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = comment.authorLogin ?: comment.authorLabel,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    IssueTimeText(
                        raw = comment.createdAt,
                        nowMillis = nowMillis,
                        modifier = Modifier.testTag("comment-created-at"),
                    )
                }
                M3RaisedCard(Modifier.fillMaxWidth()) {
                    IssueMarkdownContent(
                        content = comment.body,
                        placeholder = "（无内容）",
                        compact = true,
                        modifier = Modifier.padding(M3CardPadding),
                    )
                }
            }
        }
        if (showConnector) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 46.dp, top = 14.dp),
                color = colors.outlineVariant.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
private fun CommentFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    ExtendedFloatingActionButton(
        onClick = onClick,
        modifier = modifier.testTag("issue-comment-fab"),
        containerColor = colors.primaryContainer,
        contentColor = colors.onPrimaryContainer,
        icon = {
            Icon(
                imageVector = Icons.Outlined.Create,
                contentDescription = null,
            )
        },
        text = { Text("评论") },
    )
}

@Composable
private fun CommentComposer(
    draft: String,
    posting: Boolean,
    error: String?,
    onDraftChange: (String) -> Unit,
    onPost: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val canSend = draft.isNotBlank() && !posting
    val fieldShape = RoundedCornerShape(22.dp)
    Column(Modifier.fillMaxWidth()) {
        error?.let {
            Text(
                text = it,
                color = colors.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .clip(fieldShape)
                .border(1.dp, colors.outline, fieldShape)
                .background(colors.surfaceVariant)
                .padding(start = 16.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            BasicTextField(
                value = draft,
                onValueChange = onDraftChange,
                enabled = !posting,
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 8.dp, bottom = 8.dp, end = 8.dp)
                    .testTag("issue-comment-input"),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
                cursorBrush = SolidColor(AccentBlue),
                maxLines = 8,
                decorationBox = { inner ->
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                        if (draft.isEmpty()) {
                            Text(
                                text = "写下评论",
                                style = MaterialTheme.typography.bodyLarge,
                                color = colors.onSurfaceVariant,
                            )
                        }
                        inner()
                    }
                },
            )
            SendCommentButton(enabled = canSend, posting = posting, onClick = onPost)
        }
    }
}

@Composable
private fun SendCommentButton(
    enabled: Boolean,
    posting: Boolean,
    onClick: () -> Unit,
) {
    val active = enabled || posting
    Box(
        modifier = Modifier
            .padding(bottom = 2.dp)
            .size(36.dp)
            .clip(CircleShape)
            .background(if (active) AccentBlue else Color.Transparent)
            .then(
                if (enabled) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            )
            .testTag("issue-comment-send"),
        contentAlignment = Alignment.Center,
    ) {
        if (posting) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = Color.White,
                strokeWidth = 2.dp,
            )
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "发送",
                tint = if (enabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun InlineMessage(message: String, action: String, onAction: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message, style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = onAction) { Text(action) }
    }
}

@Composable
private fun StatusChip(
    text: String,
    background: Color,
    foreground: Color,
    icon: ImageVector? = null,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = foreground,
                modifier = Modifier.size(14.dp),
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = foreground,
        )
    }
}

private fun IssueDetail.displayStateLabel(): String? {
    val raw = state?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return when (raw.lowercase(Locale.US)) {
        "open", "opened", "reopened" -> "打开"
        "closed" -> "已关闭"
        else -> raw
    }
}

private fun statusColors(raw: String, dark: Boolean): Pair<Color, Color> {
    val key = raw.trim().lowercase(Locale.US)
    return when {
        key.contains("close") || key == "closed" || key.contains("已完成") ->
            if (dark) Color(0xFF3D1215) to Color(0xFFFF7B72) else Color(0xFFFFEBE9) to Color(0xFFCF222E)
        key.contains("open") || key == "opened" || key.contains("待办") ->
            if (dark) Color(0xFF0D2818) to Color(0xFF3FB950) else Color(0xFFDCFFE4) to Color(0xFF1A7F37)
        else ->
            if (dark) AccentBlueContainerDark to AccentBlueBright else AccentBlueContainer to AccentBlue
    }
}

private fun issuePageUrl(htmlUrl: String?, owner: String, repo: String, number: Int): String {
    val given = htmlUrl?.trim().orEmpty()
    if (given.startsWith("https://") || given.startsWith("http://")) return given
    if (owner.isBlank() || repo.isBlank() || number <= 0) return ""
    return "https://atomgit.com/$owner/$repo/issues/$number"
}

private fun openIssuePage(
    uriHandler: androidx.compose.ui.platform.UriHandler,
    url: String,
    onFail: (String) -> Unit,
) {
    if (url.startsWith("https://") || url.startsWith("http://")) {
        runCatching { uriHandler.openUri(url) }.onFailure { onFail("无法打开网页") }
    } else {
        onFail("网页地址不可用")
    }
}

private fun ApiError.issueMessage(): String = when (this) {
    ApiError.NotFound -> "Issue 不存在或已删除"
    ApiError.Forbidden -> "没有权限查看这个 Issue"
    ApiError.Unauthorized -> "登录状态已失效"
    ApiError.Network -> "网络连接失败"
    ApiError.Parse -> "数据解析失败"
    is ApiError.RateLimited -> "请求过于频繁，请稍后重试"
    else -> "加载失败，请稍后重试"
}

private fun ApiError.commentsMessage(): String = when (this) {
    ApiError.Network -> "网络连接失败"
    ApiError.Unauthorized -> "登录状态已失效"
    else -> "评论加载失败"
}

private fun ApiError.commentPostMessage(): String = when (this) {
    ApiError.Forbidden -> "没有权限发表评论"
    ApiError.Unauthorized -> "登录状态已失效"
    ApiError.Network -> "网络连接失败"
    else -> "评论发送失败，请重试"
}