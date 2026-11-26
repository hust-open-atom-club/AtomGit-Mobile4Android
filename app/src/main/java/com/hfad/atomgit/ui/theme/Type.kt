package com.hfad.atomgit.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.unit.sp

/** M3 默认 Type scale；仅微调正文行高以适配列表与 Markdown。 */
val Typography = Typography().let { base ->
    base.copy(
        bodyLarge = base.bodyLarge.copy(
            lineHeight = 24.sp,
            letterSpacing = 0.25.sp,
        ),
        bodyMedium = base.bodyMedium.copy(
            lineHeight = 20.sp,
        ),
    )
}