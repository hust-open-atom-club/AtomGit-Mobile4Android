package com.hfad.atomgit.data.repository

import com.hfad.atomgit.data.remote.RemoteTestFixture
import com.hfad.atomgit.data.remote.error.ApiError
import com.hfad.atomgit.data.remote.error.ApiResult
import com.hfad.atomgit.data.session.CurrentAccountSource
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class IssueRepositoryTest {

    private lateinit var fixture: RemoteTestFixture
    private lateinit var accountSource: CurrentAccountSource
    private lateinit var repository: IssueRepository

    @Before
    fun setUp() {
        fixture = RemoteTestFixture()
        fixture.tokenProvider.setToken("test-pat")
        accountSource = CurrentAccountSource()
        accountSource.set(id = "acct-1", login = "alice")
        repository = DefaultIssueRepository(
            api = fixture.v5Service(),
            accountSource = accountSource,
        )
    }

    @After
    fun tearDown() {
        fixture.close()
    }

    @Test
    fun get_withoutAccount_returnsUnauthorizedWithoutRequest() = runTest {
        accountSource.clear()

        val result = repository.get("acme", "demo", 3)

        assertEquals(ApiResult.Failure(ApiError.Unauthorized), result)
        assertEquals(0, fixture.server.requestCount)
    }

    @Test
    fun get_mapsStateAndIssueState() = runTest {
        fixture.server.enqueue(MockResponse().setResponseCode(200).setBody(ISSUE_JSON))

        val issue = repository.get("acme", "demo", 3).valueOrFail()

        assertEquals(3, issue.number)
        assertEquals("标题", issue.title)
        assertEquals("正文", issue.body)
        assertEquals("closed", issue.state)
        assertEquals("待办的", issue.issueState)
        assertEquals("alice", issue.authorLogin)
        assertEquals("北不选狙", issue.authorName)
        assertEquals(listOf("bob"), issue.assignees)
        assertEquals("/api/v5/repos/acme/demo/issues/3", fixture.server.takeRequest().path)
    }

    @Test
    fun get_acceptsStringNumberAndStringUserId() = runTest {
        fixture.server.enqueue(MockResponse().setResponseCode(200).setBody(MEASURED_ISSUE_JSON))

        val issue = repository.get("donk945", "mobile-org-probe", 4).valueOrFail()

        assertEquals(4, issue.number)
        assertEquals("open", issue.state)
        assertEquals("待办的", issue.issueState)
        assertEquals("J1113449", issue.authorLogin)
        assertEquals(listOf("J1113449"), issue.assignees)
        assertEquals("donk945/mobile-org-probe", issue.repoPath)
        assertEquals(listOf("enhancement"), issue.labels.map { it.name })
    }

    @Test
    fun listComments_acceptsStringUserId() = runTest {
        fixture.server.enqueue(MockResponse().setResponseCode(200).setBody(MEASURED_COMMENTS_JSON))

        val comments = repository.listComments("donk945", "mobile-org-probe", 3, page = 1, perPage = 30)
            .valueOrFail()

        assertEquals("第二条评论：同 Issue 多条通知应合并成一行并显示角标。", comments.single().body)
        assertEquals("J1113449", comments.single().authorLogin)
    }

    @Test
    fun get_notFound() = runTest {
        fixture.server.enqueue(MockResponse().setResponseCode(404).setBody("""{"message":"missing"}"""))

        val result = repository.get("acme", "demo", 9)

        assertEquals(ApiResult.Failure(ApiError.NotFound), result)
    }

    @Test
    fun listComments_mapsAuthorAndPage() = runTest {
        fixture.server.enqueue(MockResponse().setResponseCode(200).setBody(COMMENTS_JSON))

        val comments = repository.listComments("acme", "demo", 3, page = 1, perPage = 30).valueOrFail()

        assertEquals(1, comments.size)
        assertEquals(11L, comments[0].id)
        assertEquals("看一下", comments[0].body)
        assertEquals("alice", comments[0].authorLogin)
        assertEquals(
            "/api/v5/repos/acme/demo/issues/3/comments?page=1&per_page=30",
            fixture.server.takeRequest().path,
        )
    }

    @Test
    fun createComment_postsBodyAndDoesNotImplyClosed() = runTest {
        fixture.server.enqueue(MockResponse().setResponseCode(201).setBody(COMMENT_JSON))

        val comment = repository.createComment("acme", "demo", 3, "  看一下  ").valueOrFail()

        assertEquals(11L, comment.id)
        assertEquals("看一下", comment.body)
        val recorded = fixture.server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/api/v5/repos/acme/demo/issues/3/comments", recorded.path)
        assertTrue(recorded.body.readUtf8().contains("\"body\":\"看一下\""))
    }

    @Test
    fun createComment_blankBody_doesNotCallServer() = runTest {
        val result = repository.createComment("acme", "demo", 3, "  ")

        assertTrue(result is ApiResult.Failure)
        assertEquals(0, fixture.server.requestCount)
    }
}

private fun <T> ApiResult<T>.valueOrFail(): T = when (this) {
    is ApiResult.Success -> value
    is ApiResult.Failure -> error(error.toString())
}

private const val ISSUE_JSON = """
{
  "id": 9,
  "number": 3,
  "title": "标题",
  "body": "正文",
  "state": "closed",
  "issue_state": "待办的",
  "html_url": "https://atomgit.com/acme/demo/issues/3",
  "created_at": "2026-02-01T01:02:03Z",
  "updated_at": "2026-03-01T01:02:03Z",
  "user": {"login": "alice", "name": "北不选狙"},
  "assignees": [{"login": "bob"}]
}
"""

private const val COMMENT_JSON = """
{"id": 11, "body": "看一下", "user": {"login": "alice", "name": "北不选狙"}, "created_at": "2026-03-02T01:02:03Z"}
"""

private const val COMMENTS_JSON = "[$COMMENT_JSON]"

private const val MEASURED_ISSUE_JSON = """
{
  "id": 4598809,
  "html_url": "https://atomgit.com/donk945/mobile-org-probe/issues/4",
  "number": "4",
  "state": "open",
  "title": "[通知测试] 仓库筛选与相对时间样例",
  "body": "测仓库筛选",
  "user": {"id": "6a7760a0533376275c1e1ccd", "login": "J1113449", "name": "北不选狙"},
  "assignees": [{"id": "6a7760a0533376275c1e1ccd", "login": "J1113449", "name": "北不选狙"}],
  "repository": {
    "id": 11148460,
    "path": "mobile-org-probe",
    "namespace": {"path": "donk945", "avatar_url": "https://example.com/repo.png"},
    "assigner": {},
    "paas": ""
  },
  "labels": [{"name": "enhancement", "color": "1d76db"}],
  "issue_state": "待办的",
  "finished_at": "",
  "issue_state_detail": {"title": "待办的", "serial": 0}
}
"""

private const val MEASURED_COMMENTS_JSON = """
[{
  "id": 203147029,
  "body": "第二条评论：同 Issue 多条通知应合并成一行并显示角标。",
  "user": {"id": "6a7760a0533376275c1e1ccd", "login": "J1113449", "name": "北不选狙"},
  "created_at": "2026-11-23T20:50:39+08:00"
}]
"""
