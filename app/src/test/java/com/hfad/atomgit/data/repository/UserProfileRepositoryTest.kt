package com.hfad.atomgit.data.repository

import com.hfad.atomgit.data.remote.RemoteTestFixture
import com.hfad.atomgit.data.remote.error.ApiError
import com.hfad.atomgit.data.remote.error.ApiResult
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class UserProfileRepositoryTest {

    private lateinit var fixture: RemoteTestFixture
    private lateinit var repository: UserProfileRepository

    @Before
    fun setUp() {
        fixture = RemoteTestFixture()
        fixture.tokenProvider.setToken("test-pat")
        repository = DefaultUserProfileRepository(api = fixture.v5Service())
    }

    @After
    fun tearDown() {
        fixture.close()
    }

    @Test
    fun get_hitsUsersPathAndDropsBlankText() = runTest {
        fixture.server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "id": "6720b0463ed0887ef5d9918e",
                      "login": "potato-security",
                      "name": "土豆.exe",
                      "bio": "",
                      "company": "",
                      "location": "武汉",
                      "followers": 1,
                      "following": 0,
                      "top_languages": ["Java", "CSS"]
                    }
                    """.trimIndent(),
                ),
        )

        val result = repository.get("potato-security")

        val profile = (result as ApiResult.Success).value
        assertEquals("土豆.exe", profile.name)
        assertNull(profile.bio)
        assertEquals("武汉", profile.location)
        assertEquals(listOf("Java", "CSS"), profile.topLanguages)

        val recorded = fixture.server.takeRequest()
        assertEquals("/api/v5/users/potato-security", recorded.requestUrl?.encodedPath)
    }

    @Test
    fun get_fallsBackToDescriptionAndBlog() = runTest {
        fixture.server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "id": "1",
                      "login": "someone",
                      "bio": "",
                      "description": "只有 description",
                      "website": "",
                      "blog": "https://example.com"
                    }
                    """.trimIndent(),
                ),
        )

        val profile = (repository.get("someone") as ApiResult.Success).value

        assertEquals("只有 description", profile.bio)
        assertEquals("https://example.com", profile.website)
    }

    @Test
    fun counts_readsTotalCountHeaders() = runTest {
        fixture.server.enqueue(MockResponse().setResponseCode(200).setHeader("total_count", "7").setBody("[]"))
        fixture.server.enqueue(MockResponse().setResponseCode(200).setHeader("total_count", "0").setBody("[]"))
        fixture.server.enqueue(MockResponse().setResponseCode(200).setHeader("total_count", "23").setBody("[]"))

        val counts = repository.counts("potato-security")

        assertEquals(7L, counts.repos)
        assertEquals(0L, counts.orgs)
        assertEquals(23L, counts.starred)
    }

    @Test
    fun counts_missingHeadersAreNull() = runTest {
        repeat(3) {
            fixture.server.enqueue(MockResponse().setResponseCode(200).setBody("[]"))
        }

        val counts = repository.counts("potato-security")

        assertNull(counts.repos)
        assertNull(counts.orgs)
        assertNull(counts.starred)
    }

    @Test
    fun counts_failureOfOneListDoesNotFailOthers() = runTest {
        fixture.server.enqueue(MockResponse().setResponseCode(404).setBody("""{"error_msg":"404 Route Not Found"}"""))
        fixture.server.enqueue(MockResponse().setResponseCode(200).setHeader("total_count", "2").setBody("[]"))
        fixture.server.enqueue(MockResponse().setResponseCode(200).setHeader("total_count", "5").setBody("[]"))

        val counts = repository.counts("potato-security")

        assertNull(counts.repos)
        assertEquals(2L, counts.orgs)
        assertEquals(5L, counts.starred)
    }

    @Test
    fun counts_usesPerPageOneToKeepResponseSmall() = runTest {
        repeat(3) {
            fixture.server.enqueue(MockResponse().setResponseCode(200).setBody("[]"))
        }

        repository.counts("potato-security")

        val paths = List(3) { fixture.server.takeRequest().requestUrl!! }
        assertEquals("/api/v5/users/potato-security/repos", paths[0].encodedPath)
        assertEquals("/api/v5/users/potato-security/orgs", paths[1].encodedPath)
        assertEquals("/api/v5/users/potato-security/starred", paths[2].encodedPath)
        paths.forEach { url ->
            assertEquals("1", url.queryParameter("per_page"))
            assertEquals("1", url.queryParameter("page"))
        }
    }
}
