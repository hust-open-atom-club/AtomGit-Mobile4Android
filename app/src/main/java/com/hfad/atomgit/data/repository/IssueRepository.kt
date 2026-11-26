package com.hfad.atomgit.data.repository

import com.hfad.atomgit.data.model.IssueComment
import com.hfad.atomgit.data.model.IssueDetail
import com.hfad.atomgit.data.remote.api.AtomGitV5Service
import com.hfad.atomgit.data.remote.dto.CommentBody
import com.hfad.atomgit.data.remote.error.ApiError
import com.hfad.atomgit.data.remote.error.ApiResult
import com.hfad.atomgit.data.remote.error.apiCall
import com.hfad.atomgit.data.remote.error.apiWriteCall
import com.hfad.atomgit.data.repository.mapping.toIssueComment
import com.hfad.atomgit.data.repository.mapping.toIssueDetail
import com.hfad.atomgit.data.session.CurrentAccountSource
import javax.inject.Inject
import javax.inject.Singleton

interface IssueRepository {
    suspend fun get(owner: String, repo: String, number: Int): ApiResult<IssueDetail>

    suspend fun listComments(
        owner: String,
        repo: String,
        number: Int,
        page: Int,
        perPage: Int,
    ): ApiResult<List<IssueComment>>

    /**
     * 发表普通评论。2xx 只表示评论已写入，不表示 Issue 已关闭或待办分类已变化。
     * 空白正文不发请求。
     */
    suspend fun createComment(
        owner: String,
        repo: String,
        number: Int,
        body: String,
    ): ApiResult<IssueComment>
}

@Singleton
class DefaultIssueRepository @Inject constructor(
    private val api: AtomGitV5Service,
    private val accountSource: CurrentAccountSource,
) : IssueRepository {

    override suspend fun get(owner: String, repo: String, number: Int): ApiResult<IssueDetail> {
        if (accountSource.get() == null) return ApiResult.Failure(ApiError.Unauthorized)
        return apiCall { api.getIssue(owner, repo, number).toIssueDetail() }
    }

    override suspend fun listComments(
        owner: String,
        repo: String,
        number: Int,
        page: Int,
        perPage: Int,
    ): ApiResult<List<IssueComment>> {
        if (accountSource.get() == null) return ApiResult.Failure(ApiError.Unauthorized)
        return apiCall {
            api.listIssueComments(owner, repo, number, page, perPage).map { it.toIssueComment() }
        }
    }

    override suspend fun createComment(
        owner: String,
        repo: String,
        number: Int,
        body: String,
    ): ApiResult<IssueComment> {
        if (accountSource.get() == null) return ApiResult.Failure(ApiError.Unauthorized)
        val text = body.trim()
        if (text.isEmpty()) {
            return ApiResult.Failure(ApiError.Unexpected(message = "body required"))
        }
        return apiWriteCall {
            api.createIssueComment(owner, repo, number, CommentBody(text)).toIssueComment()
        }
    }
}
