package com.hfad.atomgit.ui.repo

private val ImagePattern = Regex("""!\[[^\]]*]\(\s*([^)\s]+)(?:\s+"[^"]*")?\s*\)""")
private val HtmlImagePattern = Regex("""<img\b[^>]*\bsrc\s*=\s*["']([^"']+)["'][^>]*>""", RegexOption.IGNORE_CASE)
private val LinkPattern = Regex("""(?<!!)\[[^\]]*]\(\s*([^)\s]+)(?:\s+"[^"]*")?\s*\)""")

/** 把仓库内的相对路径规整成仓库根路径，外部地址与锚点返回 null */
internal fun resolveReadmePath(baseDir: String, target: String): String? {
    if (target.isBlank()) return null
    if (target.startsWith("http://") || target.startsWith("https://")) return null
    if (target.startsWith("//") || target.startsWith("mailto:")) return null
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

/** 相对图片路径，按出现顺序去重 */
internal fun collectReadmeImages(markdown: String, baseDir: String): List<String> {
    val targets = buildList {
        ImagePattern.findAll(markdown).forEach { add(it.groupValues[1]) }
        HtmlImagePattern.findAll(markdown).forEach { add(it.groupValues[1]) }
    }
    return targets.mapNotNull { resolveReadmePath(baseDir, it) }.distinct()
}

/**
 * 重写 README：图片换成 raw 地址，相对文件链接换成仓库网页地址，锚点补全到当前文件
 */
internal fun rewriteReadme(
    markdown: String,
    baseDir: String,
    owner: String,
    repo: String,
    ref: String,
    filePath: String,
    imageUrls: Map<String, String>,
): String {
    val webBase = "https://atomgit.com/$owner/$repo/blob/$ref"
    fun rawUrl(target: String): String? {
        val path = resolveReadmePath(baseDir, target) ?: return null
        return imageUrls[path]
    }

    var result = markdown.replace(ImagePattern) { match ->
        val target = match.groupValues[1]
        val url = rawUrl(target) ?: target
        match.value.replace(target, url)
    }
    result = result.replace(HtmlImagePattern) { match ->
        val target = match.groupValues[1]
        val url = rawUrl(target) ?: target
        match.value.replace(target, url)
    }
    result = result.replace(LinkPattern) { match ->
        val target = match.groupValues[1]
        val resolved = when {
            target.startsWith("#") -> "$webBase/$filePath$target"
            else -> resolveReadmePath(baseDir, target)?.let { "$webBase/$it" }
        } ?: return@replace match.value
        match.value.replace(target, resolved)
    }
    return result
}
