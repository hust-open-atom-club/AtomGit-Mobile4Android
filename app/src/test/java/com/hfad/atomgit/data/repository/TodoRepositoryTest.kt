package com.hfad.atomgit.data.repository

import com.hfad.atomgit.data.model.TodoKind
import com.hfad.atomgit.data.remote.RemoteTestFixture
import com.hfad.atomgit.data.remote.api.IssueState
import com.hfad.atomgit.data.remote.api.PullState
import com.hfad.atomgit.data.session.CurrentAccountSource
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class TodoRepositoryTest {

    private lateinit var fixture: RemoteTestFixture
    private lateinit var accountSource: CurrentAccountSource
    private lateinit var repository: TodoRepository

    @Before
    fun setUp() {
        fixture = RemoteTestFixture()
        fixture.tokenProvider.setToken("test-pat")
        accountSource = CurrentAccountSource()
        accountSource.set(id = "acct-1", login = "alice")
        repository = DefaultTodoRepository(
            api = fixture.v5Service(),
            accountSource = accountSource,
        )
    }

    @After
    fun tearDown() {
        fixture.close()
    }

    @Test
    fun listAssignedOpenIssues_withoutAccount_doesNotSendRequest() = runTest {
        accountSource.clear()
        try {
            repository.listAssignedOpenIssues(page = 1, perPage = 20)
            fail("expected IllegalStateException")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("Current account unavailable"))
        }
        assertEquals(0, fixture.server.requestCount)
    }

    @Test
    fun listAssignedOpenIssues_usesAssignedOpenQueryAndHeaders() = runTest {
        fixture.server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("total_count", "1")
                .setHeader("total_page", "1")
                .setBody(
                    """
                    [{
                      "number": 2,
                      "title": "todo",
                      "state": "open",
                      "issue_state": "待办的",
                      "updated_at": "2026-10-17T10:00:00Z",
                      "repository": {
                        "path": "mobile-org-probe",
                        "full_name": "111 / mobile-org-probe",
                        "namespace": { "path": "donk945", "name": "111" }
                      }
                    }]
                    """.trimIndent(),
                ),
        )

        val page = repository.listAssignedOpenIssues(page = 1, perPage = 20)

        assertEquals(1L, page.totalCount)
        assertEquals(1, page.totalPage)
        val item = page.items.single()
        assertEquals(TodoKind.Issue, item.kind)
        assertEquals(2, item.number)
        assertEquals("open", item.state)
        assertEquals("待办的", item.issueState)
        assertEquals("donk945/mobile-org-probe", item.repoPath)
        assertTrue(item.dedupeKey.contains("donk945/mobile-org-probe"))

        val recorded = fixture.server.takeRequest()
        assertEquals(
            "/api/v5/user/issues?filter=assigned&state=open&page=1&per_page=20",
            recorded.path,
        )
    }

    @Test
    fun listCreatedIssues_emptyPageHasNullTotalsWhenHeadersMissing() = runTest {
        fixture.server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("[]"),
        )

        val page = repository.listCreatedIssues(
            state = IssueState.All,
            page = 99,
            perPage = 20,
        )

        assertTrue(page.items.isEmpty())
        assertNull(page.totalCount)
        assertNull(page.totalPage)

        val recorded = fixture.server.takeRequest()
        assertEquals(
            "/api/v5/user/issues?filter=created&state=all&page=99&per_page=20",
            recorded.path,
        )
    }

    @Test
    fun listNeedMyApprovePulls_keepsAcceptFieldsAndScope() = runTest {
        fixture.server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("total_count", "1")
                .setBody(
                    """
                    [{
                      "number": 1,
                      "title": "pr",
                      "state": "open",
                      "updated_at": "2026-10-17T12:00:00Z",
                      "assignees": [{"login":"alice","accept":false}],
                      "approval_reviewers": [{
                        "login":"alice",
                        "accept":false,
                        "accept_status":"pending"
                      }],
                      "repository": {
                        "path": "repo",
                        "full_name": "alice/repo",
                        "namespace": { "path": "alice" }
                      }
                    }]
                    """.trimIndent(),
                ),
        )

        val page = repository.listNeedMyApprovePulls(
            state = PullState.Open,
            page = 1,
            perPage = 10,
        )

        val item = page.items.single()
        assertEquals(TodoKind.PullRequest, item.kind)
        assertEquals("alice/repo", item.repoPath)
        assertEquals(false, item.assignees.single().accept)
        assertEquals("pending", item.approvalReviewers.single().acceptStatus)

        val recorded = fixture.server.takeRequest()
        assertEquals(
            "/api/v5/user/pulls?scope=need_my_approve&state=open&page=1&per_page=10",
            recorded.path,
        )
    }

    @Test
    fun listNeedMyReviewPulls_andCreatedByMe_useDistinctScopes() = runTest {
        fixture.server.enqueue(MockResponse().setResponseCode(200).setBody("[]"))
        repository.listNeedMyReviewPulls(PullState.Open, page = 1, perPage = 5)
        assertEquals(
            "/api/v5/user/pulls?scope=need_my_review&state=open&page=1&per_page=5",
            fixture.server.takeRequest().path,
        )

        fixture.server.enqueue(MockResponse().setResponseCode(200).setBody("[]"))
        repository.listCreatedByMePulls(PullState.Open, page = 1, perPage = 5)
        assertEquals(
            "/api/v5/user/pulls?scope=created_by_me&state=open&page=1&per_page=5",
            fixture.server.takeRequest().path,
        )
    }
}
