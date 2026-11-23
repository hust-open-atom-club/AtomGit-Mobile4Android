package com.hfad.atomgit.ui.todo

import com.hfad.atomgit.MainDispatcherRule
import com.hfad.atomgit.data.model.PagedItems
import com.hfad.atomgit.data.model.TodoAssignee
import com.hfad.atomgit.data.model.TodoItem
import com.hfad.atomgit.data.model.TodoKind
import com.hfad.atomgit.data.model.TodoReviewer
import com.hfad.atomgit.data.remote.api.IssueState
import com.hfad.atomgit.data.remote.api.PullState
import com.hfad.atomgit.data.remote.error.ApiError
import com.hfad.atomgit.data.remote.error.ApiResult
import com.hfad.atomgit.data.repository.TodoRepository
import com.hfad.atomgit.data.session.CurrentAccountSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TodoViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeTodoRepository()
    private val accountSource = CurrentAccountSource().apply {
        set(id = "acct-1", login = "alice")
    }

    @Test
    fun startLoadsAssignedIssuesFirstPage() = runTest {
        repository.enqueueAssigned(ok(issue(1), issue(2)))

        val viewModel = TodoViewModel(repository, accountSource)
        viewModel.start()

        val state = viewModel.state.value
        assertEquals(TodoSource.AssignedIssues, state.source)
        assertEquals(listOf(1, 2), state.items.map { it.number })
        assertFalse(state.isLoading)
        assertFalse(state.endReached)
        assertEquals(listOf(Call.Assigned(1, 30)), repository.calls)
    }

    @Test
    fun loadMoreAppendsAndDropsDuplicate() = runTest {
        repository.enqueueAssigned(ok(issue(1), issue(2)))
        repository.enqueueAssigned(ok(issue(2), issue(3)))

        val viewModel = TodoViewModel(repository, accountSource)
        viewModel.start()
        viewModel.loadMore()

        assertEquals(listOf(1, 2, 3), viewModel.state.value.items.map { it.number })
        assertEquals(Call.Assigned(2, 30), repository.calls.last())
    }

    @Test
    fun emptyPageEndsPagingAndBlocksFurtherLoads() = runTest {
        repository.enqueueAssigned(ok(issue(1)))
        repository.enqueueAssigned(ok())

        val viewModel = TodoViewModel(repository, accountSource)
        viewModel.start()
        viewModel.loadMore()

        assertTrue(viewModel.state.value.endReached)
        viewModel.loadMore()
        assertEquals(2, repository.calls.size)
    }

    @Test
    fun loadMoreFailureKeepsItemsAndExposesError() = runTest {
        repository.enqueueAssigned(ok(issue(1)))
        repository.enqueueAssigned(ApiResult.Failure(ApiError.Network))

        val viewModel = TodoViewModel(repository, accountSource)
        viewModel.start()
        viewModel.loadMore()

        val state = viewModel.state.value
        assertEquals(listOf(1), state.items.map { it.number })
        assertEquals(ApiError.Network, state.error)
        assertFalse(state.isLoadingMore)
    }

    @Test
    fun retryAfterFailureReusesSamePage() = runTest {
        repository.enqueueAssigned(ApiResult.Failure(ApiError.Network))
        repository.enqueueAssigned(ok(issue(1)))

        val viewModel = TodoViewModel(repository, accountSource)
        viewModel.start()
        viewModel.retry()

        assertEquals(listOf(1), viewModel.state.value.items.map { it.number })
        assertEquals(Call.Assigned(1, 30), repository.calls.last())
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun selectSourceCachesPreviousAndDoesNotMixPages() = runTest {
        repository.enqueueAssigned(ok(issue(1)))
        repository.enqueueApprove(
            ok(
                pull(
                    number = 10,
                    assignees = listOf(TodoAssignee(login = "alice", accept = false)),
                ),
            ),
        )

        val viewModel = TodoViewModel(repository, accountSource)
        viewModel.start()
        viewModel.selectSource(TodoSource.NeedMyApprove)

        assertEquals(TodoSource.NeedMyApprove, viewModel.state.value.source)
        assertEquals(listOf(10), viewModel.state.value.items.map { it.number })

        viewModel.selectSource(TodoSource.AssignedIssues)
        assertEquals(listOf(1), viewModel.state.value.items.map { it.number })
        assertEquals(2, repository.calls.size)
    }

    @Test
    fun selectGroupSwitchesToDefaultSource() = runTest {
        repository.enqueueAssigned(ok(issue(1)))
        repository.enqueueCreatedIssues(ok(issue(9)))

        val viewModel = TodoViewModel(repository, accountSource)
        viewModel.start()
        viewModel.selectGroup(TodoGroup.Following)

        val state = viewModel.state.value
        assertEquals(TodoGroup.Following, state.group)
        assertEquals(TodoSource.CreatedIssues, state.source)
        assertEquals(listOf(9), state.items.map { it.number })
    }

    @Test
    fun needMyApproveFiltersAlreadyAcceptedForCurrentUser() = runTest {
        repository.enqueueAssigned(ok())
        repository.enqueueApprove(
            ok(
                pull(
                    number = 1,
                    assignees = listOf(TodoAssignee(login = "alice", accept = true)),
                ),
                pull(
                    number = 2,
                    assignees = listOf(TodoAssignee(login = "alice", accept = false)),
                ),
                pull(
                    number = 3,
                    assignees = listOf(TodoAssignee(login = "bob", accept = false)),
                ),
            ),
        )

        val viewModel = TodoViewModel(repository, accountSource)
        viewModel.start()
        viewModel.selectSource(TodoSource.NeedMyApprove)

        assertEquals(listOf(2), viewModel.state.value.items.map { it.number })
        assertFalse(viewModel.state.value.endReached)
    }

    @Test
    fun needMyReviewFiltersApprovedReviewer() = runTest {
        repository.enqueueAssigned(ok())
        repository.enqueueReview(
            ok(
                pull(
                    number = 1,
                    reviewers = listOf(
                        TodoReviewer(login = "alice", accept = false, acceptStatus = "approved"),
                    ),
                ),
                pull(
                    number = 2,
                    reviewers = listOf(
                        TodoReviewer(login = "alice", accept = false, acceptStatus = "pending"),
                    ),
                ),
            ),
        )

        val viewModel = TodoViewModel(repository, accountSource)
        viewModel.start()
        viewModel.selectSource(TodoSource.NeedMyReview)

        assertEquals(listOf(2), viewModel.state.value.items.map { it.number })
    }

    @Test
    fun filteredEmptyPageAutoContinuesToNextPage() = runTest {
        repository.enqueueAssigned(ok())
        repository.enqueueApprove(
            ok(
                pull(
                    number = 1,
                    assignees = listOf(TodoAssignee(login = "alice", accept = true)),
                ),
            ),
        )
        repository.enqueueApprove(
            ok(
                pull(
                    number = 2,
                    assignees = listOf(TodoAssignee(login = "alice", accept = false)),
                ),
            ),
        )

        val viewModel = TodoViewModel(repository, accountSource)
        viewModel.start()
        viewModel.selectSource(TodoSource.NeedMyApprove)

        assertEquals(listOf(2), viewModel.state.value.items.map { it.number })
        assertEquals(
            listOf(Call.Assigned(1, 30), Call.Approve(1, 30), Call.Approve(2, 30)),
            repository.calls,
        )
    }

    @Test
    fun totalCountFromHeaderIsExposed() = runTest {
        repository.enqueueAssigned(
            ApiResult.Success(PagedItems(items = listOf(issue(1)), totalCount = 42, totalPage = 3)),
        )

        val viewModel = TodoViewModel(repository, accountSource)
        viewModel.start()

        assertEquals(42L, viewModel.state.value.totalCount)
    }

    @Test
    fun splitOwnerRepoParsesValidPath() {
        assertEquals("donk945" to "mobile-org-probe", "donk945/mobile-org-probe".splitOwnerRepo())
        assertNull(null.splitOwnerRepo())
        assertNull("".splitOwnerRepo())
        assertNull("only-owner".splitOwnerRepo())
        assertNull("/repo".splitOwnerRepo())
        assertNull("owner/".splitOwnerRepo())
        assertNull("a/b/c".splitOwnerRepo())
    }

    @Test
    fun formatUpdatedAtShortensIsoTimestamp() {
        val formatted = formatUpdatedAt("2026-10-17T10:00:00Z")
        assertTrue(formatted!!.matches(Regex("""\d{2}-\d{2} \d{2}:\d{2}""")))
        assertNull(formatUpdatedAt(null))
        assertNull(formatUpdatedAt(""))
    }

    private fun issue(number: Int) = TodoItem(
        accountId = "acct-1",
        kind = TodoKind.Issue,
        number = number,
        title = "issue-$number",
        repoPath = "owner/repo",
        htmlUrl = null,
        updatedAt = "2026-01-01T00:00:00Z",
        state = "open",
        issueState = "待办的",
    )

    private fun pull(
        number: Int,
        assignees: List<TodoAssignee> = emptyList(),
        reviewers: List<TodoReviewer> = emptyList(),
    ) = TodoItem(
        accountId = "acct-1",
        kind = TodoKind.PullRequest,
        number = number,
        title = "pr-$number",
        repoPath = "owner/repo",
        htmlUrl = null,
        updatedAt = "2026-01-01T00:00:00Z",
        state = "open",
        assignees = assignees,
        approvalReviewers = reviewers,
    )

    private fun ok(vararg items: TodoItem): ApiResult<PagedItems<TodoItem>> =
        ApiResult.Success(PagedItems(items = items.toList()))

    private sealed interface Call {
        data class Assigned(val page: Int, val perPage: Int) : Call
        data class Approve(val page: Int, val perPage: Int) : Call
        data class Review(val page: Int, val perPage: Int) : Call
        data class CreatedIssues(val page: Int, val perPage: Int) : Call
    }

    private class FakeTodoRepository : TodoRepository {
        val calls = mutableListOf<Call>()
        private val assigned = ArrayDeque<ApiResult<PagedItems<TodoItem>>>()
        private val approve = ArrayDeque<ApiResult<PagedItems<TodoItem>>>()
        private val review = ArrayDeque<ApiResult<PagedItems<TodoItem>>>()
        private val createdIssues = ArrayDeque<ApiResult<PagedItems<TodoItem>>>()

        fun enqueueAssigned(result: ApiResult<PagedItems<TodoItem>>) {
            assigned.addLast(result)
        }

        fun enqueueApprove(result: ApiResult<PagedItems<TodoItem>>) {
            approve.addLast(result)
        }

        fun enqueueReview(result: ApiResult<PagedItems<TodoItem>>) {
            review.addLast(result)
        }

        fun enqueueCreatedIssues(result: ApiResult<PagedItems<TodoItem>>) {
            createdIssues.addLast(result)
        }

        override suspend fun listAssignedOpenIssues(
            page: Int,
            perPage: Int,
        ): ApiResult<PagedItems<TodoItem>> {
            calls += Call.Assigned(page, perPage)
            return assigned.removeFirst()
        }

        override suspend fun listCreatedIssues(
            state: IssueState,
            page: Int,
            perPage: Int,
        ): ApiResult<PagedItems<TodoItem>> {
            calls += Call.CreatedIssues(page, perPage)
            return createdIssues.removeFirst()
        }

        override suspend fun listCreatedByMePulls(
            state: PullState,
            page: Int,
            perPage: Int,
        ): ApiResult<PagedItems<TodoItem>> = error("unused")

        override suspend fun listNeedMyApprovePulls(
            state: PullState,
            page: Int,
            perPage: Int,
        ): ApiResult<PagedItems<TodoItem>> {
            calls += Call.Approve(page, perPage)
            return approve.removeFirst()
        }

        override suspend fun listNeedMyReviewPulls(
            state: PullState,
            page: Int,
            perPage: Int,
        ): ApiResult<PagedItems<TodoItem>> {
            calls += Call.Review(page, perPage)
            return review.removeFirst()
        }
    }
}
