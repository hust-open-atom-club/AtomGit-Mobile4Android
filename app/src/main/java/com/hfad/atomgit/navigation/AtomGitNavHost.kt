package com.hfad.atomgit.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.hfad.atomgit.ui.code.CodeViewerScreen
import com.hfad.atomgit.ui.code.CodeViewerSettingsScreen
import com.hfad.atomgit.ui.feed.FeedScreen
import com.hfad.atomgit.ui.follow.FollowListScreen
import com.hfad.atomgit.ui.issue.IssueDetailScreen
import com.hfad.atomgit.ui.notification.NotificationScreen
import com.hfad.atomgit.ui.profile.ProfileReposScreen
import com.hfad.atomgit.ui.profile.ProfileScreen
import com.hfad.atomgit.ui.pull.PullDetailScreen
import com.hfad.atomgit.ui.repo.RepoDetailScreen
import com.hfad.atomgit.ui.settings.SettingsScreen
import com.hfad.atomgit.ui.todo.TodoScreen
import com.hfad.atomgit.ui.user.UserProfileScreen

/**
 * 单 Activity 的主导航。
 */
@Composable
fun AtomGitNavHost(
    modifier: Modifier = Modifier,
    onExit: () -> Unit = {},
) {
    val navState = rememberTabNavState()
    val listStates = rememberTabListStates()

    Scaffold(
        modifier = modifier,
        bottomBar = {
            val current = navState.currentStack.lastOrNull()
            if (current !is RepoDetailRoute && current !is CodeViewerRoute) {
                AtomGitBottomBar(
                    selected = navState.selectedTab,
                    onSelect = navState::select,
                )
            }
        },
    ) { innerPadding ->
        NavDisplay(
            backStack = navState.currentStack,// 堆栈列表
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            onBack = { if (!navState.pop()) onExit() },
            entryDecorators = listOf(
                // 为每个 NavEntry 提供独立的 rememberSaveable 状态保存作用域
                rememberSaveableStateHolderNavEntryDecorator(),
                // 提供独立的 ViewModelStore 作用域
                rememberViewModelStoreNavEntryDecorator(),
            ),
            // 路由表
            entryProvider = remember(navState) {
                entryProvider<NavKey> {
                    entry<FeedRoute> {
                        FeedScreen(
                            listState = listStates.getValue(TopLevelTab.Feed),
                            onOpenIssue = { owner, repo, number -> navState.push(IssueDetailRoute(owner, repo, number)) },
                        )
                    }
                    entry<TodoRoute> {
                        TodoScreen(
                            listState = listStates.getValue(TopLevelTab.Todo),
                            onOpenIssue = { owner, repo, number ->
                                navState.push(IssueDetailRoute(owner, repo, number))
                            },
                            onOpenPull = { owner, repo, number ->
                                navState.push(PullDetailRoute(owner, repo, number))
                            },
                        )
                    }
                    entry<NotificationRoute> {
                        NotificationScreen(
                            listState = listStates.getValue(TopLevelTab.Notification),
                            onOpenIssue = { owner, repo, number -> navState.push(IssueDetailRoute(owner, repo, number)) },
                        )
                    }
                    entry<ProfileRoute> {
                        ProfileScreen(
                            listState = listStates.getValue(TopLevelTab.Profile),
                            onOpenRepos = { navState.push(ProfileReposRoute) },
                            onOpenFollow = { login, kind -> navState.push(FollowListRoute(login, kind)) },
                            onOpenSettings = { navState.push(SettingsRoute) },
                        )
                    }
                    entry<ProfileReposRoute> {
                        ProfileReposScreen(
                            onBack = { navState.pop() },
                            onOpenRepo = { owner, repo -> navState.push(RepoDetailRoute(owner, repo)) },
                        )
                    }
                    entry<RepoDetailRoute> { route ->
                        val uriHandler = LocalUriHandler.current
                        RepoDetailScreen(
                            owner = route.owner,
                            repo = route.repo,
                            onBack = { navState.pop() },
                            onOpenFile = { path, ref ->
                                navState.push(CodeViewerRoute(route.owner, route.repo, path, ref))
                            },
                            onOpenLink = { url ->
                                val link = parseAtomGitLink(url)
                                val sameRepo = link != null &&
                                    link.owner == route.owner && link.repo == route.repo
                                when {
                                    link is AtomGitLink.File ->
                                        navState.push(CodeViewerRoute(link.owner, link.repo, link.path, link.ref))
                                    link != null && !sameRepo ->
                                        navState.push(RepoDetailRoute(link.owner, link.repo))
                                    else -> runCatching { uriHandler.openUri(url) }
                                }
                            },
                        )
                    }
                    entry<FollowListRoute> { route ->
                        FollowListScreen(
                            login = route.login,
                            kind = route.kind,
                            onBack = { navState.pop() },
                            onOpenUser = { navState.push(UserProfileRoute(it)) },
                        )
                    }
                    entry<UserProfileRoute> { route ->
                        UserProfileScreen(
                            login = route.login,
                            onBack = { navState.pop() },
                            onOpenFollow = { login, kind ->
                                navState.push(FollowListRoute(login, kind))
                            },
                        )
                    }
                    entry<SettingsRoute> {
                        SettingsScreen(
                            onBack = { navState.pop() },
                            onOpenCodeSettings = { navState.push(CodeViewerSettingsRoute) },
                        )
                    }
                    entry<CodeViewerSettingsRoute> {
                        CodeViewerSettingsScreen(
                            onBack = { navState.pop() },
                            onOpenSample = {
                                navState.push(CodeViewerRoute(sample = true))
                            },
                        )
                    }
                    entry<CodeViewerRoute> { route ->
                        CodeViewerScreen(
                            owner = route.owner,
                            repo = route.repo,
                            path = route.path,
                            ref = route.ref,
                            sample = route.sample,
                            onBack = { navState.pop() },
                            onOpenSettings = { navState.push(CodeViewerSettingsRoute) },
                        )
                    }
                    entry<IssueDetailRoute> { route ->
                        IssueDetailScreen(
                            route = route,
                            onBack = { navState.pop() },
                        )
                    }
                    entry<PullDetailRoute> { route ->
                        PullDetailScreen(
                            route = route,
                            onBack = { navState.pop() },
                        )
                    }
                }
            },
        )
    }
}

/**
 * 底部导航栏
 */
@Composable
private fun AtomGitBottomBar(
    selected: TopLevelTab,
    onSelect: (TopLevelTab) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    NavigationBar(
        containerColor = colors.surface,
        contentColor = colors.onSurface,
    ) {
        TopLevelTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = tab == selected,
                onClick = { onSelect(tab) },
                icon = {
                    Icon(
                        imageVector = tab.icon(),
                        contentDescription = null,
                    )
                },
                label = { Text(text = tab.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colors.onSurface,
                    selectedTextColor = colors.onSurface,
                    indicatorColor = colors.surfaceVariant,
                    unselectedIconColor = colors.onSurfaceVariant,
                    unselectedTextColor = colors.onSurfaceVariant,
                ),
                modifier = Modifier.testTag("tab-${tab.name}"),
            )
        }
    }
}

/**
 * 入口图标
 */
private fun TopLevelTab.icon(): ImageVector = when (this) {
    TopLevelTab.Feed -> Icons.Outlined.Home
    TopLevelTab.Todo -> Icons.Outlined.CheckCircle
    TopLevelTab.Notification -> Icons.Outlined.Notifications
    TopLevelTab.Profile -> Icons.Outlined.Person
}

/**
 * 每个入口列表滚动位置
 */
@Composable
private fun rememberTabListStates(): Map<TopLevelTab, LazyListState> {
    val feed = rememberLazyListState()
    val todo = rememberLazyListState()
    val notifications = rememberLazyListState()
    val profile = rememberLazyListState()
    return remember(feed, todo, notifications, profile) {
        mapOf(
            TopLevelTab.Feed to feed,
            TopLevelTab.Todo to todo,
            TopLevelTab.Notification to notifications,
            TopLevelTab.Profile to profile,
        )
    }
}
