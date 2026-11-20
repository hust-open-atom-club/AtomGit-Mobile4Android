package com.hfad.atomgit.ui.components

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.hfad.atomgit.ui.theme.Ink
import com.hfad.atomgit.ui.theme.Paper

/** 下拉刷新指示器：白底黑字，避免主题 primary 蓝色/默认紫色。 */
@Composable
fun BoxScope.AtomPullRefreshIndicator(
    state: PullToRefreshState,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier,
) {
    PullToRefreshDefaults.Indicator(
        state = state,
        isRefreshing = isRefreshing,
        modifier = modifier.align(Alignment.TopCenter),
        containerColor = Paper,
        color = Ink,
    )
}
