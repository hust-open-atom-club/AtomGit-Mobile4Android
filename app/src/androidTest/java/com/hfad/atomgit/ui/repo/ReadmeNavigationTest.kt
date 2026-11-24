package com.hfad.atomgit.ui.repo

import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.hfad.atomgit.data.model.RepoDetail
import com.hfad.atomgit.data.model.RepoEntry
import com.hfad.atomgit.data.model.RepoEntryKind
import com.hfad.atomgit.data.model.RepoRef
import com.hfad.atomgit.data.model.RepoRefKind
import com.hfad.atomgit.navigation.CodeViewerRoute
import com.hfad.atomgit.navigation.AtomGitLink
import com.hfad.atomgit.navigation.RepoDetailRoute
import com.hfad.atomgit.navigation.parseAtomGitLink
import com.hfad.atomgit.ui.theme.AtomGitTheme
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class ReadmeNavigationTest {
    @get:Rule val ui = createComposeRule(effectContext = StandardTestDispatcher())

    @Test fun returningFromLinkedFilePreservesParagraphAndOffset() {
        val url = "https://atomgit.com/team/project/blob/main/src/Main.kt"
        val markdown = (1..60).joinToString("\n\n") {
            if (it == 30) "[Main.kt]($url)" else "README paragraph $it"
        }
        val state = RepoDetailUiState(
            owner = "team", repo = "project",
            detail = RepoDetail("team", "project", "project"),
            selectedRef = RepoRef("main", RepoRefKind.Branch),
            files = RepoDirectoryState(entries = listOf(RepoEntry("README.md", "README.md", RepoEntryKind.File)), loaded = true),
            readme = RepoReadmeState(path = "README.md", preview = markdown, loaded = true),
        )
        val readme = mutableStateOf(state.readme)
        ui.setContent {
            AtomGitTheme {
                val stack = rememberNavBackStack(RepoDetailRoute("team", "project"))
                NavDisplay(backStack = stack, entryProvider = entryProvider<NavKey> {
                    entry<RepoDetailRoute> {
                        RepoDetailContent(
                            state = state.copy(readme = readme.value), onBack = {}, onTab = {}, onDirectory = {}, onRef = {},
                            onRefresh = {}, onRetry = {}, onRetryHeader = {}, onRetryReadme = {},
                            onLoadMore = {}, onOpenFile = {},
                            onOpenLink = { target ->
                                assertEquals(url, target)
                                val file = parseAtomGitLink(target) as AtomGitLink.File
                                stack.add(CodeViewerRoute(file.owner, file.repo, file.path, file.ref))
                            },
                        )
                    }
                    entry<CodeViewerRoute> { file ->
                        TextButton(onClick = {
                            readme.value = RepoReadmeState(isLoading = true)
                            stack.removeAt(stack.lastIndex)
                        }) { Text(file.path) }
                    }
                })
            }
        }

        val list = ui.onNode(hasScrollToIndexAction())
        val marker = ui.onNodeWithText("Main.kt")
        val frame = list.fetchSemanticsNode().boundsInRoot
        ui.waitUntil(20_000) {
            val y = ui.onAllNodesWithText("Main.kt").fetchSemanticsNodes().singleOrNull()?.boundsInRoot?.center?.y
            if (y != null && y in (frame.top + frame.height * .3f)..(frame.bottom - frame.height * .1f)) true
            else {
                list.performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, frame.height * .34f) }
                false
            }
        }
        val before = marker.fetchSemanticsNode().boundsInRoot
        ui.onNodeWithTag("repo-back-to-top").assertIsDisplayed()
        marker.performTouchInput { click(Offset(5f, centerY)) }
        ui.onNodeWithTag("screen-repo-detail").assertDoesNotExist()
        ui.onNodeWithText("src/Main.kt").performClick()
        ui.onNodeWithTag("screen-repo-detail").assertIsDisplayed()
        ui.runOnIdle { readme.value = state.readme }
        ui.waitUntil(10_000) { ui.onAllNodesWithText("Main.kt").fetchSemanticsNodes().isNotEmpty() }
        marker.assertIsDisplayed()
        assertEquals(before.top, marker.fetchSemanticsNode().boundsInRoot.top, 1f)
        val scrollRange = list.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange]
        ui.mainClock.autoAdvance = false
        var previous = ui.runOnIdle { scrollRange.value() }
        ui.onNodeWithTag("repo-back-to-top").performClick()
        repeat(6) {
            ui.mainClock.advanceTimeBy(80)
            val current = ui.runOnIdle { scrollRange.value() }
            assertTrue("回顶途中停顿或反向滚动: $previous -> $current", previous == 0f || current < previous)
            previous = current
        }
        assertEquals(0f, previous, 0f)
        ui.mainClock.autoAdvance = true
        ui.onNodeWithTag("repo-detail-header").assertIsDisplayed()
        ui.onNodeWithTag("repo-back-to-top").assertDoesNotExist()
    }
}
