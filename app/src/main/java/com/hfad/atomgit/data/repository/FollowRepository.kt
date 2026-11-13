package com.hfad.atomgit.data.repository

import com.hfad.atomgit.data.model.FollowKind
import com.hfad.atomgit.data.model.PagedItems
import com.hfad.atomgit.data.model.UserSummary
import com.hfad.atomgit.data.remote.api.AtomGitV5Service
import com.hfad.atomgit.data.remote.error.ApiResult
import com.hfad.atomgit.data.remote.error.apiCall
import com.hfad.atomgit.data.remote.error.requireBody
import com.hfad.atomgit.data.remote.totalCountHeader
import com.hfad.atomgit.data.remote.totalPageHeader
import com.hfad.atomgit.data.repository.mapping.toUserSummary
import javax.inject.Inject
import javax.inject.Singleton

/** 只读列表，这两个端点不需要 token。 */
interface FollowRepository {
    suspend fun list(
        login: String,
        kind: FollowKind,
        page: Int,
        perPage: Int,
    ): ApiResult<PagedItems<UserSummary>>
}

@Singleton
class DefaultFollowRepository @Inject constructor(
    private val api: AtomGitV5Service,
) : FollowRepository {

    override suspend fun list(
        login: String,
        kind: FollowKind,
        page: Int,
        perPage: Int,
    ): ApiResult<PagedItems<UserSummary>> = apiCall {
        val response = when (kind) {
            FollowKind.Followers -> api.listUserFollowers(login, page, perPage)
            FollowKind.Following -> api.listUserFollowing(login, page, perPage)
        }
        PagedItems(
            items = response.requireBody().map { it.toUserSummary() },
            totalCount = response.totalCountHeader(),
            totalPage = response.totalPageHeader(),
        )
    }
}
