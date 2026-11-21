package com.hfad.atomgit.ui.preview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hfad.atomgit.data.model.CodeViewerPreferences
import com.hfad.atomgit.data.model.CommitSummary
import com.hfad.atomgit.data.model.RepoDetail
import com.hfad.atomgit.data.model.RepoEntry
import com.hfad.atomgit.data.model.RepoEntryKind
import com.hfad.atomgit.data.model.RepoRef
import com.hfad.atomgit.data.model.RepoRefKind
import com.hfad.atomgit.ui.code.CodeViewerBody
import com.hfad.atomgit.ui.code.CodeViewerUiState
import com.hfad.atomgit.ui.components.CodeBlock
import com.hfad.atomgit.ui.repo.RepoDetailContent
import com.hfad.atomgit.ui.repo.RepoDetailTab
import com.hfad.atomgit.ui.repo.RepoDetailUiState
import com.hfad.atomgit.ui.repo.RepoDirectoryState
import com.hfad.atomgit.ui.repo.RepoPageState
import com.hfad.atomgit.ui.repo.RepoReadmeState
import com.hfad.atomgit.ui.theme.AtomGitTheme

private const val SAMPLE_CODE = """
package com.hfad.atomgit

fun greet(name: String, times: Int = 1): String {
    // 示例注释：用于检查语法高亮与行号
    val path = "src/main/kotlin/com/hfad/atomgit/Sample.kt"
    return "Hello, " + name + "!".repeat(times)
}
"""

private val DarkPrefs = CodeViewerPreferences(alwaysDarkTheme = true, showLineNumbers = true)
private val LightPrefs = CodeViewerPreferences(alwaysDarkTheme = false, showLineNumbers = true)

private val UnsupportedState = CodeViewerUiState(
    owner = "potato-security",
    repo = "PotatoTool",
    path = "dist/PotatoTool-2.5.jar",
    ref = "main",
    previewSupported = false,
    previewMessage = "该文件暂不支持预览，请在项目网页查看",
)

private val SampleDetail = RepoDetail(
    owner = "potato-security",
    path = "PotatoTool",
    name = "PotatoTool",
    description = "一款基于 JavaFX 的桌面安全工具，包含流量解包、信息收集、漏洞扫描等能力。",
    stars = 58,
    isPrivate = false,
    defaultBranch = "main",
    htmlUrl = "https://atomgit.com/potato-security/PotatoTool",
    ownerName = "土豆.exe",
    forks = 4,
)

private val SampleState = RepoDetailUiState(
    owner = "potato-security",
    repo = "PotatoTool",
    detail = SampleDetail,
    tab = RepoDetailTab.Files,
    selectedRef = RepoRef(name = "main", kind = RepoRefKind.Branch, isDefault = true),
    files = RepoDirectoryState(
        path = "",
        entries = listOf(
            RepoEntry(name = "src", path = "src", kind = RepoEntryKind.Directory),
            RepoEntry(name = ".gitignore", path = ".gitignore", kind = RepoEntryKind.File),
            RepoEntry(name = "LICENSE", path = "LICENSE", kind = RepoEntryKind.File),
            RepoEntry(name = "README.md", path = "README.md", kind = RepoEntryKind.File),
            RepoEntry(name = "pom.xml", path = "pom.xml", kind = RepoEntryKind.File),
        ),
        loaded = true,
    ),
    readme = RepoReadmeState(
        path = "README.md",
        preview = "# PotatoTool" + "\n\n" + "一款基于 JavaFX 的桌面安全工具。",
        loaded = true,
    ),
    commits = RepoPageState(
        items = listOf(
            CommitSummary(
                sha = "ec63139911e65c60c7f6744539174bc32e9f319f",
                message = "docs(readme): 补充国内托管说明",
                date = "2026-11-20T23:05:12+08:00",
            ),
        ),
        loaded = true,
    ),
)

@Preview(name = "代码块 · 深色", showBackground = true, heightDp = 420)
@Composable
private fun CodeBlockDarkPreview() {
    AtomGitTheme(darkTheme = true) {
        Box(Modifier.fillMaxWidth().height(400.dp)) {
            CodeBlock(
                code = SAMPLE_CODE,
                preferences = DarkPrefs,
                path = "Sample.kt",
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Preview(name = "代码块 · 浅色", showBackground = true, heightDp = 420)
@Composable
private fun CodeBlockLightPreview() {
    AtomGitTheme(darkTheme = false) {
        Box(Modifier.fillMaxWidth().height(400.dp)) {
            CodeBlock(
                code = SAMPLE_CODE,
                preferences = LightPrefs,
                path = "Sample.kt",
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Preview(name = "代码页 · 不支持预览 · 深色", showBackground = true, heightDp = 480)
@Composable
private fun CodeViewerUnsupportedDarkPreview() {
    AtomGitTheme(darkTheme = true) {
        CodeViewerBody(state = UnsupportedState, preferences = DarkPrefs, onRetry = {})
    }
}

@Preview(name = "代码页 · 不支持预览 · 浅色", showBackground = true, heightDp = 480)
@Composable
private fun CodeViewerUnsupportedLightPreview() {
    AtomGitTheme(darkTheme = false) {
        CodeViewerBody(state = UnsupportedState, preferences = LightPrefs, onRetry = {})
    }
}

@Preview(name = "项目详情 · 深色", showBackground = true, heightDp = 900)
@Composable
private fun RepoDetailDarkPreview() {
    AtomGitTheme(darkTheme = true) {
        RepoDetailContent(
            state = SampleState,
            onBack = {},
            onTab = {},
            onDirectory = {},
            onRef = {},
            onRefresh = {},
            onRetry = {},
            onRetryHeader = {},
            onRetryReadme = {},
            onLoadMore = {},
            onOpenFile = {},
        )
    }
}

@Preview(name = "项目详情 · 浅色", showBackground = true, heightDp = 900)
@Composable
private fun RepoDetailLightPreview() {
    AtomGitTheme(darkTheme = false) {
        RepoDetailContent(
            state = SampleState,
            onBack = {},
            onTab = {},
            onDirectory = {},
            onRef = {},
            onRefresh = {},
            onRetry = {},
            onRetryHeader = {},
            onRetryReadme = {},
            onLoadMore = {},
            onOpenFile = {},
        )
    }
}