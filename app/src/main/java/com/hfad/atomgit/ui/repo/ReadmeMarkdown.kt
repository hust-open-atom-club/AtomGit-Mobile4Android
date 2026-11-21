package com.hfad.atomgit.ui.repo

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
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
 * 链接统一交给 [onLink]，让调用方决定 App 内跳转还是开浏览器
 */
@Composable
internal fun ReadmeMarkdown(
    markdown: String,
    onLink: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val handler = remember(onLink) {
        object : UriHandler {
            override fun openUri(uri: String) = onLink(uri)
        }
    }
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
    CompositionLocalProvider(LocalUriHandler provides handler) {
        Markdown(
            content = markdown,
            modifier = modifier.fillMaxWidth(),
            typography = typography,
            imageTransformer = Coil3ImageTransformerImpl,
        )
    }
}
