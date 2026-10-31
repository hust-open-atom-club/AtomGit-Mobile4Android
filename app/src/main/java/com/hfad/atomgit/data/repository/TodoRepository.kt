package com.hfad.atomgit.data.repository

import com.hfad.atomgit.data.model.TodoPage
import com.hfad.atomgit.data.remote.api.AtomGitV5Service
import com.hfad.atomgit.data.remote.api.IssueState
import com.hfad.atomgit.data.remote.api.PullState
import com.hfad.atomgit.data.remote.api.UserIssueFilter
import com.hfad.atomgit.data.remote.api.UserPullScope
import com.hfad.atomgit.data.remote.totalCountHeader
import com.hfad.atomgit.data.remote.totalPageHeader
import com.hfad.atomgit.data.repository.mapping.toTodoItem
import com.hfad.atomgit.data.session.CurrentAccountSource
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 待办分源查询，各自分页；不把各分类第一页拼成「全部待办」。
 */
interface TodoRepository {
    /** 待处理：分配给我的未完成 Issue。 */
    suspend fun listAssignedOpenIssues(page: Int, perPage: Int): TodoPage

    /** 跟进：我创建的 Issue；[state] 由调用方传入。 */
    suspend fun listCreatedIssues(state: IssueState, page: Int, perPage: Int): TodoPage

    /** 跟进：我发起的 PR。 */
    suspend fun listCreatedByMePulls(state: PullState, page: Int, perPage: Int): TodoPage

    /** 待处理：待我批准的 PR。 */
    suspend fun listNeedMyApprovePulls(state: PullState, page: Int, perPage: Int): TodoPage

    /** 待处理：待我评审的 PR。 */
    suspend fun listNeedMyReviewPulls(state: PullState, page: Int, perPage: Int): TodoPage
}

@Singleton
class DefaultTodoRepository @Inject constructor(
    private val api: AtomGitV5Service,
    private val accountSource: CurrentAccountSource,
) : TodoRepository {

    override suspend fun listAssignedOpenIssues(page: Int, perPage: Int): TodoPage {
        val account = accountSource.require()
        val response = api.listUserIssues(
            filter = UserIssueFilter.Assigned,
            state = IssueState.Open,
            page = page,
            perPage = perPage,
        )
        return response.toTodoPage { it.toTodoItem(account.id) }
    }

    override suspend fun listCreatedIssues(
        state: IssueState,
        page: Int,
        perPage: Int,
    ): TodoPage {
        val account = accountSource.require()
        val response = api.listUserIssues(
            filter = UserIssueFilter.Created,
            state = state,
            page = page,
            perPage = perPage,
        )
        return response.toTodoPage { it.toTodoItem(account.id) }
    }

    override suspend fun listCreatedByMePulls(
        state: PullState,
        page: Int,
        perPage: Int,
    ): TodoPage = listPulls(UserPullScope.CreatedByMe, state, page, perPage)

    override suspend fun listNeedMyApprovePulls(
        state: PullState,
        page: Int,
        perPage: Int,
    ): TodoPage = listPulls(UserPullScope.NeedMyApprove, state, page, perPage)

    override suspend fun listNeedMyReviewPulls(
        state: PullState,
        page: Int,
        perPage: Int,
    ): TodoPage = listPulls(UserPullScope.NeedMyReview, state, page, perPage)

    private suspend fun listPulls(
        scope: UserPullScope,
        state: PullState,
        page: Int,
        perPage: Int,
    ): TodoPage {
        val account = accountSource.require()
        val response = api.listUserPulls(
            scope = scope,
            state = state,
            page = page,
            perPage = perPage,
        )
        return response.toTodoPage { it.toTodoItem(account.id) }
    }
}

private fun <T> retrofit2.Response<List<T>>.toTodoPage(
    mapItem: (T) -> com.hfad.atomgit.data.model.TodoItem,
): TodoPage {
    val body = body().orEmpty()
    return TodoPage(
        items = body.map(mapItem),
        totalCount = totalCountHeader(),
        totalPage = totalPageHeader(),
    )
}
