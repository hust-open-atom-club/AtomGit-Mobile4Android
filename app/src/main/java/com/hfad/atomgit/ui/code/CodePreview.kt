package com.hfad.atomgit.ui.code

internal data class CodePreview(val text: String, val message: String? = null, val supported: Boolean = true)

internal fun prepareCodePreview(content: String): CodePreview {
    if (content.take(8192).any { it == '\u0000' }) {
        return CodePreview("", "暂不支持预览二进制文件，请在仓库网页查看", supported = false)
    }
    val candidate = content.take(200_000)
    val lines = candidate.lineSequence().take(3001).toList()
    if (content.length <= 200_000 && lines.size <= 3000 && lines.none { it.length > 1000 }) {
        return CodePreview(content)
    }
    // 单行长度单独限制，避免超出 Compose 布局约束
    val limited = lines.take(3000).joinToString("\n") { it.take(1000) }
    return CodePreview(
        text = limited,
        message = "内容过长，已截断预览。复制和分享仍使用完整原文。",
    )
}
