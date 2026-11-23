package com.hfad.atomgit.ui.repo

import android.view.ViewGroup
import android.graphics.BitmapFactory
import android.graphics.Color.BLACK
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.BitmapPainter
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
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.ImageData
import com.mikepenz.markdown.model.ImageTransformer
import com.mikepenz.markdown.model.ImageWidth
import com.mikepenz.markdown.model.PlaceholderConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.graphics.drawable.toDrawable

/**
 * README 预览，按 Markdown 渲染。
 * 仓库内图片用预取的字节渲染（避免 raw 域名的鉴权问题），外链图片仍交给 Coil。
 * 任意图片点击后打开全屏预览。
 */
@Composable
internal fun ReadmeMarkdown(
    markdown: String,
    images: Map<String, ByteArray> = emptyMap(),
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
    val transformer = remember(images) {
        ReadmeImageTransformer(images) { target -> preview = target }
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
            content = markdown,
            modifier = modifier.fillMaxWidth(),
            typography = typography,
            imageTransformer = transformer,
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

private const val IMAGE_LINK_PREFIX = "atomgit-image:"

/** 预览来源：仓库内图片带字节与路径，外链只有地址 */
internal sealed interface ImagePreviewTarget {
    data class Local(val path: String, val bytes: ByteArray) : ImagePreviewTarget {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Local

            if (path != other.path) return false
            if (!bytes.contentEquals(other.bytes)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = path.hashCode()
            result = 31 * result + bytes.contentHashCode()
            return result
        }
    }

    data class Remote(val url: String) : ImagePreviewTarget
}

private class ReadmeImageTransformer(
    private val images: Map<String, ByteArray>,
    private val onPreview: (ImagePreviewTarget) -> Unit,
) : ImageTransformer {

    @Composable
    override fun transform(link: String): ImageData? {
        val bytes = images[link] ?: return external(link)
        val bitmap by produceState<ImageBitmap?>(initialValue = null, bytes) {
            value = withContext(Dispatchers.Default) {
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }
        }
        val image = bitmap ?: return null
        val path = link.removePrefix(IMAGE_LINK_PREFIX)
        return ImageData(
            painter = remember(image) { BitmapPainter(image) },
            modifier = Modifier.clickable { onPreview(ImagePreviewTarget.Local(path, bytes)) },
            contentScale = ContentScale.FillWidth,
        )
    }

    @Composable
    private fun external(link: String): ImageData? {
        val base = Coil3ImageTransformerImpl.transform(link) ?: return null
        return base.copy(modifier = Modifier.clickable { onPreview(ImagePreviewTarget.Remote(link)) })
    }

    override fun placeholderConfig(
        link: String,
        density: Density,
        containerSize: androidx.compose.ui.geometry.Size,
        imageWidth: ImageWidth,
        imageSize: androidx.compose.ui.geometry.Size,
        imageSizeChanged: ((link: String, androidx.compose.ui.geometry.Size) -> Unit)?,
    ): PlaceholderConfig = Coil3ImageTransformerImpl.placeholderConfig(
        link = link,
        density = density,
        containerSize = containerSize,
        imageWidth = imageWidth,
        imageSize = imageSize,
        imageSizeChanged = imageSizeChanged,
    )
}

@Composable
private fun ImagePreviewDialog(
    target: ImagePreviewTarget,
    onDismiss: () -> Unit,
    onOpenFile: (String) -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
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
                    val bitmap by produceState<ImageBitmap?>(initialValue = null, target.bytes) {
                        value = withContext(Dispatchers.Default) {
                            BitmapFactory.decodeByteArray(target.bytes, 0, target.bytes.size)?.asImageBitmap()
                        }
                    }
                    val image = bitmap
                    if (image == null) {
                        Text("图片解码失败", color = Color.White)
                    } else {
                        Image(
                            bitmap = image,
                            contentDescription = null,
                            modifier = transform,
                            contentScale = ContentScale.Fit,
                        )
                    }
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