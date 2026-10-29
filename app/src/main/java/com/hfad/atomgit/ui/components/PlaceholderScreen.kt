package com.hfad.atomgit.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * 导航骨架用的占位入口页
 */
@Composable
fun PlaceholderScreen(
    title: String,
    testTag: String,
    listState: LazyListState,
    onOpenIssue: (owner: String, repo: String, number: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag(testTag),
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
        HorizontalDivider()
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            items(PLACEHOLDER_ITEM_COUNT) { index ->
                ListItem(
                    headlineContent = { Text("$title 占位条目 ${index + 1}") },
                    supportingContent = { Text("仅用于导航验证，未接接口") },
                    trailingContent = if (index == 0) {
                        {
                            TextButton(onClick = { onOpenIssue(PLACEHOLDER_OWNER, PLACEHOLDER_REPO, 1) }) {
                                Text("打开 Issue 详情")
                            }
                        }
                    } else {
                        null
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                HorizontalDivider()
            }
        }
    }
}

private const val PLACEHOLDER_ITEM_COUNT = 30
private const val PLACEHOLDER_OWNER = "hfad"
private const val PLACEHOLDER_REPO = "atomgit-mobile"
