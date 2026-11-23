package com.hfad.atomgit.ui.code

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.BoldHighlight
import dev.snipme.highlights.model.ColorHighlight
import dev.snipme.highlights.model.SyntaxLanguage
import dev.snipme.highlights.model.SyntaxThemes

/** 与 README 代码块同一套高亮：整段一次着色，再由 [splitByLines] 切成行 */
fun highlightCodeText(code: String, language: CodeLanguage, dark: Boolean): AnnotatedString {
    if (code.isEmpty()) return AnnotatedString("")
    val syntaxLanguage = when (language) {
        CodeLanguage.Kotlin -> SyntaxLanguage.KOTLIN
        CodeLanguage.Java -> SyntaxLanguage.JAVA
        CodeLanguage.Generic -> null
    }
    val builder = Highlights.Builder().theme(SyntaxThemes.atom(darkMode = dark))
    builder.code(code)
    if (syntaxLanguage != null) builder.language(syntaxLanguage)
    val highlights = builder.build().getHighlights()
    return buildAnnotatedString {
        append(code)
        highlights.forEach { highlight ->
            val style = when (highlight) {
                is ColorHighlight -> SpanStyle(color = Color(highlight.rgb).copy(alpha = 1f))
                is BoldHighlight -> SpanStyle(fontWeight = FontWeight.Bold)
            }
            addStyle(style = style, start = highlight.location.start, end = highlight.location.end)
        }
    }
}

/** 按 `\n` 切成行，保留每行原有的样式区间 */
fun AnnotatedString.splitByLines(): List<AnnotatedString> {
    val result = ArrayList<AnnotatedString>()
    var start = 0
    var index = 0
    val text = this.text
    while (index <= text.length) {
        if (index == text.length || text[index] == '\n') {
            result.add(subSequence(start, index))
            start = index + 1
        }
        index++
    }
    return result
}
