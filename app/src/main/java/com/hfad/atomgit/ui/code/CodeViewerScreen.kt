package com.hfad.atomgit.ui.code

import android.graphics.BitmapFactory

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hfad.atomgit.data.model.CodeViewerPreferences
import com.hfad.atomgit.data.remote.error.ApiError
import com.hfad.atomgit.ui.components.CodeBlock
import com.hfad.atomgit.ui.theme.RepoFilterMenuDark

@Composable
fun CodeViewerScreen(
    owner: String,
    repo: String,
    path: String,
    ref: String,
    sample: Boolean,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CodeViewerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var menuExpanded by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(owner, repo, path, ref, sample) {
        viewModel.start(owner, repo, path, ref, sample)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .testTag("screen-code-viewer"),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("code-viewer-back")) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "返回",
                )
            }
            Text(
                text = state.displayPath,
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState())
                    .padding(end = 4.dp)
                    .testTag("code-viewer-path"),
                style = MaterialTheme.typography.titleSmall,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Visible,
                softWrap = false,
            )
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.testTag("code-viewer-settings"),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "代码设置",
                )
            }
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.testTag("code-viewer-more"),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = "更多",
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    containerColor = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) {
                        RepoFilterMenuDark
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                ) {
                    val menuItemColors = MenuDefaults.itemColors(
                        textColor = MaterialTheme.colorScheme.onSurface,
                        leadingIconColor = MaterialTheme.colorScheme.onSurface,
                        trailingIconColor = MaterialTheme.colorScheme.onSurface,
                    )
                    DropdownMenuItem(
                        text = { Text("复制路径") },
                        onClick = {
                            menuExpanded = false
                            copyText(context, state.displayPath, "路径已复制")
                        },
                        colors = menuItemColors,
                        modifier = Modifier.testTag("code-viewer-copy-path"),
                    )
                    DropdownMenuItem(
                        text = { Text("复制内容") },
                        enabled = state.previewSupported && !state.isLoading && state.error == null,
                        onClick = {
                            menuExpanded = false
                            copyText(context, state.content, "内容已复制")
                        },
                        colors = menuItemColors,
                        modifier = Modifier.testTag("code-viewer-copy-content"),
                    )
                    DropdownMenuItem(
                        text = { Text("分享") },
                        enabled = state.previewSupported && !state.isLoading && state.error == null,
                        onClick = {
                            menuExpanded = false
                            shareText(context, state.displayPath, state.content)
                        },
                        colors = menuItemColors,
                        modifier = Modifier.testTag("code-viewer-share"),
                    )
                }
            }
        }

        CodeViewerBody(
            state = state,
            preferences = preferences,
            onRetry = viewModel::retry,
        )
    }
}

@Composable
internal fun CodeViewerBody(
    state: CodeViewerUiState,
    preferences: CodeViewerPreferences,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            state.error != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = state.error.toMessage(),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    TextButton(
                        onClick = onRetry,
                        modifier = Modifier.testTag("code-viewer-retry"),
                    ) {
                        Text("重试")
                    }
                }
            }

            state.imageBytes != null -> ImagePreview(state.imageBytes)

            !state.previewSupported -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(24.dp), contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.previewMessage.orEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            else -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    state.previewMessage?.let {
                        Text(
                            it,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    CodeBlock(
                        code = state.previewContent ?: state.content,
                        preferences = preferences,
                        path = state.path.ifBlank { state.displayPath },
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("code-viewer-block"),
                    )
                }
            }
        }
    }
}

private fun copyText(context: Context, text: String, toast: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("code", text))
    Toast.makeText(context, toast, Toast.LENGTH_SHORT).show()
}

private fun shareText(context: Context, path: String, content: String) {
    val body = if (content.isBlank()) path else "$path\n\n$content"
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, path)
        putExtra(Intent.EXTRA_TEXT, body)
    }
    context.startActivity(Intent.createChooser(intent, "分享代码"))
}

private fun ApiError?.toMessage(): String = when (this) {
    ApiError.Unauthorized -> "未登录或凭证无效"
    ApiError.Forbidden -> "没有权限查看该文件"
    ApiError.NotFound -> "文件不存在"
    is ApiError.RateLimited -> "请求过于频繁，请稍后再试"
    ApiError.Network -> "网络异常"
    ApiError.Parse -> "文件内容解析失败"
    is ApiError.Server -> "服务端错误${code.takeIf { it > 0 }?.let { " ($it)" } ?: ""}"
    is ApiError.Unexpected -> message ?: "加载失败"
    ApiError.WriteUncertain -> "加载失败"
    is ApiError.Partial -> "加载失败"
    null -> "加载失败"
}
@Composable
private fun ImagePreview(bytes: ByteArray) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, bytes) {
        value = withContext(Dispatchers.Default) {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("code-viewer-image"),
        contentAlignment = Alignment.Center,
    ) {
        val image = bitmap
        if (image == null) {
            CircularProgressIndicator()
        } else {
            Image(
                bitmap = image,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                contentScale = ContentScale.FillWidth,
            )
        }
    }
}