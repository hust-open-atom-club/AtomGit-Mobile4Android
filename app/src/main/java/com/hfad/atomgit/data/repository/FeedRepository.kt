package com.hfad.atomgit.data.repository

import com.hfad.atomgit.data.model.RepoFeedPage
import com.hfad.atomgit.data.model.UserFeedPage
import com.hfad.atomgit.data.remote.api.AtomGitV5Service
import com.hfad.atomgit.data.remote.error.ApiError
import com.hfad.atomgit.data.remote.error.ApiResult
import com.hfad.atomgit.data.remote.error.apiCall
import com.hfad.atomgit.data.repository.mapping.flattenUserEvents
import com.hfad.atomgit.data.repository.mapping.toFeedEvent
import com.hfad.atomgit.data.session.CurrentAccountSource
import javax.inject.Inject
import javax.inject.Singleton

/** 动态数据源。两个来源分别分页；无账号时返回 [ApiError.Unauthorized]。 */
interface FeedRepository {
    /** 本人动态。 */
    suspend fun listMyEvents(next: String? = null): ApiResult<UserFeedPage>

    /** 选定项目的动态。 */
    suspend fun listRepoEvents(
        owner: String,
        repo: String,
        page: Int,
        perPage: Int,
    ): ApiResult<RepoFeedPage>
}

@Singleton
class DefaultFeedRepository @Inject constructor(
    private val api: AtomGitV5Service,
    private val accountSource: CurrentAccountSource,
) : FeedRepository {

    override suspend fun listMyEvents(next: String?): ApiResult<UserFeedPage> {
        val account = accountSource.get() ?: return ApiResult.Failure(ApiError.Unauthorized)
        return apiCall {
            val response = api.listUserEvents(username = account.login, next = next)
            UserFeedPage(
                items = response.events.flattenUserEvents(account.id),
                next = response.next,
            )
        }
    }

    override suspend fun listRepoEvents(
        owner: String,
        repo: String,
        page: Int,
        perPage: Int,
    ): ApiResult<RepoFeedPage> {
        val account = accountSource.get() ?: return ApiResult.Failure(ApiError.Unauthorized)
        return apiCall {
            val response = api.listRepoEvents(
                owner = owner,
                repo = repo,
                page = page,
                perPage = perPage,
            )
            RepoFeedPage(
                items = response.events.map { it.toFeedEvent(account.id) },
                hasNextPage = response.hasNextPage != 0,
            )
        }
    }
}
