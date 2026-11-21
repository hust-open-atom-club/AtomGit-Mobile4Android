package com.hfad.atomgit.ui.repo

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.mikepenz.markdown.coil3.Coil3ImageTransformerImpl
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownTypography

/**
 * README 预览，按 Markdown 渲染；图片 Coil3
 */
@Composable
internal fun ReadmeMarkdown(markdown: String, modifier: Modifier = Modifier) {
    val base = MaterialTheme.typography
    val linkStyle = SpanStyle(
        color = MaterialTheme.colorScheme.primary,
        textDecoration = TextDecoration.Underline,
    )
    val typography = markdownTypography(
        h1 = base.headlineSmall.copy(fontWeight = FontWeight.Bold),
        h2 = base.titleLarge.copy(fontWeight = FontWeight.Bold),
        h3 = base.titleMedium.copy(fontWeight = FontWeight.Bold),
        h4 = base.titleSmall.copy(fontWeight = FontWeight.Bold),
        h5 = base.bodyLarge.copy(fontWeight = FontWeight.Bold),
        h6 = base.bodyMedium.copy(fontWeight = FontWeight.Bold),
        text = base.bodyMedium,
        paragraph = base.bodyMedium,
        list = base.bodyMedium,
        ordered = base.bodyMedium,
        bullet = base.bodyMedium,
        quote = base.bodyMedium,
        code = base.bodySmall.copy(fontFamily = FontFamily.Monospace),
        inlineCode = base.bodyMedium.copy(fontFamily = FontFamily.Monospace),
        textLink = TextLinkStyles(style = linkStyle),
    )
    Markdown(
        content = markdown,
        modifier = modifier.fillMaxWidth(),
        typography = typography,
        imageTransformer = Coil3ImageTransformerImpl,
    )
}
