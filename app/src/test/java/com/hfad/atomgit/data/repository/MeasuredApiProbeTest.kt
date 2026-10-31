package com.hfad.atomgit.data.repository

import com.hfad.atomgit.data.remote.RemoteTestFixture
import com.hfad.atomgit.data.remote.api.PullState
import com.hfad.atomgit.data.remote.api.UserPullScope
import com.hfad.atomgit.data.session.CurrentAccountSource
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.mockwebserver.MockResponse
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * 读本地 [docs/接口JSON对照复测.json]，按实测形状走 Repository。
 * 该文件不入库；CI 上没有时跳过。
 */
class MeasuredApiProbeTest {

    private val probe = loadMeasuredProbe()
    private lateinit var fixture: RemoteTestFixture
    private lateinit var accountSource: CurrentAccountSource
    private lateinit var feed: FeedRepository
    private lateinit var todo: TodoRepository

    @Before
    fun setUp() {
        assumeTrue("本地没有 docs/接口JSON对照复测.json", probe != null)
        fixture = RemoteTestFixture()
        fixture.tokenProvider.setToken("test-pat")
        accountSource = CurrentAccountSource().apply { set(id = "acct-1", login = "alice") }
        feed = DefaultFeedRepository(fixture.v5Service(), accountSource)
        todo = DefaultTodoRepository(fixture.v5Service(), accountSource)
    }

    @After
    fun tearDown() {
        if (this::fixture.isInitialized) fixture.close()
    }

    @Test
    fun userEventsStayGroupedAndKeepNoteLinkFields() = runTest {
        val events = probe!!.jsonObject["user_events"]!!.jsonObject
        assertTrue(events["events_is_object"]!!.jsonPrimitive.boolean)
        assertEquals("str", events["next_type"]!!.jsonPrimitive.content)
        val noteKeys = events["note_keys"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertTrue(noteKeys.containsAll(listOf("id", "noteable_type", "noteable_iid")))

        fixture.server.enqueue(
            MockResponse().setBody(
                """
                {
                  "events": {
                    "2026-10-17": [{
                      "action": 1,
                      "created_at": "2026-10-17T11:56:10.805Z",
                      "project_id": 9,
                      "note": {"id": 3, "noteable_type": "MergeRequest", "noteable_iid": 1}
                    }]
                  },
                  "next": "2026-10-17T11:56:10.805Z"
                }
                """.trimIndent(),
            ),
        )

        val page = feed.listMyEvents(next = null)
        assertEquals("MergeRequest", page.items.single().noteableType)
        assertEquals(1, page.items.single().noteableIid)
        assertEquals("2026-10-17T11:56:10.805Z", page.next)
    }

    @Test
    fun orgRepoPathUsesNamespacePathNotSpacedFullName() = runTest {
        val item = probe!!.jsonObject["org_repos"]!!.jsonObject["items"]!!.jsonArray.first().jsonObject
        val fullName = item["full_name"]!!.jsonPrimitive.content
        val namespacePath = item["namespace_path"]!!.jsonPrimitive.content
        val path = item["path"]!!.jsonPrimitive.content
        assertTrue(fullName.contains(' '))

        fixture.server.enqueue(
            MockResponse()
                .setHeader("total_count", probe.jsonObject["org_repos"]!!.jsonObject["total_count"]!!.jsonPrimitive.content)
                .setBody(
                    """
                    [{
                      "number": 2,
                      "title": "org issue",
                      "state": "open",
                      "issue_state": "待办的",
                      "repository": {
                        "full_name": "$fullName",
                        "path": "$path",
                        "namespace": {"path": "$namespacePath", "name": "${item["namespace_name"]!!.jsonPrimitive.content}"}
                      }
                    }]
                    """.trimIndent(),
                ),
        )

        val page = todo.listAssignedOpenIssues(page = 1, perPage = 20)
        assertEquals("$namespacePath/$path", page.items.single().repoPath)
        assertFalse(page.items.single().repoPath!!.contains(' '))
    }

    @Test
    fun pullScopesKeepAcceptFieldsAndDoNotQueryAssignedToMe() = runTest {
        val pull = probe!!.jsonObject["pull"]!!.jsonObject
        val assigneeAccept = pull["assignees"]!!.jsonArray.first().jsonObject["accept"]!!.jsonPrimitive.boolean
        val assigneeStatus = pull["assignees"]!!.jsonArray.first().jsonObject["accept_status"]
        assertTrue(assigneeStatus is JsonNull)
        val reviewerStatus = pull["approval_reviewers"]!!.jsonArray.first()
            .jsonObject["accept_status"]!!.jsonPrimitive.content
        val totalCount = probe.jsonObject["pull_scopes"]!!.jsonObject["need_my_approve"]!!
            .jsonObject["total_count"]!!.jsonPrimitive.content
        assertTrue(probe.jsonObject["pull_scopes"]!!.jsonObject.containsKey("assigned_to_me"))
        assertFalse(UserPullScope.entries.any { it.name == "AssignedToMe" })

        fixture.server.enqueue(
            MockResponse()
                .setHeader("total_count", totalCount)
                .setBody(
                    """
                    [{
                      "number": 1,
                      "title": "probe pr",
                      "state": "${pull["state"]!!.jsonPrimitive.content}",
                      "assignees": [{"accept": $assigneeAccept, "accept_status": null}],
                      "approval_reviewers": [{"accept": false, "accept_status": "$reviewerStatus"}],
                      "repository": {"path": "repo", "namespace": {"path": "alice"}}
                    }]
                    """.trimIndent(),
                ),
        )

        val page = todo.listNeedMyApprovePulls(PullState.Open, page = 1, perPage = 20)
        val item = page.items.single()
        assertEquals(totalCount.toLong(), page.totalCount)
        assertEquals(assigneeAccept, item.assignees.single().accept)
        assertNull(item.assignees.single().acceptStatus)
        assertEquals(reviewerStatus, item.approvalReviewers.single().acceptStatus)
        assertEquals(
            "/api/v5/user/pulls?scope=need_my_approve&state=open&page=1&per_page=20",
            fixture.server.takeRequest().path,
        )
    }
}

private fun loadMeasuredProbe(): kotlinx.serialization.json.JsonElement? {
    var dir = File(System.getProperty("user.dir") ?: return null)
    repeat(4) {
        val candidate = File(dir, "docs/接口JSON对照复测.json")
        if (candidate.isFile) {
            return Json.parseToJsonElement(candidate.readText())
        }
        dir = dir.parentFile ?: return null
    }
    return null
}
