package com.hfad.atomgit.ui.code

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

/** 轻量语法着色用的配色。 */
data class SyntaxColors(
    val plain: Color,
    val keyword: Color,
    val string: Color,
    val comment: Color,
    val number: Color,
    val parameter: Color,
)

fun syntaxColorsFor(dark: Boolean): SyntaxColors = if (dark) {
    SyntaxColors(
        plain = Color(0xFFE8E8E8),
        keyword = Color(0xFFCC7832),
        string = Color(0xFF6A8759),
        comment = Color(0xFF808080),
        number = Color(0xFF6897BB),
        parameter = Color(0xFF9876AA),
    )
} else {
    SyntaxColors(
        plain = Color(0xFF111111),
        keyword = Color(0xFF0033B3),
        string = Color(0xFF067D17),
        comment = Color(0xFF8C8C8C),
        number = Color(0xFF1750EB),
        parameter = Color(0xFF871094),
    )
}

enum class CodeLanguage {
    Kotlin,
    Java,
    Generic,
}

fun codeLanguageFromPath(path: String): CodeLanguage {
    val ext = path.substringAfterLast('.', missingDelimiterValue = "").lowercase()
    return when (ext) {
        "kt", "kts" -> CodeLanguage.Kotlin
        "java" -> CodeLanguage.Java
        else -> CodeLanguage.Generic
    }
}

private val KOTLIN_KEYWORDS = setOf(
    "abstract", "actual", "annotation", "as", "break", "by", "catch", "class", "companion",
    "const", "constructor", "continue", "crossinline", "data", "delegate", "do", "dynamic",
    "else", "enum", "expect", "external", "false", "field", "file", "final", "finally", "for",
    "fun", "get", "if", "import", "in", "infix", "init", "inline", "inner", "interface",
    "internal", "is", "lateinit", "noinline", "null", "object", "open", "operator", "out",
    "override", "package", "param", "private", "property", "protected", "public", "reified",
    "return", "sealed", "set", "super", "suspend", "tailrec", "this", "throw", "true", "try",
    "typealias", "typeof", "val", "var", "vararg", "when", "where", "while",
)

private val JAVA_KEYWORDS = setOf(
    "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
    "const", "continue", "default", "do", "double", "else", "enum", "extends", "final",
    "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int",
    "interface", "long", "native", "new", "package", "private", "protected", "public",
    "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this",
    "throw", "throws", "transient", "try", "void", "volatile", "while", "true", "false", "null",
)

private val GENERIC_KEYWORDS = KOTLIN_KEYWORDS + JAVA_KEYWORDS + setOf(
    "def", "elif", "except", "from", "lambda", "pass", "raise", "with", "yield",
    "fn", "let", "mut", "impl", "trait", "struct", "match", "use", "mod", "pub", "crate",
)

/**
 * 将源码着色为 [AnnotatedString]。
 * 规则顺序：注释 / 字符串 → 数字 → 关键字 → 函数形参名。
 */
fun highlightCode(
    code: String,
    language: CodeLanguage,
    colors: SyntaxColors,
): AnnotatedString {
    if (code.isEmpty()) return AnnotatedString("")
    val styles = Array<SpanStyle?>(code.length) { null }
    val keywords = when (language) {
        CodeLanguage.Kotlin -> KOTLIN_KEYWORDS
        CodeLanguage.Java -> JAVA_KEYWORDS
        CodeLanguage.Generic -> GENERIC_KEYWORDS
    }

    applyRegex(code, COMMENT_OR_STRING) { start, end, text ->
        val style = if (text.startsWith("//") || text.startsWith("/*") || text.startsWith("#")) {
            SpanStyle(color = colors.comment)
        } else {
            SpanStyle(color = colors.string)
        }
        fillStyle(styles, start, end, style)
    }

    applyRegex(code, NUMBER) { start, end, _ ->
        if (styles[start] == null) {
            fillStyle(styles, start, end, SpanStyle(color = colors.number))
        }
    }

    applyRegex(code, IDENTIFIER) { start, end, text ->
        if (styles[start] != null) return@applyRegex
        if (text in keywords) {
            fillStyle(styles, start, end, SpanStyle(color = colors.keyword))
        }
    }

    applyRegex(code, PARAMETER) { start, end, _ ->
        // group 1 is the parameter name; match includes leading comma/paren so re-find name
        val nameMatch = IDENTIFIER.find(code, start) ?: return@applyRegex
        if (nameMatch.range.first >= end) return@applyRegex
        val nameStart = nameMatch.range.first
        val nameEnd = nameMatch.range.last + 1
        if (nameEnd <= styles.size && styles[nameStart] == null) {
            fillStyle(styles, nameStart, nameEnd, SpanStyle(color = colors.parameter))
        }
    }

    return buildAnnotatedString {
        var index = 0
        while (index < code.length) {
            val style = styles[index]
            var end = index + 1
            while (end < code.length && styles[end] == style) end++
            if (style != null) {
                withStyle(style) { append(code.substring(index, end)) }
            } else {
                withStyle(SpanStyle(color = colors.plain)) { append(code.substring(index, end)) }
            }
            index = end
        }
    }
}

private val COMMENT_OR_STRING = Regex(
    """//[^\n]*|/\*[\s\S]*?\*/|"(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*'|`(?:\\.|[^`\\])*`""",
)

private val NUMBER = Regex("""\b\d+(?:\.\d+)?(?:[eE][+-]?\d+)?[fFdDlL]?\b""")

private val IDENTIFIER = Regex("""\b[A-Za-z_][A-Za-z0-9_]*\b""")

/** 形参：`(name:` 或 `, name:` */
private val PARAMETER = Regex("""[(,]\s*([A-Za-z_][A-Za-z0-9_]*)\s*:""")

private inline fun applyRegex(
    code: String,
    regex: Regex,
    action: (start: Int, end: Int, text: String) -> Unit,
) {
    regex.findAll(code).forEach { match ->
        action(match.range.first, match.range.last + 1, match.value)
    }
}

private fun fillStyle(styles: Array<SpanStyle?>, start: Int, end: Int, style: SpanStyle) {
    val safeEnd = end.coerceAtMost(styles.size)
    for (i in start until safeEnd) {
        if (styles[i] == null) styles[i] = style
    }
}
