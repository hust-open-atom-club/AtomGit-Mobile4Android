package com.hfad.atomgit.data.repository

import com.hfad.atomgit.data.model.RepoSort
import com.hfad.atomgit.data.model.RepoVisibility
import com.hfad.atomgit.data.remote.RemoteTestFixture
import com.hfad.atomgit.data.remote.error.ApiError
import com.hfad.atomgit.data.remote.error.ApiResult
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RepoRepositoryTest {

    private lateinit var fixture: RemoteTestFixture
    private lateinit var repository: RepoRepository

    @Before
    fun setUp() {
        fixture = RemoteTestFixture()
        fixture.tokenProvider.setToken("test-pat")
        repository = DefaultRepoRepository(api = fixture.v5Service())
    }

    @After
    fun tearDown() {
        fixture.close()
    }

    @Test
    fun listMine_parsesPathOwnerAndFlags() = runTest {
        fixture.server.enqueue(MockResponse().setResponseCode(200).setBody(REPO_JSON))

        val result = repository.listMine(
            query = "",
            visibility = RepoVisibility.All,
            sort = RepoSort.UpdatedDesc,
            page = 1,
            perPage = 30,
        )

        val page = (result as ApiResult.Success).value
        val repo = page.items.single()
        assertEquals("J1113449", repo.ownerPath)
        assertEquals("mobile-api-probe", repo.path)
        assertEquals("mobile-api-probe", repo.name)
        assertEquals("fixture", repo.description)
        assertEquals(3, repo.stars)
        assertTrue(repo.isPrivate)
    }

    @Test
    fun listMine_blankQueryIsOmitted() = runTest {
        fixture.server.enqueue(MockResponse().setResponseCode(200).setBody("[]"))

        repository.listMine("  ", RepoVisibility.All, RepoSort.UpdatedDesc, 1, 30)

        val recorded = fixture.server.takeRequest()
        assertNull(recorded.requestUrl?.queryParameter("q"))
    }

    @Test
    fun listMine_mapsVisibilityAndSortDirection() = runTest {
        fixture.server.enqueue(MockResponse().setResponseCode(200).setBody("[]"))

        repository.listMine("probe", RepoVisibility.Private, RepoSort.NameAsc, 2, 50)

        val url = fixture.server.takeRequest().requestUrl!!
        assertEquals("/api/v5/user/repos", url.encodedPath)
        assertEquals("probe", url.queryParameter("q"))
        assertEquals("private", url.queryParameter("visibility"))
        assertEquals("full_name", url.queryParameter("sort"))
        assertEquals("asc", url.queryParameter("direction"))
        assertEquals("2", url.queryParameter("page"))
        assertEquals("50", url.queryParameter("per_page"))
    }

    @Test
    fun listMine_readsTotalHeaders() = runTest {
        fixture.server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("total_count", "12")
                .setHeader("total_page", "2")
                .setBody("[]"),
        )

        val page = (repository.listMine("", RepoVisibility.All, RepoSort.UpdatedDesc, 1, 30)
            as ApiResult.Success).value

        assertEquals(12L, page.totalCount)
        assertEquals(2, page.totalPage)
    }

    @Test
    fun listMine_forbiddenIsFailureNotEmptyPage() = runTest {
        fixture.server.enqueue(MockResponse().setResponseCode(403).setBody("""{"error_msg":"no"}"""))

        val result = repository.listMine("", RepoVisibility.All, RepoSort.UpdatedDesc, 1, 30)

        assertEquals(ApiResult.Failure(ApiError.Forbidden), result)
    }

    @Test
    fun countMine_readsTotalCountHeader() = runTest {
        fixture.server.enqueue(
            MockResponse().setResponseCode(200).setHeader("total_count", "7").setBody("[]"),
        )

        assertEquals(7L, repository.countMine())

        val url = fixture.server.takeRequest().requestUrl!!
        assertEquals("1", url.queryParameter("per_page"))
        assertEquals("1", url.queryParameter("page"))
    }

    @Test
    fun countMine_missingHeaderIsNull() = runTest {
        fixture.server.enqueue(MockResponse().setResponseCode(200).setBody("[]"))

        assertNull(repository.countMine())
    }

    @Test
    fun countMine_failureIsNull() = runTest {
        fixture.server.enqueue(MockResponse().setResponseCode(401).setBody("""{"error_msg":"no"}"""))

        assertNull(repository.countMine())
    }

    private companion object {
        const val REPO_JSON = """
            [{
              "id": 1,
              "name": "mobile-api-probe",
              "full_name": "J1113449/mobile-api-probe",
              "path": "mobile-api-probe",
              "description": "fixture",
              "private": true,
              "stargazers_count": 3,
              "updated_at": "2026-10-17T19:56:09.729+08:00",
              "namespace": { "path": "J1113449", "name": "北不选狙" }
            }]
        """
    }
}
