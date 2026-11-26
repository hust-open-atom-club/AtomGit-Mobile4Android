package com.hfad.atomgit.ui.pull

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.hfad.atomgit.navigation.PullDetailRoute

/**
 * 共用的 PR 详情占位页
 */
@Composable
fun PullDetailScreen(
    route: PullDetailRoute,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("screen-pull-detail")
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = "PR 详情", style = MaterialTheme.typography.titleLarge)
        Text(text = "${route.owner}/${route.repo}#${route.number}")
        Text(text = "占位页，未加载 diff 与评论")
        TextButton(onClick = onBack) {
            Text("返回")
        }
    }
}
