package com.hfad.atomgit.data.model

/** 覆盖系统字体时可选的字号档位（sp）。 */
val CodeFontSizeSteps: List<Int> = listOf(10, 12, 13, 14, 16, 18, 20)

const val DefaultCodeFontSizeSp: Int = 13

/** 代码查看页显示偏好。 */
data class CodeViewerPreferences(
    val showLineNumbers: Boolean = true,
    /** 开启后代码区固定深色；关闭则跟随 App 浅/深色主题。 */
    val alwaysDarkTheme: Boolean = false,
    val overrideSystemFontSize: Boolean = false,
    /** 仅在 [overrideSystemFontSize] 为 true 时生效。 */
    val fontSizeSp: Int = DefaultCodeFontSizeSp,
    val wordWrap: Boolean = false,
) {
    val resolvedFontSizeSp: Int
        get() = fontSizeSp.coerceIn(CodeFontSizeSteps.first(), CodeFontSizeSteps.last())
}
