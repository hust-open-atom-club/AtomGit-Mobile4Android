package com.hfad.atomgit.ui.notification

import com.hfad.atomgit.MainDispatcherRule
import com.hfad.atomgit.data.model.NotificationItem
import com.hfad.atomgit.data.model.NotificationTarget
import com.hfad.atomgit.data.model.PagedItems
import com.hfad.atomgit.data.model.RepoSort
import com.hfad.atomgit.data.model.RepoSummary
import com.hfad.atomgit.data.model.RepoVisibility
import com.hfad.atomgit.data.remote.error.ApiError
import com.hfad.atomgit.data.remote.error.ApiResult
import com.hfad.atomgit.data.repository.NotificationRepository
import com.hfad.atomgit.data.repository.RepoRepository
import com.hfad.atomgit.data.repository.mapping.parseNotificationTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val notifications = FakeNotificationRepository()
    private val repos = FakeRepoRepository(
        listOf(
            RepoSummary(ownerPath = "acme", path = "demo", name = "demo"),
            RepoSummary(ownerPath = "acme", path = "other", name = "other"),
        ),
    )

    @Test
    fun startLoadsAllFromEachRepoAndGroupsSameIssue() = runTest {
        notifications.enqueue("acme", "demo", unread = false, ApiResult.Success(listOf(issue("n1", 30), issue("n2", 30, millis = 2_000))))
        notifications.enqueue("acme", "other", unread = false, ApiResult.Success(listOf(issue("n3", 4, repo = "other"))))

        val viewModel = viewModel()
        viewModel.start()

        val state = viewModel.state.value
        assertEquals(listOf("acme/demo|issue|30", "acme/other|issue|4"), state.threads.map { it.key }.sorted())
        val grouped = state.threads.first { it.key == "acme/demo|issue|30" }
        assertEquals(2, grouped.count)
        assertEquals(listOf("n1", "n2"), grouped.unreadIds.sorted())
        assertEquals(2, notifications.calls.size)
        assertTrue(notifications.calls.all { !it.unread && it.page == 1 })
    }

    @Test
    fun partialFailureKeepsSuccessfulRepo() = runTest {
        notifications.enqueue("acme", "demo", unread = false, ApiResult.Success(listOf(issue("n1", 30))))
        notifications.enqueue("acme", "other", unread = false, ApiResult.Failure(ApiError.Network))

        val viewModel = viewModel()
        viewModel.start()

        val state = viewModel.state.value
        assertEquals(listOf("acme/demo|issue|30"), state.threads.map { it.key })
        assertEquals(listOf("acme/other"), state.partialFailures.map { it.source })
        assertNull(state.error)
    }

    @Test
    fun inboxShowsOnlyUnreadUntilReadFilterSelected() = runTest {
        notifications.enqueue(
            "acme",
            "demo",
            unread = false,
            ApiResult.Success(
                listOf(
                    issue("n1", 30, unread = true),
                    issue("n9", 9, unread = false),
                ),
            ),
        )
        notifications.enqueue("acme", "other", unread = false, ApiResult.Success(emptyList()))

        val viewModel = viewModel()
        viewModel.start()
        assertEquals(listOf(30), viewModel.state.value.threads.map { (it.target as NotificationTarget.Issue).number })

        val calls = notifications.calls.size
        viewModel.selectKind(NotificationKindFilter.Read)

        assertEquals(calls, notifications.calls.size)
        assertEquals(NotificationKindFilter.Read, viewModel.state.value.kindFilter)
        assertEquals(listOf(9), viewModel.state.value.threads.map { (it.target as NotificationTarget.Issue).number })
        assertTrue(viewModel.state.value.hasActiveFilter)
    }

    @Test
    fun kindAndRepoFiltersDoNotRefetch() = runTest {
        notifications.enqueue(
            "acme",
            "demo",
            unread = false,
            ApiResult.Success(listOf(issue("n1", 30, type = "event"), issue("n2", 8, type = "referer"))),
        )
        notifications.enqueue("acme", "other", unread = false, ApiResult.Success(emptyList()))

        val viewModel = viewModel()
        viewModel.start()
        val calls = notifications.calls.size
        viewModel.selectKind(NotificationKindFilter.Mention)
        viewModel.selectRepo("acme/demo")

        assertEquals(calls, notifications.calls.size)
        assertEquals(listOf(8), viewModel.state.value.threads.map { (it.target as NotificationTarget.Issue).number })
    }

    @Test
    fun clearFiltersDropsEveryFilterWithoutRefetch() = runTest {
        notifications.enqueue("acme", "demo", unread = false, ApiResult.Success(listOf(issue("n1", 30, type = "event"))))
        notifications.enqueue("acme", "other", unread = false, ApiResult.Success(emptyList()))

        val viewModel = viewModel()
        viewModel.start()
        val calls = notifications.calls.size
        viewModel.toggleImportant()
        viewModel.selectKind(NotificationKindFilter.Event)
        viewModel.selectRepo("acme/demo")
        viewModel.clearFilters()

        val state = viewModel.state.value
        assertEquals(false, state.importantOnly)
        assertEquals(NotificationKindFilter.All, state.kindFilter)
        assertNull(state.selectedRepoKey)
        assertEquals(false, state.hasActiveFilter)
        assertEquals(calls, notifications.calls.size)
    }

    @Test
    fun importantToggleKeepsMentionsWithoutRefetch() = runTest {
        notifications.enqueue(
            "acme",
            "demo",
            unread = false,
            ApiResult.Success(listOf(issue("n1", 30, type = "event"), issue("n2", 8, type = "referer"))),
        )
        notifications.enqueue("acme", "other", unread = false, ApiResult.Success(emptyList()))

        val viewModel = viewModel()
        viewModel.start()
        val calls = notifications.calls.size
        viewModel.toggleImportant()

        assertEquals(calls, notifications.calls.size)
        assertTrue(viewModel.state.value.importantOnly)
        assertEquals(listOf(8), viewModel.state.value.threads.map { (it.target as NotificationTarget.Issue).number })
    }

    @Test
    fun markReadDropsThreadFromInbox() = runTest {
        notifications.enqueue("acme", "demo", unread = false, ApiResult.Success(listOf(issue("n1", 30))))
        notifications.enqueue("acme", "other", unread = false, ApiResult.Success(emptyList()))

        val viewModel = viewModel()
        viewModel.start()
        val thread = viewModel.state.value.threads.single()
        viewModel.markRead(thread)

        assertEquals(listOf("n1"), notifications.marks.single().ids)
        assertTrue(viewModel.state.value.threads.isEmpty())
    }

    @Test
    fun markReadFailureKeepsThreadAndExplains() = runTest {
        notifications.enqueue("acme", "demo", unread = false, ApiResult.Success(listOf(issue("n1", 30))))
        notifications.enqueue("acme", "other", unread = false, ApiResult.Success(emptyList()))
        notifications.markResult = ApiResult.Failure(ApiError.Network)

        val viewModel = viewModel()
        viewModel.start()
        viewModel.markRead(viewModel.state.value.threads.single())

        assertTrue(viewModel.state.value.threads.single().unread)
        assertEquals("标为已读失败，仍保留为未读", viewModel.state.value.notice)
    }

    @Test
    fun markUnreadRestoresUnreadLocallyOnReadFilter() = runTest {
        notifications.enqueue(
            "acme",
            "demo",
            unread = false,
            ApiResult.Success(listOf(issue("n1", 30, unread = false))),
        )
        notifications.enqueue("acme", "other", unread = false, ApiResult.Success(emptyList()))

        val viewModel = viewModel()
        viewModel.start()
        viewModel.selectKind(NotificationKindFilter.Read)
        assertFalse(viewModel.state.value.threads.single().unread)
        viewModel.markUnread(viewModel.state.value.threads.single())

        assertTrue(viewModel.state.value.threads.isEmpty())
        viewModel.selectKind(NotificationKindFilter.All)
        assertTrue(viewModel.state.value.threads.single().unread)
        assertTrue(notifications.marks.isEmpty())
    }

    @Test
    fun toggleReadMarksReadAndLeavesInbox() = runTest {
        notifications.enqueue("acme", "demo", unread = false, ApiResult.Success(listOf(issue("n1", 30))))
        notifications.enqueue("acme", "other", unread = false, ApiResult.Success(emptyList()))

        val viewModel = viewModel()
        viewModel.start()
        viewModel.toggleRead(viewModel.state.value.threads.single())
        assertTrue(viewModel.state.value.threads.isEmpty())
    }

    @Test
    fun formatNotificationAge_usesHourBucket() {
        val raw = "2026-11-23T04:00:00Z"
        val now = parseNotificationTime(raw)!! + 2 * 60 * 60 * 1000
        assertEquals("2 小时", formatNotificationAge(raw, now))
    }

    private fun viewModel() = NotificationViewModel(notifications, repos)

    private fun issue(
        id: String,
        number: Int,
        repo: String = "demo",
        type: String = "event",
        unread: Boolean = true,
        millis: Long = 1_000L,
    ) = NotificationItem(
        id = id,
        owner = "acme",
        repo = repo,
        content = "title $number",
        type = type,
        unread = unread,
        updatedAt = "2026-11-23T06:00:00Z",
        updatedAtMillis = millis,
        htmlUrl = null,
        actorLogin = "larkz-hh",
        actorName = null,
        actorAvatarUrl = null,
        target = NotificationTarget.Issue("acme", repo, number),
    )

    private class FakeRepoRepository(
        private val repos: List<RepoSummary>,
    ) : RepoRepository {
        var calls: Int = 0

        override suspend fun listMine(
            query: String,
            visibility: RepoVisibility,
            sort: RepoSort,
            page: Int,
            perPage: Int,
        ): ApiResult<PagedItems<RepoSummary>> {
            calls += 1
            return ApiResult.Success(PagedItems(repos))
        }

        override suspend fun countMine(): Long? = null
    }

    private class FakeNotificationRepository : NotificationRepository {
        val calls = mutableListOf<ListCall>()
        val marks = mutableListOf<MarkCall>()
        var markResult: ApiResult<Unit> = ApiResult.Success(Unit)
        private val scripts = mutableMapOf<String, ArrayDeque<ApiResult<List<NotificationItem>>>>()

        fun enqueue(
            owner: String,
            repo: String,
            unread: Boolean,
            result: ApiResult<List<NotificationItem>>,
        ) {
            scripts.getOrPut("$owner/$repo/$unread") { ArrayDeque() }.addLast(result)
        }

        override suspend fun listRepo(
            owner: String,
            repo: String,
            unread: Boolean,
            page: Int,
            perPage: Int,
        ): ApiResult<List<NotificationItem>> {
            calls += ListCall(owner, repo, unread, page, perPage)
            return scripts["$owner/$repo/$unread"]?.removeFirstOrNull() ?: ApiResult.Success(emptyList())
        }

        override suspend fun markRead(
            owner: String,
            repo: String,
            ids: List<String>,
        ): ApiResult<Unit> {
            marks += MarkCall(owner, repo, ids)
            return markResult
        }
    }

    private data class ListCall(
        val owner: String,
        val repo: String,
        val unread: Boolean,
        val page: Int,
        val perPage: Int,
    )

    private data class MarkCall(
        val owner: String,
        val repo: String,
        val ids: List<String>,
    )
}
