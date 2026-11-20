package com.hfad.atomgit.data.repository

import com.hfad.atomgit.data.model.PagedItems
import com.hfad.atomgit.data.model.RepoSort
import com.hfad.atomgit.data.model.RepoSummary
import com.hfad.atomgit.data.model.RepoVisibility
import com.hfad.atomgit.data.remote.api.AtomGitV5Service
import com.hfad.atomgit.data.remote.error.ApiResult
import com.hfad.atomgit.data.remote.error.apiCall
import com.hfad.atomgit.data.remote.error.getOrNull
import com.hfad.atomgit.data.remote.error.requireBody
import com.hfad.atomgit.data.remote.totalCountHeader
import com.hfad.atomgit.data.remote.totalPageHeader
import com.hfad.atomgit.data.repository.mapping.toRepoSummary
import javax.inject.Inject
import javax.inject.Singleton

/** 我的项目，搜索和筛选 */
interface RepoRepository {
    suspend fun listMine(
        query: String,
        visibility: RepoVisibility,
        sort: RepoSort,
        page: Int,
        perPage: Int,
    ): ApiResult<PagedItems<RepoSummary>>

    suspend fun countMine(): Long?
}

@Singleton
class DefaultRepoRepository @Inject constructor(
    private val api: AtomGitV5Service,
) : RepoRepository {

    override suspend fun listMine(
        query: String,
        visibility: RepoVisibility,
        sort: RepoSort,
        page: Int,
        perPage: Int,
    ): ApiResult<PagedItems<RepoSummary>> = apiCall {
        val response = api.listMyRepos(
            q = query.ifBlank { null },
            visibility = visibility.param,
            sort = sort.param,
            direction = sort.direction,
            page = page,
            perPage = perPage,
        )
        PagedItems(
            items = response.requireBody().map { it.toRepoSummary() },
            totalCount = response.totalCountHeader(),
            totalPage = response.totalPageHeader(),
        )
    }

    override suspend fun countMine(): Long? =
        apiCall { api.listMyRepos(page = 1, perPage = 1).totalCountHeader() }.getOrNull()
}
