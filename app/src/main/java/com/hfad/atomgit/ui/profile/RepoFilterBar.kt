package com.hfad.atomgit.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hfad.atomgit.data.model.RepoSort
import com.hfad.atomgit.data.model.RepoVisibility

@Composable
internal fun FilterPill(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 8.dp, top = 7.dp, bottom = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SortSheet(
    selected: RepoSort,
    onSelect: (RepoSort) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        Text(
            text = "排序方式",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        )
        SortSection(
            title = "推送时间",
            options = listOf(RepoSort.PushedDesc, RepoSort.PushedAsc),
            selected = selected,
            onSelect = onSelect,
        )
        SortSection(
            title = "更新时间",
            options = listOf(RepoSort.UpdatedDesc, RepoSort.UpdatedAsc),
            selected = selected,
            onSelect = onSelect,
        )
        SortSection(
            title = "创建时间",
            options = listOf(RepoSort.CreatedDesc, RepoSort.CreatedAsc),
            selected = selected,
            onSelect = onSelect,
        )
        SortSection(
            title = "名称",
            options = listOf(RepoSort.NameAsc, RepoSort.NameDesc),
            selected = selected,
            onSelect = onSelect,
        )
        Box(modifier = Modifier.padding(bottom = 24.dp))
    }
}

@Composable
private fun SortSection(
    title: String,
    options: List<RepoSort>,
    selected: RepoSort,
    onSelect: (RepoSort) -> Unit,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
    )
    options.forEach { option ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelect(option) }
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = option.label(),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            RadioButton(selected = option == selected, onClick = null)
        }
    }
}

internal fun RepoVisibility.label(): String = when (this) {
    RepoVisibility.All -> "全部"
    RepoVisibility.Public -> "公开"
    RepoVisibility.Private -> "私有"
}

internal fun RepoSort.label(): String = when (this) {
    RepoSort.PushedDesc -> "最近推送"
    RepoSort.PushedAsc -> "最久未推送"
    RepoSort.UpdatedDesc -> "最近更新"
    RepoSort.UpdatedAsc -> "最早更新"
    RepoSort.CreatedDesc -> "最新创建"
    RepoSort.CreatedAsc -> "最早创建"
    RepoSort.NameAsc -> "名称升序"
    RepoSort.NameDesc -> "名称降序"
}