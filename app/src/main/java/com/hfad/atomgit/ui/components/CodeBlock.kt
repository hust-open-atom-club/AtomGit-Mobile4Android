package com.hfad.atomgit.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hfad.atomgit.data.model.CodeViewerPreferences
import com.hfad.atomgit.ui.code.CodeLanguage
import com.hfad.atomgit.ui.code.codeLanguageFromPath
import com.hfad.atomgit.ui.code.highlightCode
import com.hfad.atomgit.ui.code.syntaxColorsFor
import com.hfad.atomgit.ui.theme.AccentBlue
import com.hfad.atomgit.ui.theme.AccentBlueDark
import com.hfad.atomgit.ui.theme.Ink
import com.hfad.atomgit.ui.theme.Paper
import com.hfad.atomgit.ui.theme.PaperDark
import com.hfad.atomgit.ui.theme.PaperDim

/**
 * 共用代码块：按逻辑行布局。
 * - 行号列整列连续灰底；换行时该行灰底随内容向下拉高，号码贴顶
 * - 不换行时共用横向滚动（各行内容区统一为最长行宽度）
 * - 点击仅短暂 ripple，不保留选中态
 */
@Composable
fun CodeBlock(
    code: String,
    preferences: CodeViewerPreferences,
    modifier: Modifier = Modifier,
    path: String = "",
    language: CodeLanguage = codeLanguageFromPath(path),
) {
    val dark = preferences.alwaysDarkTheme
    val background = if (dark) PaperDark else Paper
    val lineNumberBackground = PaperDim
    val lineNumberColor = Ink
    val colors = remember(dark) { syntaxColorsFor(dark) }
    val lines = remember(code) {
        if (code.isEmpty()) listOf("") else code.split('\n')
    }
    val lineCount = lines.size
    val gutterWidth = remember(lineCount) {
        val digits = lineCount.toString().length.coerceAtLeast(2)
        (digits * 10 + 16).dp
    }
    val fontSizeSp = if (preferences.overrideSystemFontSize) {
        preferences.resolvedFontSizeSp
    } else {
        13
    }
    val fontSize = fontSizeSp.sp
    val lineHeight = (fontSizeSp * 1.55f).sp
    val codeStyle = remember(fontSize, lineHeight) {
        TextStyle(
            fontSize = fontSize,
            lineHeight = lineHeight,
            fontFamily = FontFamily.Monospace,
        )
    }
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val unifiedContentWidth = remember(lines, codeStyle, preferences.wordWrap, fontSizeSp) {
        if (preferences.wordWrap) {
            0.dp
        } else {
            val maxPx = lines.maxOf { line ->
                textMeasurer.measure(
                    text = line,
                    style = codeStyle,
                    softWrap = false,
                    maxLines = 1,
                ).size.width
            }
            with(density) { maxPx.toDp() } + 24.dp
        }
    }
    val horizontalScroll = rememberScrollState()

    val content: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(background),
        ) {
            if (preferences.showLineNumbers) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .width(gutterWidth)
                        .fillMaxHeight()
                        .background(lineNumberBackground),
                )
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(
                    items = lines,
                    key = { index, _ -> index },
                ) { index, line ->
                    val highlighted = remember(line, language, colors) {
                        highlightCode(line, language, colors)
                    }
                    CodeLineRow(
                        lineNumber = index + 1,
                        highlighted = highlighted,
                        showLineNumbers = preferences.showLineNumbers,
                        wordWrap = preferences.wordWrap,
                        gutterWidth = gutterWidth,
                        unifiedContentWidth = unifiedContentWidth,
                        lineNumberColor = lineNumberColor,
                        fontSize = fontSize,
                        lineHeight = lineHeight,
                        horizontalScroll = horizontalScroll,
                        darkBackground = dark,
                    )
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(background),
    ) {
        if (preferences.overrideSystemFontSize) {
            val localDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = localDensity.density,
                    fontScale = 1f,
                ),
            ) {
                content()
            }
        } else {
            content()
        }
    }
}

@Composable
private fun CodeLineRow(
    lineNumber: Int,
    highlighted: AnnotatedString,
    showLineNumbers: Boolean,
    wordWrap: Boolean,
    gutterWidth: Dp,
    unifiedContentWidth: Dp,
    lineNumberColor: Color,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    horizontalScroll: ScrollState,
    darkBackground: Boolean,
) {
    val interaction = remember { MutableInteractionSource() }
    // 深色代码区用亮蓝 ripple，灰色在深底上看不清
    val pressRipple = ripple(
        color = if (darkBackground) AccentBlueDark else AccentBlue,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.Top,
    ) {
        if (showLineNumbers) {
            Box(
                modifier = Modifier
                    .width(gutterWidth)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = interaction,
                        indication = pressRipple,
                        onClick = {},
                    ),
            ) {
                Text(
                    text = lineNumber.toString(),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    color = lineNumberColor,
                    fontSize = fontSize,
                    lineHeight = lineHeight,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.End,
                    softWrap = false,
                    maxLines = 1,
                )
            }
        }

        if (wordWrap) {
            Text(
                text = highlighted,
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = pressRipple,
                        onClick = {},
                    )
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                fontSize = fontSize,
                lineHeight = lineHeight,
                fontFamily = FontFamily.Monospace,
                softWrap = true,
            )
        } else {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(horizontalScroll),
            ) {
                Text(
                    text = highlighted,
                    modifier = Modifier
                        .width(unifiedContentWidth)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = pressRipple,
                            onClick = {},
                        )
                        .padding(horizontal = 12.dp, vertical = 2.dp),
                    fontSize = fontSize,
                    lineHeight = lineHeight,
                    fontFamily = FontFamily.Monospace,
                    softWrap = false,
                    maxLines = 1,
                    overflow = TextOverflow.Visible,
                )
            }
        }
    }
}

/** 设置页预览用的短示例。 */
val CODE_PREVIEW_SAMPLE = """
fun greet(name: String, times: Int = 1): String {
    // say hello — long enough to scroll when wrap is off: val path = "/very/long/path/to/src/main/kotlin/com/example/Sample.kt"
    return "Hello, ${'$'}name!".repeat(times)
}
""".trimIndent()

/** 完整查看页示例路径与正文。 */
const val CODE_VIEWER_SAMPLE_PATH = "src/main/kotlin/Sample.kt"

val CODE_VIEWER_SAMPLE_CONTENT = """
package com.example

/**
 * Sample file for the code viewer.
 */
data class User(val id: Long, val name: String)

fun loadUser(id: Long, force: Boolean = false): User? {
    if (force) return User(id, "guest")
    // TODO: fetch from network — /users/{id}/profile?include=repos,orgs,followers&pretty=true
    return null
}
""".trimIndent()
