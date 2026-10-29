package com.hfad.atomgit.ui.profile

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hfad.atomgit.ui.components.PlaceholderScreen

/**
 * 我的入口
 */
@Composable
fun ProfileScreen(
    listState: LazyListState,
    onOpenIssue: (owner: String, repo: String, number: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    PlaceholderScreen(
        title = "我的",
        testTag = "screen-profile",
        listState = listState,
        onOpenIssue = onOpenIssue,
        modifier = modifier,
    )
}
