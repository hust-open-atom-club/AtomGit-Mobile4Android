package com.hfad.atomgit.data.repository

import com.hfad.atomgit.data.model.RepoFeedPage
import com.hfad.atomgit.data.model.UserFeedPage
import com.hfad.atomgit.data.remote.api.AtomGitV5Service
import com.hfad.atomgit.data.repository.mapping.flattenUserEvents
import com.hfad.atomgit.data.repository.mapping.toFeedEvent
import com.hfad.atomgit.data.session.CurrentAccountSource
import javax.inject.Inject
import javax.inject.Singleton

/** 动态数据源：本人事件与选定项目事件分开分页，不拼成一条时间线。 */
interface FeedRepository {
    /**
     * 本人事件。翻页只传 [next]；首页传 null。
     * 无当前账号时不发请求。
     */
    suspend fun listMyEvents(next: String? = null): UserFeedPage

    /**
     * 选定项目事件。[page] / [perPage] 由调用方传入，无全局默认页大小。
     */
    suspend fun listRepoEvents(
        owner: String,
        repo: String,
        page: Int,
        perPage: Int,
    ): RepoFeedPage
}

@Singleton
class DefaultFeedRepository @Inject constructor(
    private val api: AtomGitV5Service,
    private val accountSource: CurrentAccountSource,
) : FeedRepository {

    override suspend fun listMyEvents(next: String?): UserFeedPage {
        val account = accountSource.require()
        val response = api.listUserEvents(username = account.login, next = next)
        return UserFeedPage(
            items = response.events.flattenUserEvents(account.id),
            next = response.next,
        )
    }

    override suspend fun listRepoEvents(
        owner: String,
        repo: String,
        page: Int,
        perPage: Int,
    ): RepoFeedPage {
        val account = accountSource.require()
        val response = api.listRepoEvents(
            owner = owner,
            repo = repo,
            page = page,
            perPage = perPage,
        )
        return RepoFeedPage(
            items = response.events.map { it.toFeedEvent(account.id) },
            hasNextPage = response.hasNextPage != 0,
        )
    }
}
