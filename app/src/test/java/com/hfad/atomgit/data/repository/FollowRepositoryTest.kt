package com.hfad.atomgit.data.repository

import com.hfad.atomgit.data.model.FollowKind
import com.hfad.atomgit.data.remote.RemoteTestFixture
import com.hfad.atomgit.data.remote.error.ApiError
import com.hfad.atomgit.data.remote.error.ApiResult
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FollowRepositoryTest {

    private lateinit var fixture: RemoteTestFixture
    private lateinit var repository: FollowRepository

    @Before
    fun setUp() {
        fixture = RemoteTestFixture()
        fixture.tokenProvider.setToken("test-pat")
        repository = DefaultFollowRepository(api = fixture.v5Service())
    }

    @After
    fun tearDown() {
        fixture.close()
    }

    @Test
    fun list_followersHitsFollowersPathAndParsesHeaders() = runTest {
        fixture.server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("total_count", "1")
                .setBody(
                    """
                    [{
                      "followers_url": "https://api.atomgit.com/api/v5/users/potato-security/followers",
                      "html_url": "https://atomgit.com/potato-security",
                      "id": "6720b0463ed0887ef5d9918e",
                      "object_id": "6720b0463ed0887ef5d9918e",
                      "login": "potato-security",
                      "name": "土豆.exe",
                      "type": "User",
                      "url": "https://api.atomgit.com/api/v5/users/potato-security"
                    }]
                    """.trimIndent(),
                ),
        )

        val result = repository.list("J1113449", FollowKind.Followers, page = 2, perPage = 30)

        val page = (result as ApiResult.Success).value
        val item = page.items.single()
        assertEquals("potato-security", item.login)
        assertEquals("土豆.exe", item.name)
        assertFalse(item.isOrganization)
        assertEquals(1L, page.totalCount)
        // 只给了 total_count，没给 total_page：缺失必须是 null，不能变成 0
        assertNull(page.totalPage)

        val recorded = fixture.server.takeRequest()
        assertEquals("/api/v5/users/J1113449/followers", recorded.requestUrl?.encodedPath)
        assertEquals("2", recorded.requestUrl?.queryParameter("page"))
        assertEquals("30", recorded.requestUrl?.queryParameter("per_page"))
    }

    @Test
    fun list_followingHitsFollowingPath() = runTest {
        fixture.server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    [{
                      "login": "potato-security",
                      "following_url": "https://api.atomgit.com/api/v5/users/potato-security/following",
                      "type": "User"
                    }]
                    """.trimIndent(),
                ),
        )

        val result = repository.list("J1113449", FollowKind.Following, page = 1, perPage = 30)

        assertTrue(result is ApiResult.Success)
        val recorded = fixture.server.takeRequest()
        assertEquals("/api/v5/users/J1113449/following", recorded.requestUrl?.encodedPath)
    }

    @Test
    fun list_organizationItemIsMarkedAsOrganization() = runTest {
        fixture.server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""[{"login":"some-org","name":"组织","type":"Organization"}]"""),
        )

        val result = repository.list("J1113449", FollowKind.Followers, page = 1, perPage = 30)

        val page = (result as ApiResult.Success).value
        assertTrue(page.items.single().isOrganization)
    }

    @Test
    fun list_missingTotalHeaders_keepsThemNull() = runTest {
        fixture.server.enqueue(MockResponse().setResponseCode(200).setBody("[]"))

        val result = repository.list("J1113449", FollowKind.Followers, page = 1, perPage = 30)

        val page = (result as ApiResult.Success).value
        assertTrue(page.items.isEmpty())
        assertNull(page.totalCount)
        assertNull(page.totalPage)
    }

    /** 404 不能伪装成空列表，否则界面会显示「暂无粉丝」。 */
    @Test
    fun list_notFound_isFailureNotFakeEmptyPage() = runTest {
        fixture.server.enqueue(
            MockResponse()
                .setResponseCode(404)
                .setBody(
                    """{"error_code":1003,"error_code_name":"NOT_EXIST","error_message":"The user does not exist."}""",
                ),
        )

        val result = repository.list("zzz-not-exist", FollowKind.Followers, page = 1, perPage = 30)

        assertEquals(ApiResult.Failure(ApiError.NotFound), result)
    }
}
