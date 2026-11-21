package com.hfad.atomgit.navigation

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.hfad.atomgit.data.model.PagedItems
import com.hfad.atomgit.data.model.TodoItem
import com.hfad.atomgit.data.remote.api.IssueState
import com.hfad.atomgit.data.remote.api.PullState
import com.hfad.atomgit.data.remote.error.ApiResult
import com.hfad.atomgit.data.repository.TodoRepository
import com.hfad.atomgit.data.session.CurrentAccountSource
import com.hfad.atomgit.ui.theme.AtomGitTheme
import com.hfad.atomgit.ui.todo.TodoScreen
import com.hfad.atomgit.ui.todo.TodoViewModel
import org.junit.Rule
import org.junit.Test

/**
 * 导航骨架的测试：动态占位详情往返；待办页在无 Hilt 环境下直接挂载 ViewModel。
 */
class NavigationSmokeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun openingDetailFromFeedReturnsToSameTab() {
        composeRule.setContent {
            AtomGitTheme {
                AtomGitNavHost()
            }
        }

        composeRule.onNodeWithTag("screen-feed").assertIsDisplayed()

        composeRule.onNodeWithText("打开 Issue 详情").performClick()
        composeRule.onNodeWithTag("screen-issue-detail").assertIsDisplayed()
        composeRule.onNodeWithTag("tab-Feed").assertIsDisplayed()

        composeRule.onNodeWithText("返回").performClick()
        composeRule.onNodeWithTag("screen-feed").assertIsDisplayed()
        composeRule.onNodeWithTag("tab-Feed").assertIsDisplayed()
    }

    @Test
    fun todoScreenShowsPendingGroupTabs() {
        val viewModel = TodoViewModel(
            repository = EmptyTodoRepository(),
            accountSource = CurrentAccountSource().apply {
                set(id = "acct-1", login = "alice")
            },
        )

        composeRule.setContent {
            AtomGitTheme {
                TodoScreen(
                    listState = rememberLazyListState(),
                    onOpenIssue = { _, _, _ -> },
                    onOpenPull = { _, _, _ -> },
                    viewModel = viewModel,
                )
            }
        }

        composeRule.onNodeWithTag("screen-todo").assertIsDisplayed()
        composeRule.onNodeWithTag("todo-group-Pending").assertIsDisplayed()
        composeRule.onNodeWithTag("todo-source-AssignedIssues").assertIsDisplayed()
    }

    private class EmptyTodoRepository : TodoRepository {
        override suspend fun listAssignedOpenIssues(
            page: Int,
            perPage: Int,
        ): ApiResult<PagedItems<TodoItem>> = ApiResult.Success(PagedItems(emptyList()))

        override suspend fun listCreatedIssues(
            state: IssueState,
            page: Int,
            perPage: Int,
        ): ApiResult<PagedItems<TodoItem>> = ApiResult.Success(PagedItems(emptyList()))

        override suspend fun listCreatedByMePulls(
            state: PullState,
            page: Int,
            perPage: Int,
        ): ApiResult<PagedItems<TodoItem>> = ApiResult.Success(PagedItems(emptyList()))

        override suspend fun listNeedMyApprovePulls(
            state: PullState,
            page: Int,
            perPage: Int,
        ): ApiResult<PagedItems<TodoItem>> = ApiResult.Success(PagedItems(emptyList()))

        override suspend fun listNeedMyReviewPulls(
            state: PullState,
            page: Int,
            perPage: Int,
        ): ApiResult<PagedItems<TodoItem>> = ApiResult.Success(PagedItems(emptyList()))
    }
}
