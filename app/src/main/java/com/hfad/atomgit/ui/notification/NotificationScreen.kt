package com.hfad.atomgit.ui.notification

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hfad.atomgit.ui.components.PlaceholderScreen

/**
 * 通知入口
 */
@Composable
fun NotificationScreen(
    listState: LazyListState,
    onOpenIssue: (owner: String, repo: String, number: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    PlaceholderScreen(
        title = "通知",
        testTag = "screen-notification",
        listState = listState,
        onOpenIssue = onOpenIssue,
        modifier = modifier,
    )
}
