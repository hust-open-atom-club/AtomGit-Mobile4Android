package com.hfad.atomgit.data.repository

import com.hfad.atomgit.data.model.CommitSummary
import com.hfad.atomgit.data.model.PagedItems
import com.hfad.atomgit.data.model.RepoDetail
import com.hfad.atomgit.data.model.RepoEntry
import com.hfad.atomgit.data.model.RepoEntryKind
import com.hfad.atomgit.data.model.RepoRef
import com.hfad.atomgit.data.remote.api.AtomGitV5Service
import com.hfad.atomgit.data.remote.error.ApiResult
import com.hfad.atomgit.data.remote.error.apiCall
import com.hfad.atomgit.data.remote.error.requireBody
import com.hfad.atomgit.data.remote.totalCountHeader
import com.hfad.atomgit.data.remote.totalPageHeader
import com.hfad.atomgit.data.repository.mapping.toCommitSummary
import com.hfad.atomgit.data.repository.mapping.toRepoDetail
import com.hfad.atomgit.data.repository.mapping.toRepoEntry
import com.hfad.atomgit.data.repository.mapping.toRepoRef
import javax.inject.Inject
import javax.inject.Singleton

interface RepoDetailRepository {
    suspend fun getDetail(owner: String, repo: String): ApiResult<RepoDetail>
    suspend fun getDirectory(
        owner: String,
        repo: String,
        path: String,
        ref: String
    ): ApiResult<List<RepoEntry>>

    suspend fun listCommits(
        owner: String,
        repo: String,
        ref: String,
        page: Int,
        perPage: Int
    ): ApiResult<PagedItems<CommitSummary>>

    suspend fun listBranches(
        owner: String,
        repo: String,
        page: Int,
        perPage: Int
    ): ApiResult<PagedItems<RepoRef>>

    suspend fun listTags(
        owner: String,
        repo: String,
        page: Int,
        perPage: Int
    ): ApiResult<PagedItems<RepoRef>>
}

@Singleton
class DefaultRepoDetailRepository @Inject constructor(
    private val api: AtomGitV5Service,
) : RepoDetailRepository {
    override suspend fun getDetail(owner: String, repo: String) = apiCall {
        api.getRepo(owner, repo).toRepoDetail(owner, repo)
    }

    override suspend fun getDirectory(owner: String, repo: String, path: String, ref: String) =
        apiCall {
            api.getContentDirectory(owner, repo, encodeContentPath(path), ref)
                .mapNotNull { it.toRepoEntry() }
                .sortedWith(compareBy<RepoEntry> { it.kind != RepoEntryKind.Directory }.thenBy { it.name.lowercase() })
        }

    override suspend fun listCommits(
        owner: String,
        repo: String,
        ref: String,
        page: Int,
        perPage: Int
    ) = apiCall {
        val response = api.listCommits(owner, repo, ref, page, perPage)
        PagedItems(
            response.requireBody().map { it.toCommitSummary() },
            response.totalCountHeader(),
            response.totalPageHeader()
        )
    }

    override suspend fun listBranches(owner: String, repo: String, page: Int, perPage: Int) =
        apiCall {
            val response = api.listBranches(owner, repo, page, perPage)
            PagedItems(
                response.requireBody().map { it.toRepoRef() },
                response.totalCountHeader(),
                response.totalPageHeader()
            )
        }

    override suspend fun listTags(owner: String, repo: String, page: Int, perPage: Int) = apiCall {
        val response = api.listTags(owner, repo, page, perPage)
        PagedItems(
            response.requireBody().mapNotNull { it.toRepoRef() },
            response.totalCountHeader(),
            response.totalPageHeader()
        )
    }
}
