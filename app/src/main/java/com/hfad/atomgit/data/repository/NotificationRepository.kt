package com.hfad.atomgit.data.repository

import com.hfad.atomgit.data.model.NotificationItem
import com.hfad.atomgit.data.remote.api.AtomGitV5Service
import com.hfad.atomgit.data.remote.error.ApiError
import com.hfad.atomgit.data.remote.error.ApiResult
import com.hfad.atomgit.data.remote.error.apiCall
import com.hfad.atomgit.data.remote.error.apiWriteCall
import com.hfad.atomgit.data.remote.error.requireBody
import com.hfad.atomgit.data.repository.mapping.toNotificationItem
import com.hfad.atomgit.data.session.CurrentAccountSource
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

interface NotificationRepository {
    suspend fun listRepo(
        owner: String,
        repo: String,
        unread: Boolean,
        page: Int,
        perPage: Int,
    ): ApiResult<List<NotificationItem>>

    /**
     * 只提交给定 id。空列表不发请求：空 ids 会把该仓库当前列表整批标已读。
     */
    suspend fun markRead(
        owner: String,
        repo: String,
        ids: List<String>,
    ): ApiResult<Unit>
}

@Singleton
class DefaultNotificationRepository @Inject constructor(
    private val api: AtomGitV5Service,
    private val accountSource: CurrentAccountSource,
) : NotificationRepository {

    override suspend fun listRepo(
        owner: String,
        repo: String,
        unread: Boolean,
        page: Int,
        perPage: Int,
    ): ApiResult<List<NotificationItem>> {
        if (accountSource.get() == null) return ApiResult.Failure(ApiError.Unauthorized)
        return apiCall {
            api.listRepoNotifications(
                owner = owner,
                repo = repo,
                unread = unread,
                page = page,
                perPage = perPage,
            ).requireBody().list.mapNotNull { it.toNotificationItem(owner, repo) }
        }
    }

    override suspend fun markRead(
        owner: String,
        repo: String,
        ids: List<String>,
    ): ApiResult<Unit> {
        if (accountSource.get() == null) return ApiResult.Failure(ApiError.Unauthorized)
        val joined = ids.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (joined.isEmpty()) {
            return ApiResult.Failure(ApiError.Unexpected(message = "ids required"))
        }
        return apiWriteCall {
            val response = api.markRepoNotificationsRead(
                owner = owner,
                repo = repo,
                ids = joined.joinToString(","),
            )
            if (!response.isSuccessful) throw HttpException(response)
        }
    }
}
