package com.hfad.atomgit.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack

/**
 * 四个顶层入口的返回栈与当前选中入口。
 *
 * 每个入口单独持有一个 [NavBackStack]，切换 Tab 只换当前渲染的对应栈。
 */
@Stable
class TabNavState internal constructor(
    private val selectedTabState: MutableState<TopLevelTab>,
    private val stacks: Map<TopLevelTab, NavBackStack<NavKey>>,
) {

    val selectedTab: TopLevelTab
        get() = selectedTabState.value

    val currentStack: NavBackStack<NavKey>
        get() = stacks.getValue(selectedTabState.value)

    fun select(tab: TopLevelTab) {
        selectedTabState.value = tab
    }

    fun push(route: Route) {
        currentStack.add(route)
    }

    fun pop(): Boolean {
        if (currentStack.size <= 1) return false
        currentStack.removeAt(currentStack.lastIndex)
        return true
    }
}

/**
 * 建立四条返回栈
 */
@Composable
fun rememberTabNavState(initialTab: TopLevelTab = TopLevelTab.Feed): TabNavState {
    val selectedTab = rememberSaveable { mutableStateOf(initialTab) }
    val feed = rememberNavBackStack(FeedRoute)
    val todo = rememberNavBackStack(TodoRoute)
    val notifications = rememberNavBackStack(NotificationRoute)
    val profile = rememberNavBackStack(ProfileRoute)
    val stacks = remember(feed, todo, notifications, profile) {
        mapOf(
            TopLevelTab.Feed to feed,
            TopLevelTab.Todo to todo,
            TopLevelTab.Notification to notifications,
            TopLevelTab.Profile to profile,
        )
    }
    return remember(selectedTab, stacks) { TabNavState(selectedTab, stacks) }
}
