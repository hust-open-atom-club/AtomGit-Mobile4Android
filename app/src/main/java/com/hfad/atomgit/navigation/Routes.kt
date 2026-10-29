package com.hfad.atomgit.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * 路由 key
 */
@Serializable
sealed interface Route : NavKey

@Serializable
data object FeedRoute : Route

@Serializable
data object TodoRoute : Route

@Serializable
data object NotificationRoute : Route

@Serializable
data object ProfileRoute : Route

/** 共用详情路由 */
@Serializable
data class IssueDetailRoute(
    val owner: String,
    val repo: String,
    val number: Int,
) : Route

/**
 * 底部四个入口
 */
enum class TopLevelTab(val root: Route, val label: String) {
    Feed(FeedRoute, "动态"),
    Todo(TodoRoute, "待办"),
    Notification(NotificationRoute, "通知"),
    Profile(ProfileRoute, "我的"),
}
