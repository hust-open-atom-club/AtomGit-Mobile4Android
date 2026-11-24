package com.hfad.atomgit.ui.repo

import android.view.ViewGroup
import android.graphics.Color.BLACK
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import coil3.compose.AsyncImage
import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.SyntaxThemes
import com.mikepenz.markdown.coil3.Coil3ImageTransformerImpl
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.MarkdownHighlightedCodeBlock
import com.mikepenz.markdown.compose.elements.MarkdownHighlightedCodeFence
import com.mikepenz.markdown.compose.LocalImageWidth
import com.mikepenz.markdown.compose.MarkdownElement
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.ImageData
import com.mikepenz.markdown.model.ImageTransformer
import com.mikepenz.markdown.model.ImageWidth
import com.mikepenz.markdown.model.PlaceholderConfig
import com.mikepenz.markdown.model.State
import com.mikepenz.markdown.model.markdownAnimations
import org.intellij.markdown.ast.ASTNode
import androidx.core.graphics.drawable.toDrawable

/**
 * README 预览，按 Markdown 渲染。
 * 仓库内图片按需通过内容接口加载，外链图片交给 Coil。
 * 任意图片点击后打开全屏预览。
 */
@Composable
internal fun ReadmeMarkdownBlock(
    markdown: State.Success,
    node: ASTNode,
    imageLoader: ReadmeImageLoader? = null,
    imageBaseDir: String = "",
    contentWidth: Dp = Dp.Unspecified,
    onImageClick: (String) -> Unit = {},
    onLink: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var preview by remember { mutableStateOf<ImagePreviewTarget?>(null) }
    val handler = remember(onLink) {
        object : UriHandler {
            override fun openUri(uri: String) = onLink(uri)
        }
    }
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val highlightsBuilder = remember(dark) {
        Highlights.Builder().theme(SyntaxThemes.atom(darkMode = dark))
    }
    val transformer = remember(imageLoader, imageBaseDir, contentWidth) {
        ReadmeImageTransformer(imageLoader, imageBaseDir, contentWidth) { target ->
            preview = target
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
    CompositionLocalProvider(
        LocalUriHandler provides handler,
        LocalImageWidth provides ImageWidth.MAX_WIDTH,
    ) {
        Markdown(
            state = markdown,
            modifier = modifier.fillMaxWidth(),
            success = { document, components, blockModifier ->
                Column(blockModifier) { MarkdownElement(node, components, document.content) }
            },
            typography = typography,
            imageTransformer = transformer,
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
    preview?.let { target ->
        ImagePreviewDialog(
            target = target,
            onDismiss = { preview = null },
            onOpenFile = { path ->
                preview = null
                onImageClick(path)
            },
        )
    }
}

/** 预览来源：仓库内图片带字节与路径，外链只有地址 */
internal sealed interface ImagePreviewTarget {
    class Local(val path: String, val bytes: ByteArray, val thumbnail: ImageBitmap) :
        ImagePreviewTarget {
        val memorySize: Long get() = bytes.size + thumbnail.width * thumbnail.height * 4L
    }

    data class Remote(val url: String) : ImagePreviewTarget
}

private class ReadmeImageTransformer(
    private val loader: ReadmeImageLoader?,
    private val baseDir: String,
    private val contentWidth: Dp,
    private val onPreview: (ImagePreviewTarget) -> Unit,
) : ImageTransformer {

    @Composable
    override fun transform(link: String): ImageData? {
        val path = resolveReadmePath(baseDir, link) ?: return external(link)
        val imageLoader = loader ?: return null
        val image = key(imageLoader, path) {
            val loaded by produceState(initialValue = imageLoader.cached(path)) {
                if (value == null) value = imageLoader.load(path)
            }
            loaded
        } ?: return null
        return ImageData(
            painter = remember(image) { BitmapPainter(image.thumbnail) },
            modifier = Modifier.clickable { onPreview(image) },
            contentScale = ContentScale.FillWidth,
        )
    }

    @Composable
    private fun external(link: String): ImageData? {
        val url = if (link.startsWith("//")) "https:$link" else link
        val base = Coil3ImageTransformerImpl.transform(url) ?: return null
        return base.copy(modifier = Modifier.clickable { onPreview(ImagePreviewTarget.Remote(url)) })
    }

    @Composable
    override fun intrinsicSize(painter: Painter): Size =
        Coil3ImageTransformerImpl.intrinsicSize(painter)

    override fun placeholderConfig(
        link: String,
        density: Density,
        containerSize: Size,
        imageWidth: ImageWidth,
        imageSize: Size,
        imageSizeChanged: ((link: String, Size) -> Unit)?,
    ): PlaceholderConfig {
        val cached = if (imageSize.isUnspecified) {
            resolveReadmePath(baseDir, link)?.let { loader?.cached(it)?.thumbnail }
        } else null
        val size = cached?.let { Size(it.width.toFloat(), it.height.toFloat()) } ?: imageSize
        val container =
            if (!size.isUnspecified && containerSize.isUnspecified && contentWidth != Dp.Unspecified) {
                val width = with(density) { contentWidth.toPx() }
                Size(width, width)
            } else containerSize
        return Coil3ImageTransformerImpl.placeholderConfig(
            link, density, container, imageWidth, size, imageSizeChanged,
        )
    }
}

@Composable
private fun ImagePreviewDialog(
    target: ImagePreviewTarget,
    onDismiss: () -> Unit,
    onOpenFile: (String) -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        ),
    ) {
        val view = LocalView.current
        val window = (view.parent as? DialogWindowProvider)?.window
        SideEffect {
            window?.apply {
                setBackgroundDrawable(BLACK.toDrawable())
                setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                WindowCompat.setDecorFitsSystemWindows(this, false)
            }
        }
        var scale by remember { mutableFloatStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        var container by remember { mutableStateOf(IntSize.Zero) }

        fun clamp(value: Offset, currentScale: Float): Offset {
            if (currentScale <= 1f) return Offset.Zero
            val maxX = container.width * (currentScale - 1f) / 2f
            val maxY = container.height * (currentScale - 1f) / 2f
            return Offset(value.x.coerceIn(-maxX, maxX), value.y.coerceIn(-maxY, maxY))
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .onSizeChanged { container = it }
                .pointerInput(Unit) { detectTapGestures { onDismiss() } },
            contentAlignment = Alignment.Center,
        ) {
            val transform = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val next = (scale * zoom).coerceIn(1f, 5f)
                        scale = next
                        offset = clamp(offset + pan, next)
                    }
                }
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y,
                )
            when (target) {
                is ImagePreviewTarget.Local -> {
                    val bitmap by produceState(initialValue = target.thumbnail, target) {
                        decodeReadmeImage(target.bytes, maxSide = 2560)?.let { value = it }
                    }
                    Image(
                        bitmap = bitmap,
                        contentDescription = null,
                        modifier = transform,
                        contentScale = ContentScale.Fit,
                    )
                }

                is ImagePreviewTarget.Remote -> AsyncImage(
                    model = target.url,
                    contentDescription = null,
                    modifier = transform,
                    contentScale = ContentScale.Fit,
                )
            }
            if (target is ImagePreviewTarget.Local) {
                TextButton(
                    onClick = { onOpenFile(target.path) },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text("在文件中打开", color = Color.White)
                }
            }
        }
    }
}
