package com.hfad.atomgit.ui.feed

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hfad.atomgit.ui.components.PlaceholderScreen

/**
 * 动态入口
 */
@Composable
fun FeedScreen(
    listState: LazyListState,
    onOpenIssue: (owner: String, repo: String, number: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    PlaceholderScreen(
        title = "动态",
        testTag = "screen-feed",
        listState = listState,
        onOpenIssue = onOpenIssue,
        modifier = modifier,
    )
}
