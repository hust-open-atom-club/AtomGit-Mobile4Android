package com.hfad.atomgit.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 列表底部翻页哨兵，进入可见区域时拉下一页；
 * 内容不足一屏时不触发，否则刷新完会多请求一次
 */
@Composable
fun LoadMoreFooter(
    itemCount: Int,
    listState: LazyListState,
    isLoadingMore: Boolean,
    onLoadMore: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        LaunchedEffect(itemCount) {
            if (listState.firstVisibleItemIndex > 0 || listState.canScrollForward) {
                onLoadMore()
            }
        }
        if (isLoadingMore) {
            Text(
                text = "加载中…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}