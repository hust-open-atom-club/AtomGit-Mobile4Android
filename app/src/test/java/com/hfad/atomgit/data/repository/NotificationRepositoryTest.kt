package com.hfad.atomgit.data.repository

import com.hfad.atomgit.data.model.NotificationTarget
import com.hfad.atomgit.data.remote.RemoteTestFixture
import com.hfad.atomgit.data.remote.error.ApiError
import com.hfad.atomgit.data.remote.error.ApiResult
import com.hfad.atomgit.data.repository.mapping.notificationTarget
import com.hfad.atomgit.data.session.CurrentAccountSource
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NotificationRepositoryTest {

    private lateinit var fixture: RemoteTestFixture
    private lateinit var accountSource: CurrentAccountSource
    private lateinit var repository: NotificationRepository

    @Before
    fun setUp() {
        fixture = RemoteTestFixture()
        fixture.tokenProvider.setToken("test-pat")
        accountSource = CurrentAccountSource()
        accountSource.set(id = "acct-1", login = "alice")
        repository = DefaultNotificationRepository(
            api = fixture.v5Service(),
            accountSource = accountSource,
        )
    }

    @After
    fun tearDown() {
        fixture.close()
    }

    @Test
    fun listRepo_withoutAccount_returnsUnauthorizedWithoutRequest() = runTest {
        accountSource.clear()

        val result = repository.listRepo("acme", "demo", unread = true, page = 1, perPage = 20)

        assertEquals(ApiResult.Failure(ApiError.Unauthorized), result)
        assertEquals(0, fixture.server.requestCount)
    }

    @Test
    fun listRepo_mapsIssueAndKeepsRequestOwner() = runTest {
        fixture.server.enqueue(MockResponse().setResponseCode(200).setBody(NOTIFICATION_JSON))

        val items = repository.listRepo("acme", "demo", unread = true, page = 1, perPage = 20).valueOrFail()

        assertEquals(1, items.size)
        assertEquals("n1", items[0].id)
        assertEquals("acme", items[0].owner)
        assertEquals("demo", items[0].repo)
        assertEquals("Closed #30 as completed", items[0].content)
        assertEquals(true, items[0].unread)
        assertEquals("larkz-hh", items[0].actorLogin)
        assertEquals(NotificationTarget.Issue("acme", "demo", 30), items[0].target)
        assertEquals(
            "/api/v5/repos/acme/demo/notifications?unread=true&page=1&per_page=20",
            fixture.server.takeRequest().path,
        )
    }

    @Test
    fun markRead_sendsCommaSeparatedIds() = runTest {
        fixture.server.enqueue(MockResponse().setResponseCode(200))

        val result = repository.markRead("acme", "demo", listOf("n1", "n2"))

        assertEquals(ApiResult.Success(Unit), result)
        val recorded = fixture.server.takeRequest()
        assertEquals("PUT", recorded.method)
        assertEquals("n1,n2", recorded.requestUrl?.queryParameter("ids"))
    }

    @Test
    fun markRead_emptyIds_doesNotCallServer() = runTest {
        val result = repository.markRead("acme", "demo", emptyList())

        assertTrue(result is ApiResult.Failure)
        assertEquals(0, fixture.server.requestCount)
    }

    @Test
    fun notificationTarget_distinguishesPullWebAndRepo() {
        assertEquals(
            NotificationTarget.PullRequest("acme", "demo", 31),
            notificationTarget("acme", "demo", "https://atomgit.com/acme/demo/pulls/31"),
        )
        assertEquals(
            NotificationTarget.Web("https://atomgit.com/acme/demo/commit/abc"),
            notificationTarget("acme", "demo", "https://atomgit.com/acme/demo/commit/abc"),
        )
        assertEquals(
            NotificationTarget.Repository("acme", "demo"),
            notificationTarget("acme", "demo", null),
        )
    }
}

private const val NOTIFICATION_JSON = """
{
  "list": [{
    "id": "n1",
    "content": "Closed #30 as completed",
    "type": "event",
    "unread": true,
    "update_at": "2026-11-23T06:00:00Z",
    "html_url": "https://atomgit.com/acme/demo/issues/30",
    "actor": {"id": 1, "login": "larkz-hh", "name": "lark"},
    "repository": {"full_name": "someone / else"}
  }]
}
"""

private fun <T> ApiResult<T>.valueOrFail(): T = when (this) {
    is ApiResult.Success -> value
    is ApiResult.Failure -> throw AssertionError("expected success but was $error")
}
