package com.hfad.atomgit.data.remote.api

import com.hfad.atomgit.data.remote.RemoteTestFixture
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

class AtomGitV8ServiceContractTest {

    private lateinit var fixture: RemoteTestFixture
    private lateinit var api: AtomGitV8Service

    @Before
    fun setUp() {
        fixture = RemoteTestFixture()
        fixture.tokenProvider.setToken("test-pat")
        api = fixture.v8Service()
    }

    @After
    fun tearDown() {
        fixture.close()
    }

    @Test
    fun listWorkflowRuns_hitsV8PathNotV5() = runTest {
        fixture.server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "total_count": 1,
                      "workflow_runs": [
                        {
                          "workflow_run_id": "1001",
                          "status": "COMPLETED",
                          "event": "Manual",
                          "head_branch": "probe-pr",
                          "head_sha": "abc123",
                          "extra_ignored": true
                        }
                      ]
                    }
                    """.trimIndent(),
                ),
        )

        val body = api.listWorkflowRuns(
            owner = "J1113449",
            repo = "mobile-api-probe-20261017",
            page = 1,
            perPage = 20,
        )

        assertEquals(1, body.totalCount)
        assertEquals("1001", body.workflowRuns.single().workflowRunId)
        assertEquals("probe-pr", body.workflowRuns.single().headBranch)
        assertEquals("abc123", body.workflowRuns.single().headSha)

        val recorded = fixture.server.takeRequest()
        assertEquals("GET", recorded.method)
        assertEquals(
            "/api/v8/repos/J1113449/mobile-api-probe-20261017/actions/runs?page=1&per_page=20",
            recorded.path,
        )
        assertFalse(recorded.path!!.startsWith("/api/v5/"))
        assertFalse(recorded.path!!.contains("pull_request_id"))
    }
}
