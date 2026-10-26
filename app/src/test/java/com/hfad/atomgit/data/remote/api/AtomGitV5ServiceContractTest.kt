package com.hfad.atomgit.data.remote.api

import com.hfad.atomgit.data.remote.RemoteTestFixture
import com.hfad.atomgit.data.remote.dto.ApprovePullRequestBody
import com.hfad.atomgit.data.remote.dto.CommentBody
import com.hfad.atomgit.data.remote.totalCountHeader
import com.hfad.atomgit.data.remote.totalPageHeader
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.IOException

class AtomGitV5ServiceContractTest {

    private lateinit var fixture: RemoteTestFixture
    private lateinit var api: AtomGitV5Service

    @Before
    fun setUp() {
        fixture = RemoteTestFixture()
        fixture.tokenProvider.setToken("test-pat")
        api = fixture.v5Service()
    }

    @After
    fun tearDown() {
        fixture.close()
    }

    @Test
    fun getUser_usesBearerHeaderNotQueryToken() = runTest {
        fixture.server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """{"id":"6a7760a0533376275c1e1ccd","login":"J1113449","name":"author"}""",
                ),
        )

        val user = api.getAuthenticatedUser()

        assertEquals("6a7760a0533376275c1e1ccd", user.id)
        assertEquals("J1113449", user.login)

        val recorded = fixture.server.takeRequest()
        assertEquals("GET", recorded.method)
        assertEquals("/api/v5/user", recorded.path)
        assertEquals("Bearer test-pat", recorded.getHeader("Authorization"))
        assertNull(recorded.requestUrl?.queryParameter("access_token"))
    }

    @Test
    fun missingToken_doesNotSendRequest() = runTest {
        fixture.tokenProvider.clear()
        try {
            api.getAuthenticatedUser()
            fail("expected IOException")
        } catch (e: IOException) {
            assertTrue(e.message!!.contains("Auth token unavailable"))
        }
        assertEquals(0, fixture.server.requestCount)
    }

    @Test
    fun listUserEvents_parsesDateGroupedObjectAndIgnoresUnknownKeys() = runTest {
        fixture.server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "events": {
                        "2026-10-17": [
                          {
                            "action": 1,
                            "action_name": "opened",
                            "created_at": "2026-10-17T11:56:10.805Z",
                            "project_id": 42,
                            "unknown_future_field": true,
                            "note": {
                              "id": 9,
                              "noteable_type": "Issue",
                              "noteable_iid": 2
                            },
                            "_links": {
                              "action_type": "https://atomgit.com/o/r/issues/2"
                            }
                          }
                        ]
                      },
                      "next": "2026-10-17T11:56:10.805Z"
                    }
                    """.trimIndent(),
                ),
        )

        val page = api.listUserEvents("J1113449", next = null)

        assertEquals("2026-10-17T11:56:10.805Z", page.next)
        val events = page.events["2026-10-17"]!!
        assertEquals(1, events.size)
        assertEquals("Issue", events[0].note?.noteableType)
        assertEquals(2, events[0].note?.noteableIid)

        val recorded = fixture.server.takeRequest()
        assertEquals("/api/v5/users/J1113449/events", recorded.path)
        assertFalse(recorded.path!!.contains("limit="))
    }

    @Test
    fun approvePullRequest_sendsActionApproveAndAccepts204() = runTest {
        fixture.server.enqueue(MockResponse().setResponseCode(204))

        val response = api.approvePullRequest(
            owner = "J1113449",
            repo = "mobile-api-probe-20261017",
            number = 1,
            body = ApprovePullRequestBody.approve(),
        )

        assertTrue(response.isSuccessful)
        assertEquals(204, response.code())

        val recorded = fixture.server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals(
            "/api/v5/repos/J1113449/mobile-api-probe-20261017/pulls/1/review",
            recorded.path,
        )
        assertEquals("""{"action":"approve"}""", recorded.body.readUtf8())
    }

    @Test
    fun markNotificationsRead_requiresIdsQuery() = runTest {
        fixture.server.enqueue(MockResponse().setResponseCode(200))

        val response = api.markRepoNotificationsRead(
            owner = "J1113449",
            repo = "mobile-api-probe-20261017",
            ids = "n1,n2",
        )

        assertTrue(response.isSuccessful)

        val recorded = fixture.server.takeRequest()
        assertEquals("PUT", recorded.method)
        assertEquals(
            "/api/v5/repos/J1113449/mobile-api-probe-20261017/notifications?ids=n1%2Cn2",
            recorded.path,
        )
        assertEquals("n1,n2", recorded.requestUrl?.queryParameter("ids"))
    }

    @Test
    fun listTags_accepts201EmptyArray() = runTest {
        fixture.server.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setBody("[]"),
        )

        val response = api.listTags("owner", "repo", page = 1, perPage = 20)

        assertTrue(response.isSuccessful)
        assertEquals(201, response.code())
        assertTrue(response.body()!!.isEmpty())

        val recorded = fixture.server.takeRequest()
        assertEquals("/api/v5/repos/owner/repo/tags?page=1&per_page=20", recorded.path)
    }

    @Test
    fun listUserIssues_readsTotalHeadersWithoutTreatingMissingAsZero() = runTest {
        fixture.server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("total_count", "1")
                .setHeader("total_page", "1")
                .setBody("""[{"number":2,"title":"todo","state":"open","issue_state":"待办的"}]"""),
        )

        val response = api.listUserIssues(
            filter = UserIssueFilter.Assigned,
            state = IssueState.Open,
            page = 1,
            perPage = 20,
        )

        assertEquals(1L, response.totalCountHeader())
        assertEquals(1, response.totalPageHeader())
        assertEquals(2, response.body()!!.single().number)

        fixture.server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("[]"),
        )
        val empty = api.listUserIssues(
            filter = UserIssueFilter.Assigned,
            state = IssueState.Open,
            page = 99,
            perPage = 20,
        )
        assertNull(empty.totalCountHeader())
        assertTrue(empty.body()!!.isEmpty())
    }

    @Test
    fun createIssueComment_postsBodyOnly() = runTest {
        fixture.server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"id":1,"body":"hello"}"""),
        )

        api.createIssueComment("o", "r", 2, CommentBody("hello"))

        val recorded = fixture.server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/api/v5/repos/o/r/issues/2/comments", recorded.path)
        assertEquals("""{"body":"hello"}""", recorded.body.readUtf8())
    }
}
