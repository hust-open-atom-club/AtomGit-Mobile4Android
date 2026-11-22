package com.hfad.atomgit.ui.repo

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import android.graphics.BitmapFactory
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Density
import com.mikepenz.markdown.coil3.Coil3ImageTransformerImpl
import com.mikepenz.markdown.compose.LocalImageWidth
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.ImageData
import com.mikepenz.markdown.model.ImageTransformer
import com.mikepenz.markdown.model.ImageWidth
import com.mikepenz.markdown.model.PlaceholderConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * README 预览，按 Markdown 渲染
 * 仓库内图片用预取的字节渲染（避免 raw 域名的鉴权问题），外链图片仍交给 Coil。
 */
@Composable
internal fun ReadmeMarkdown(
    markdown: String,
    images: Map<String, ByteArray> = emptyMap(),
    onLink: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val handler = remember(onLink) {
        object : UriHandler {
            override fun openUri(uri: String) = onLink(uri)
        }
    }
    val transformer = remember(images) { ReadmeImageTransformer(images) }
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
        )
    }
}

private class ReadmeImageTransformer(
    private val images: Map<String, ByteArray>,
) : ImageTransformer {

    @Composable
    override fun transform(link: String): ImageData? {
        val bytes = images[link] ?: return Coil3ImageTransformerImpl.transform(link)
        // 后台解码，避免在组合期解图卡首屏
        val bitmap by produceState<ImageBitmap?>(initialValue = null, bytes) {
            value = withContext(Dispatchers.Default) {
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }
        }
        val image = bitmap ?: return null
        return ImageData(
            painter = remember(image) { BitmapPainter(image) },
            contentScale = ContentScale.FillWidth,
        )
    }

    override fun placeholderConfig(
        link: String,
        density: Density,
        containerSize: Size,
        imageWidth: ImageWidth,
        imageSize: Size,
        imageSizeChanged: ((link: String, Size) -> Unit)?,
    ): PlaceholderConfig = Coil3ImageTransformerImpl.placeholderConfig(
        link = link,
        density = density,
        containerSize = containerSize,
        imageWidth = imageWidth,
        imageSize = imageSize,
        imageSizeChanged = imageSizeChanged,
    )
}
