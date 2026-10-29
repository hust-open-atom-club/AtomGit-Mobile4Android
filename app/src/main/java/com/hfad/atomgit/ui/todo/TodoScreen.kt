package com.hfad.atomgit.ui.todo

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hfad.atomgit.ui.components.PlaceholderScreen

/**
 * 待办入口
 */
@Composable
fun TodoScreen(
    listState: LazyListState,
    onOpenIssue: (owner: String, repo: String, number: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    PlaceholderScreen(
        title = "待办",
        testTag = "screen-todo",
        listState = listState,
        onOpenIssue = onOpenIssue,
        modifier = modifier,
    )
}
