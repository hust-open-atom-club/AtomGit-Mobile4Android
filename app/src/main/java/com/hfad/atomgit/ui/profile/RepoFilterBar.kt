package com.hfad.atomgit.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hfad.atomgit.data.model.RepoSort
import com.hfad.atomgit.data.model.RepoVisibility
import com.hfad.atomgit.ui.theme.SheetCardDark
import com.hfad.atomgit.ui.theme.SheetDark
import com.hfad.atomgit.ui.theme.SheetMutedDark

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
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val cardColor = if (dark) SheetCardDark else MaterialTheme.colorScheme.surface
    val gapColor = if (dark) SheetDark else MaterialTheme.colorScheme.surfaceVariant
    val mutedColor = if (dark) SheetMutedDark else MaterialTheme.colorScheme.onSurfaceVariant

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = gapColor,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
        shape = RectangleShape,
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cardColor)
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 48.dp, height = 4.dp)
                        .background(mutedColor, CircleShape),
                )
            }
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardColor)
                .padding(start = 4.dp, end = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "关闭",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "排序方式",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            listOf(
                listOf(RepoSort.PushedDesc, RepoSort.PushedAsc),
                listOf(RepoSort.CreatedDesc, RepoSort.CreatedAsc),
                listOf(RepoSort.NameAsc, RepoSort.NameDesc),
            ).forEach { options ->
                SortCard(
                    options = options,
                    selected = selected,
                    onSelect = onSelect,
                    color = cardColor,
                    unselectedColor = mutedColor,
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SortCard(
    options: List<RepoSort>,
    selected: RepoSort,
    onSelect: (RepoSort) -> Unit,
    color: Color,
    unselectedColor: Color,
) {
    Surface(
        color = color,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            options.forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = option == selected,
                            role = Role.RadioButton,
                            onClick = { onSelect(option) },
                        )
                        .padding(start = 16.dp, end = 20.dp, top = 18.dp, bottom = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = option.label(),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    RadioButton(
                        selected = option == selected,
                        onClick = null,
                        colors = RadioButtonDefaults.colors(
                            selectedColor = MaterialTheme.colorScheme.primary,
                            unselectedColor = unselectedColor,
                        ),
                    )
                }
            }
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
