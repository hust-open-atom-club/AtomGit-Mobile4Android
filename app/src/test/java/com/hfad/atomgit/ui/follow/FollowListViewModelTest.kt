package com.hfad.atomgit.ui.follow

import com.hfad.atomgit.MainDispatcherRule
import com.hfad.atomgit.data.model.FollowKind
import com.hfad.atomgit.data.model.PagedItems
import com.hfad.atomgit.data.model.UserSummary
import com.hfad.atomgit.data.remote.error.ApiError
import com.hfad.atomgit.data.remote.error.ApiResult
import com.hfad.atomgit.data.repository.FollowRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FollowListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeFollowRepository()

    @Test
    fun startLoadsFirstPage() = runTest {
        repository.enqueue(ok(user("a"), user("b")))

        val viewModel = FollowListViewModel(repository)
        viewModel.start("J1113449", FollowKind.Followers)

        val state = viewModel.state.value
        assertEquals(listOf("a", "b"), state.items.map { it.login })
        assertFalse(state.isLoading)
        assertFalse(state.endReached)
        assertEquals(Triple(FollowKind.Followers, 1, 30), repository.calls.single())
    }

    @Test
    fun loadMoreAppendsAndDropsDuplicate() = runTest {
        repository.enqueue(ok(user("a"), user("b")))
        repository.enqueue(ok(user("b"), user("c")))

        val viewModel = FollowListViewModel(repository)
        viewModel.start("J1113449", FollowKind.Followers)
        viewModel.loadMore()

        assertEquals(listOf("a", "b", "c"), viewModel.state.value.items.map { it.login })
        assertEquals(Triple(FollowKind.Followers, 2, 30), repository.calls.last())
    }

    @Test
    fun emptyPageEndsPagingAndBlocksFurtherLoads() = runTest {
        repository.enqueue(ok(user("a")))
        repository.enqueue(ok())

        val viewModel = FollowListViewModel(repository)
        viewModel.start("J1113449", FollowKind.Followers)
        viewModel.loadMore()

        assertTrue(viewModel.state.value.endReached)

        viewModel.loadMore()
        assertEquals(2, repository.calls.size)
    }

    @Test
    fun loadMoreFailureKeepsItemsAndExposesError() = runTest {
        repository.enqueue(ok(user("a")))
        repository.enqueue(ApiResult.Failure(ApiError.Network))

        val viewModel = FollowListViewModel(repository)
        viewModel.start("J1113449", FollowKind.Followers)
        viewModel.loadMore()

        val state = viewModel.state.value
        assertEquals(listOf("a"), state.items.map { it.login })
        assertEquals(ApiError.Network, state.error)
        assertFalse(state.isLoadingMore)
    }

    @Test
    fun retryAfterFailureReusesSamePage() = runTest {
        repository.enqueue(ApiResult.Failure(ApiError.Network))
        repository.enqueue(ok(user("a")))

        val viewModel = FollowListViewModel(repository)
        viewModel.start("J1113449", FollowKind.Followers)
        viewModel.retry()

        assertEquals(listOf("a"), viewModel.state.value.items.map { it.login })
        assertEquals(1, repository.calls.last().second)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun selectKindClearsListAndReloadsFromFirstPage() = runTest {
        repository.enqueue(ok(user("a")))
        repository.enqueue(ok(user("c")))

        val viewModel = FollowListViewModel(repository)
        viewModel.start("J1113449", FollowKind.Followers)
        viewModel.selectKind(FollowKind.Following)

        val state = viewModel.state.value
        assertEquals(FollowKind.Following, state.kind)
        assertEquals(listOf("c"), state.items.map { it.login })
        assertEquals(Triple(FollowKind.Following, 1, 30), repository.calls.last())
    }

    private fun user(login: String) = UserSummary(login = login)

    private fun ok(vararg users: UserSummary): ApiResult<PagedItems<UserSummary>> =
        ApiResult.Success(PagedItems(items = users.toList()))

    private class FakeFollowRepository : FollowRepository {

        val calls = mutableListOf<Triple<FollowKind, Int, Int>>()
        private val queued = ArrayDeque<ApiResult<PagedItems<UserSummary>>>()

        fun enqueue(result: ApiResult<PagedItems<UserSummary>>) {
            queued.addLast(result)
        }

        override suspend fun list(
            login: String,
            kind: FollowKind,
            page: Int,
            perPage: Int,
        ): ApiResult<PagedItems<UserSummary>> {
            calls += Triple(kind, page, perPage)
            return queued.removeFirst()
        }
    }
}
