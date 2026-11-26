package com.hfad.atomgit.ui.components

import androidx.compose.ui.unit.dp

/**
 * 收件箱 / 待办类列表页的共用间距与触控尺寸，便于多 Tab 视觉一致。
 */
object InboxLayout {
    val screenHorizontal = 16.dp
    /** 两 Tab 顶栏固定高度，保证「待办 / 通知」标题位置一致。 */
    val titleBarHeight = 56.dp
    val filterBarVertical = 8.dp
    val listHorizontal = 16.dp
    val listTop = 8.dp
    val listBottom = 12.dp
    val listItemGap = 8.dp
    val rowHorizontal = 16.dp
    val rowVertical = 12.dp
    val rowMinHeight = 56.dp
    val leadingIcon = 20.dp
    val filterChipHorizontal = 12.dp
    val filterChipVertical = 6.dp
}