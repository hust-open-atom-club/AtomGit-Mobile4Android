package com.hfad.atomgit.ui.repo

private val LinkPattern = Regex("""(?<!!)\[[^\]]*]\(\s*([^)\s]+)(?:\s+"[^"]*")?\s*\)""")
private val AbsoluteScheme = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:")

/** 把仓库内的相对路径规整成仓库根路径，外部地址与锚点返回 null */
internal fun resolveReadmePath(baseDir: String, target: String): String? {
    if (target.isBlank()) return null
    if (target.startsWith("//") || AbsoluteScheme.containsMatchIn(target)) return null
    if (target.startsWith("#")) return null
    val path = target.substringBefore('#').substringBefore('?')
    if (path.isBlank()) return null
    val segments = ArrayList<String>()
    if (!path.startsWith("/")) baseDir.split('/').filter { it.isNotEmpty() }.forEach(segments::add)
    path.split('/').filter { it.isNotEmpty() }.forEach { segment ->
        when (segment) {
            "." -> Unit
            ".." -> if (segments.isNotEmpty()) segments.removeAt(segments.lastIndex)
            else -> segments.add(segment)
        }
    }
    return segments.joinToString("/").ifBlank { null }
}

/**
 * 相对文件链接换成仓库网页地址，锚点补全到当前文件。
 * 图片在对应的 Markdown 块渲染时才解析路径
 */
internal fun rewriteReadme(
    markdown: String,
    baseDir: String,
    owner: String,
    repo: String,
    ref: String,
    filePath: String,
): String {
    val webBase = "https://atomgit.com/$owner/$repo/blob/$ref"
    return markdown.replace(LinkPattern) { match ->
        val target = match.groupValues[1]
        val resolved = when {
            target.startsWith("#") -> "$webBase/$filePath$target"
            else -> resolveReadmePath(baseDir, target)?.let { "$webBase/$it" }
        } ?: return@replace match.value
        match.value.replace(target, resolved)
    }
}
