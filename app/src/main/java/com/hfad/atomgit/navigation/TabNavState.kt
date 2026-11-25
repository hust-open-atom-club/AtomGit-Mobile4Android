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
 * 主导航动画意图。底部 Tab 左右切换用平移翻页；栈内 push 仍用右侧滑入。
 */
enum class NavMotion {
    /** 点到更靠右的 Tab：新页从右滑入。 */
    TabToRight,
    /** 点到更靠左的 Tab：新页从左滑入。 */
    TabToLeft,
    /** 当前栈压入详情。 */
    Push,
}

/**
 * 四个顶层入口的返回栈与当前选中入口。
 *
 * 每个入口单独持有一个 [NavBackStack]，切换 Tab 只换当前渲染的对应栈。
 */
@Stable
class TabNavState internal constructor(
    private val selectedTabState: MutableState<TopLevelTab>,
    private val stacks: Map<TopLevelTab, NavBackStack<NavKey>>,
    private val motionState: MutableState<NavMotion> = mutableStateOf(NavMotion.Push),
) {

    val selectedTab: TopLevelTab
        get() = selectedTabState.value

    val currentStack: NavBackStack<NavKey>
        get() = stacks.getValue(selectedTabState.value)

    /** 最近一次导航要用的过渡方向，供 [androidx.navigation3.ui.NavDisplay] 读取。 */
    val motion: NavMotion
        get() = motionState.value

    fun select(tab: TopLevelTab) {
        val current = selectedTabState.value
        if (tab == current) return
        motionState.value = if (tab.ordinal > current.ordinal) {
            NavMotion.TabToRight
        } else {
            NavMotion.TabToLeft
        }
        selectedTabState.value = tab
    }

    fun push(route: Route) {
        motionState.value = NavMotion.Push
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
