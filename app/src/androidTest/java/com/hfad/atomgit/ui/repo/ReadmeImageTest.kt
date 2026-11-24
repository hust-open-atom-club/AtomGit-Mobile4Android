package com.hfad.atomgit.ui.repo

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.unit.dp
import com.hfad.atomgit.ui.theme.AtomGitTheme
import com.hfad.atomgit.data.model.RepoDetail
import com.hfad.atomgit.data.model.RepoEntry
import com.hfad.atomgit.data.model.RepoEntryKind
import com.mikepenz.markdown.model.State
import com.mikepenz.markdown.model.rememberMarkdownState
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ReadmeImageTest {
    @get:Rule
    val ui = createComposeRule(effectContext = StandardTestDispatcher())

    @Test
    fun imageReturningToCompositionDoesNotAnimateFromPlaceholderWidth() {
        val bytes = testPng()
        val requests = mutableListOf<String>()
        val visible = mutableStateOf(false)
        ui.setContent {
            AtomGitTheme {
                val parsed by rememberMarkdownState(
                    "![screenshot](screenshot.png)",
                    immediate = true
                ).state.collectAsState()
                val document = parsed as State.Success
                val scope = rememberCoroutineScope()
                val loader = remember(scope) {
                    ReadmeImageLoader(scope) { path ->
                        requests += path
                        bytes
                    }
                }
                Box(Modifier
                    .width(300.dp)
                    .testTag("image-container")) {
                    if (visible.value) ReadmeMarkdownBlock(
                        document, document.node.children.first(), loader, contentWidth = 300.dp,
                    )
                }
            }
        }

        val fullWidth = ui.onNodeWithTag("image-container").fetchSemanticsNode().boundsInRoot.width
        ui.mainClock.autoAdvance = false
        repeat(2) {
            ui.runOnIdle { visible.value = true }
            val partialWidths = mutableSetOf<Int>()
            ui.waitUntil(10_000) {
                ui.mainClock.advanceTimeByFrame()
                val width = ui.onAllNodesWithContentDescription("Image", useUnmergedTree = true)
                    .fetchSemanticsNodes().singleOrNull()?.boundsInRoot?.width ?: 0f
                if (width > 0f && width < fullWidth - 1) partialWidths.add(width.toInt())
                width >= fullWidth - 1
            }
            assertTrue("Intermediate image widths: $partialWidths", partialWidths.size <= 1)
            ui.runOnIdle { visible.value = false }
            ui.mainClock.advanceTimeByFrame()
        }
        ui.runOnIdle { assertEquals(listOf("screenshot.png"), requests) }
    }

    @Test
    fun eleventhImageLoadsOnScrollAndUsesTheCacheOnReturn() {
        val bytes = testPng()
        val requests = mutableListOf<String>()
        val markdown = buildString {
            repeat(80) { append("README paragraph $it " + "long content ".repeat(64) + "\n\n") }
            repeat(11) { append("![screenshot ${it + 1}](images/${it + 1}.png)\n\n") }
            repeat(40) { append("Ending paragraph $it\n\n") }
            append("End of the complete README")
        }
        val imageKey = "readme-block:${markdown.indexOf("![screenshot 11]")}"
        val ending = "End of the complete README"
        val state = RepoDetailUiState(
            owner = "team", repo = "project",
            detail = RepoDetail("team", "project", "project"),
            files = RepoDirectoryState(
                entries = listOf(
                    RepoEntry(
                        "README.md",
                        "README.md",
                        RepoEntryKind.File
                    )
                ), loaded = true
            ),
            readme = RepoReadmeState(path = "README.md", preview = markdown, loaded = true),
        )
        ui.setContent {
            AtomGitTheme {
                val scope = rememberCoroutineScope()
                val loader = remember(scope) {
                    ReadmeImageLoader(scope) { path ->
                        requests += path
                        bytes
                    }
                }
                RepoDetailContent(
                    state = state, onBack = {}, onTab = {}, onDirectory = {}, onRef = {},
                    onRefresh = {}, onRetry = {}, onRetryHeader = {}, onRetryReadme = {},
                    onLoadMore = {}, onOpenFile = {}, onOpenLink = {}, readmeImages = loader,
                )
            }
        }

        val list = ui.onNode(hasScrollToIndexAction())
        ui.waitUntil(10_000) {
            val semantics = list.fetchSemanticsNode().config
            SemanticsProperties.CollectionInfo in semantics &&
                    semantics[SemanticsProperties.CollectionInfo].rowCount > 100
        }
        ui.runOnIdle { assertTrue(requests.isEmpty()) }
        repeat(2) {
            list.performScrollToKey(imageKey)
            ui.waitUntil(10_000) {
                ui.runOnIdle { "images/11.png" in requests } &&
                        ui.onAllNodesWithContentDescription("Image", useUnmergedTree = true)
                            .fetchSemanticsNodes().isNotEmpty()
            }
            list.performScrollToIndex(0)
            ui.waitUntil(10_000) { ui.onNodeWithTag("repo-detail-header").isDisplayed() }
        }
        ui.runOnIdle { assertEquals(1, requests.count { it == "images/11.png" }) }
        list.performScrollToKey("readme-block:${markdown.indexOf("Ending paragraph 30")}")
        ui.waitUntil(10_000) { ui.onNodeWithText(ending).isDisplayed() }
    }

    private fun testPng(): ByteArray = ByteArrayOutputStream().use { output ->
        Bitmap.createBitmap(80, 40, Bitmap.Config.ARGB_8888)
            .compress(Bitmap.CompressFormat.PNG, 100, output)
        output.toByteArray()
    }
}
