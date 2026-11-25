package com.hfad.atomgit.ui.issue

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.mikepenz.markdown.coil3.Coil3ImageTransformerImpl
import com.mikepenz.markdown.compose.LocalImageWidth
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.MarkdownHighlightedCodeBlock
import com.mikepenz.markdown.compose.elements.MarkdownHighlightedCodeFence
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.ImageWidth
import com.mikepenz.markdown.model.State
import com.mikepenz.markdown.model.markdownAnimations
import com.mikepenz.markdown.model.rememberMarkdownState
import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.SyntaxThemes

/**
 * Issue 描述与评论正文：Markdown 渲染；空串显示占位文案。
 */
@Composable
fun IssueMarkdownContent(
    content: String?,
    modifier: Modifier = Modifier,
    placeholder: String = "（无内容）",
    compact: Boolean = false,
) {
    val raw = content?.trim().orEmpty()
    val colors = MaterialTheme.colorScheme
    if (raw.isEmpty()) {
        Text(
            text = placeholder,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurfaceVariant,
            modifier = modifier,
        )
        return
    }
    val markdownHolder = rememberMarkdownState(raw, immediate = true)
    val parseState by markdownHolder.state.collectAsState()
    when (val state = parseState) {
        is State.Success -> IssueMarkdownDocument(state, modifier, compact = compact)
        is State.Loading, is State.Error -> Text(
            text = raw,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurface,
            modifier = modifier,
        )
    }
}

@Composable
private fun IssueMarkdownDocument(
    state: State.Success,
    modifier: Modifier,
    compact: Boolean,
) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val highlightsBuilder = remember(dark) {
        Highlights.Builder().theme(SyntaxThemes.atom(darkMode = dark))
    }
    val base = MaterialTheme.typography
    val bodyStyle = if (compact) base.bodyMedium else base.bodyLarge
    val linkStyle = SpanStyle(
        color = MaterialTheme.colorScheme.primary,
        textDecoration = TextDecoration.Underline,
    )
    val typography = markdownTypography(
        h1 = base.titleLarge.copy(fontWeight = FontWeight.Bold),
        h2 = base.titleMedium.copy(fontWeight = FontWeight.Bold),
        h3 = base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        h4 = base.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
        h5 = base.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
        h6 = base.bodyMedium.copy(fontWeight = FontWeight.Medium),
        text = bodyStyle,
        paragraph = bodyStyle,
        list = bodyStyle,
        ordered = bodyStyle,
        bullet = bodyStyle,
        quote = bodyStyle.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
        code = base.bodySmall.copy(fontFamily = FontFamily.Monospace),
        inlineCode = bodyStyle.copy(fontFamily = FontFamily.Monospace),
        textLink = TextLinkStyles(style = linkStyle),
    )
    CompositionLocalProvider(
        LocalUriHandler provides LocalUriHandler.current,
        LocalImageWidth provides ImageWidth.MAX_WIDTH,
    ) {
        Markdown(
            state = state,
            modifier = modifier.fillMaxWidth(),
            typography = typography,
            imageTransformer = Coil3ImageTransformerImpl,
            animations = markdownAnimations(animateTextSize = { this }),
            components = markdownComponents(
                codeBlock = {
                    MarkdownHighlightedCodeBlock(
                        content = it.content,
                        node = it.node,
                        style = it.typography.code,
                        highlightsBuilder = highlightsBuilder,
                    )
                },
                codeFence = {
                    MarkdownHighlightedCodeFence(
                        content = it.content,
                        node = it.node,
                        style = it.typography.code,
                        highlightsBuilder = highlightsBuilder,
                    )
                },
            ),
        )
    }
}