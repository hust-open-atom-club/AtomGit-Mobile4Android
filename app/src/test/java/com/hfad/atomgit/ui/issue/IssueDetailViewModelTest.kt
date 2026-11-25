package com.hfad.atomgit.ui.issue

import com.hfad.atomgit.MainDispatcherRule
import com.hfad.atomgit.data.model.IssueComment
import com.hfad.atomgit.data.model.IssueDetail
import com.hfad.atomgit.data.remote.error.ApiError
import com.hfad.atomgit.data.remote.error.ApiResult
import com.hfad.atomgit.data.repository.IssueRepository
import com.hfad.atomgit.data.session.CurrentAccountSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class IssueDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeIssueRepository()
    private val accountSource = CurrentAccountSource().apply { set(id = "acct-1", login = "alice") }

    @Test
    fun startLoadsIssueAndComments() = runTest {
        repository.enqueueGet(ApiResult.Success(issue(state = "open", issueState = "待办的")))
        repository.enqueueComments(ApiResult.Success(listOf(comment(11, "第一条"))))

        val viewModel = viewModel()
        viewModel.start("acme", "demo", 3)

        val state = viewModel.state.value
        assertEquals("open", state.issue?.state)
        assertEquals("待办的", state.issue?.issueState)
        assertEquals(listOf(11L), state.comments.map { it.id })
        assertEquals(false, state.isLoading)
        assertEquals(listOf("get", "comments:1"), repository.calls)
    }

    @Test
    fun missingIssueShowsError() = runTest {
        repository.enqueueGet(ApiResult.Failure(ApiError.NotFound))
        repository.enqueueComments(ApiResult.Success(emptyList()))

        val viewModel = viewModel()
        viewModel.start("acme", "demo", 9)

        assertNull(viewModel.state.value.issue)
        assertEquals(ApiError.NotFound, viewModel.state.value.error)
    }

    @Test
    fun postCommentAppendsAndRereadKeepsOpen() = runTest {
        repository.enqueueGet(ApiResult.Success(issue(state = "open", issueState = "待办的")))
        repository.enqueueComments(ApiResult.Success(emptyList()))
        repository.enqueueCreate(ApiResult.Success(comment(21, "新评论")))
        repository.enqueueGet(ApiResult.Success(issue(state = "open", issueState = "待办的")))

        val viewModel = viewModel()
        viewModel.start("acme", "demo", 3)
        viewModel.updateDraft("新评论")
        viewModel.postComment()

        val state = viewModel.state.value
        assertEquals(listOf(21L), state.comments.map { it.id })
        assertEquals("", state.draft)
        assertEquals("open", state.issue?.state)
        assertEquals("待办的", state.issue?.issueState)
        assertNull(state.syncMessage)
        assertEquals(listOf("get", "comments:1", "create", "get"), repository.calls)
    }

    @Test
    fun postCommentRereadFailureKeepsPreviousIssue() = runTest {
        repository.enqueueGet(ApiResult.Success(issue(state = "open", issueState = "待办的")))
        repository.enqueueComments(ApiResult.Success(emptyList()))
        repository.enqueueCreate(ApiResult.Success(comment(21, "新评论")))
        repository.enqueueGet(ApiResult.Failure(ApiError.Network))

        val viewModel = viewModel()
        viewModel.start("acme", "demo", 3)
        viewModel.updateDraft("新评论")
        viewModel.postComment()

        val state = viewModel.state.value
        assertEquals("open", state.issue?.state)
        assertEquals("待办的", state.issue?.issueState)
        assertEquals(listOf(21L), state.comments.map { it.id })
        assertEquals("Issue 回读失败，完成状态尚待同步", state.syncMessage)
    }

    @Test
    fun blankCommentDoesNotCallServer() = runTest {
        repository.enqueueGet(ApiResult.Success(issue(state = "open", issueState = "待办的")))
        repository.enqueueComments(ApiResult.Success(emptyList()))

        val viewModel = viewModel()
        viewModel.start("acme", "demo", 3)
        viewModel.updateDraft("   ")
        viewModel.postComment()

        assertTrue(repository.calls.none { it == "create" })
        assertTrue(viewModel.state.value.comments.isEmpty())
    }

    @Test
    fun uncertainCommentIsNotResentAndStaysInDraft() = runTest {
        repository.enqueueGet(ApiResult.Success(issue(state = "open", issueState = "待办的")))
        repository.enqueueComments(ApiResult.Success(emptyList()))
        repository.enqueueCreate(ApiResult.Failure(ApiError.WriteUncertain))
        repository.enqueueComments(ApiResult.Success(emptyList()))

        val viewModel = viewModel()
        viewModel.start("acme", "demo", 3)
        viewModel.updateDraft("可能没发出去")
        viewModel.postComment()

        val state = viewModel.state.value
        assertEquals("可能没发出去", state.draft)
        assertEquals("评论结果不确定，请刷新确认后再发送", state.syncMessage)
        assertEquals(listOf("get", "comments:1", "create", "comments:1"), repository.calls)
        assertEquals("open", state.issue?.state)
    }

    @Test
    fun uncertainCommentConfirmedByRereadClearsDraft() = runTest {
        repository.enqueueGet(ApiResult.Success(issue(state = "open", issueState = "待办的")))
        repository.enqueueComments(ApiResult.Success(emptyList()))
        repository.enqueueCreate(ApiResult.Failure(ApiError.WriteUncertain))
        repository.enqueueComments(ApiResult.Success(listOf(comment(30, "已经在了", login = "alice"))))
        repository.enqueueGet(ApiResult.Success(issue(state = "open", issueState = "待办的")))

        val viewModel = viewModel()
        viewModel.start("acme", "demo", 3)
        viewModel.updateDraft("已经在了")
        viewModel.postComment()

        val state = viewModel.state.value
        assertEquals("", state.draft)
        assertNull(state.syncMessage)
        assertEquals(listOf(30L), state.comments.map { it.id })
        assertEquals("open", state.issue?.state)
        assertEquals(1, repository.calls.count { it == "create" })
    }

    private fun viewModel() = IssueDetailViewModel(repository, accountSource)

    private fun issue(state: String, issueState: String) = IssueDetail(
        number = 3,
        title = "标题",
        body = "正文",
        state = state,
        issueState = issueState,
        htmlUrl = "https://atomgit.com/acme/demo/issues/3",
        createdAt = "2026-02-01T01:02:03Z",
        updatedAt = "2026-03-01T01:02:03Z",
        authorLogin = "alice",
        authorName = "北不选狙",
    )

    private fun comment(id: Long, body: String, login: String = "alice") = IssueComment(
        id = id,
        body = body,
        authorLogin = login,
        authorName = null,
        createdAt = "2026-03-02T01:02:03Z",
    )
}

private class FakeIssueRepository : IssueRepository {
    val calls = mutableListOf<String>()
    private val gets = ArrayDeque<ApiResult<IssueDetail>>()
    private val commentPages = ArrayDeque<ApiResult<List<IssueComment>>>()
    private val creates = ArrayDeque<ApiResult<IssueComment>>()

    fun enqueueGet(result: ApiResult<IssueDetail>) {
        gets.add(result)
    }

    fun enqueueComments(result: ApiResult<List<IssueComment>>) {
        commentPages.add(result)
    }

    fun enqueueCreate(result: ApiResult<IssueComment>) {
        creates.add(result)
    }

    override suspend fun get(owner: String, repo: String, number: Int): ApiResult<IssueDetail> {
        calls += "get"
        return gets.removeFirst()
    }

    override suspend fun listComments(
        owner: String,
        repo: String,
        number: Int,
        page: Int,
        perPage: Int,
    ): ApiResult<List<IssueComment>> {
        calls += "comments:$page"
        return commentPages.removeFirst()
    }

    override suspend fun createComment(
        owner: String,
        repo: String,
        number: Int,
        body: String,
    ): ApiResult<IssueComment> {
        calls += "create"
        return creates.removeFirst()
    }
}
