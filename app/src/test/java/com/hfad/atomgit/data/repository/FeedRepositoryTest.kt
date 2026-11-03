package com.hfad.atomgit.data.repository

import com.hfad.atomgit.data.remote.RemoteTestFixture
import com.hfad.atomgit.data.session.CurrentAccountSource
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

class FeedRepositoryTest {

    private lateinit var fixture: RemoteTestFixture
    private lateinit var accountSource: CurrentAccountSource
    private lateinit var repository: FeedRepository

    @Before
    fun setUp() {
        fixture = RemoteTestFixture()
        fixture.tokenProvider.setToken("test-pat")
        accountSource = CurrentAccountSource()
        accountSource.set(id = "acct-1", login = "alice")
        repository = DefaultFeedRepository(
            api = fixture.v5Service(),
            accountSource = accountSource,
        )
    }

    @After
    fun tearDown() {
        fixture.close()
    }

    @Test
    fun listMyEvents_withoutAccount_doesNotSendRequest() = runTest {
        accountSource.clear()
        try {
            repository.listMyEvents(next = null)
            fail("expected IllegalStateException")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("Current account unavailable"))
        }
        assertEquals(0, fixture.server.requestCount)
    }

    @Test
    fun listMyEvents_flattensDateGroupsNewestFirstAndReturnsNext() = runTest {
        fixture.server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "events": {
                        "2026-10-16": [
                          {
                            "action": 1,
                            "action_name": "opened",
                            "created_at": "2026-10-16T10:00:00.000Z",
                            "project_id": 1,
                            "project_name": "alice/old"
                          }
                        ],
                        "2026-10-17": [
                          {
                            "action": 2,
                            "action_name": "commented",
                            "created_at": "2026-10-17T11:00:00.000Z",
                            "project_id": 2,
                            "project_name": "alice/new",
                            "note": {
                              "id": 9,
                              "noteable_type": "Issue",
                              "noteable_iid": 2
                            },
                            "_links": {
                              "action_type": "https://atomgit.com/alice/new/issues/2"
                            }
                          }
                        ]
                      },
                      "next": "2026-10-16T09:00:00.000Z"
                    }
                    """.trimIndent(),
                ),
        )

        val page = repository.listMyEvents(next = null)

        assertEquals(2, page.items.size)
        assertEquals("2026-10-17T11:00:00.000Z", page.items[0].createdAt)
        assertEquals("Issue", page.items[0].noteableType)
        assertEquals(2, page.items[0].noteableIid)
        assertEquals("https://atomgit.com/alice/new/issues/2", page.items[0].actionUrl)
        assertEquals("2026-10-16T10:00:00.000Z", page.items[1].createdAt)
        assertEquals("2026-10-16T09:00:00.000Z", page.next)
        assertTrue(page.items[0].dedupeKey.contains("9"))

        val recorded = fixture.server.takeRequest()
        assertEquals("/api/v5/users/alice/events", recorded.path)
    }

    @Test
    fun listMyEvents_passesNextCursor() = runTest {
        fixture.server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"events":{},"next":null}"""),
        )

        val page = repository.listMyEvents(next = "2026-10-16T09:00:00.000Z")

        assertTrue(page.items.isEmpty())
        assertNull(page.next)
        val recorded = fixture.server.takeRequest()
        assertEquals(
            "/api/v5/users/alice/events?next=2026-10-16T09%3A00%3A00.000Z",
            recorded.path,
        )
    }

    @Test
    fun listRepoEvents_mapsHasNextPage() = runTest {
        fixture.server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "events": [
                        {
                          "action": 3,
                          "action_name": "push",
                          "created_at": "2026-10-17T12:00:00.000Z",
                          "project_id": 7,
                          "title": "pushed to main"
                        }
                      ],
                      "has_next_page": 1
                    }
                    """.trimIndent(),
                ),
        )

        val page = repository.listRepoEvents(
            owner = "alice",
            repo = "demo",
            page = 1,
            perPage = 20,
        )

        assertEquals(1, page.items.size)
        assertTrue(page.hasNextPage)
        assertEquals("push", page.items.single().actionName)

        fixture.server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"events":[],"has_next_page":0}"""),
        )
        val empty = repository.listRepoEvents("alice", "demo", page = 2, perPage = 20)
        assertTrue(empty.items.isEmpty())
        assertFalse(empty.hasNextPage)

        val first = fixture.server.takeRequest()
        assertEquals("/api/v5/repos/alice/demo/events?page=1&per_page=20", first.path)
    }
}
