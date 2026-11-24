package com.hfad.atomgit.ui.repo

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.hfad.atomgit.ui.theme.AtomGitTheme
import com.mikepenz.markdown.model.State
import com.mikepenz.markdown.model.rememberMarkdownState
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ReadmeImageTest {
    @get:Rule val ui = createComposeRule()

    @Test fun imageReturningToCompositionDoesNotAnimateFromPlaceholderWidth() {
        val link = blobLink("screenshot.png")
        val bytes = ByteArrayOutputStream().use { output ->
            Bitmap.createBitmap(80, 40, Bitmap.Config.ARGB_8888)
                .compress(Bitmap.CompressFormat.PNG, 100, output)
            output.toByteArray()
        }
        val images = mapOf(link to bytes)
        val visible = mutableStateOf(false)
        ui.setContent {
            AtomGitTheme {
                val parsed by rememberMarkdownState("![screenshot]($link)", immediate = true).state.collectAsState()
                val document = parsed as State.Success
                Box(Modifier.width(300.dp).testTag("image-container")) {
                    if (visible.value) ReadmeMarkdownBlock(document, document.node.children.first(), images)
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
            // A single placeholder size is fine. A sequence of widths is a visible reveal.
            assertTrue("Intermediate image widths: $partialWidths", partialWidths.size <= 1)
            ui.runOnIdle { visible.value = false }
            ui.mainClock.advanceTimeByFrame()
        }
    }
}
