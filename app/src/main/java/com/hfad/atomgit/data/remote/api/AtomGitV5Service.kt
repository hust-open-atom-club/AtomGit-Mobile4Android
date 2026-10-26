package com.hfad.atomgit.data.remote.api

import com.hfad.atomgit.data.remote.dto.ApprovePullRequestBody
import com.hfad.atomgit.data.remote.dto.BranchDto
import com.hfad.atomgit.data.remote.dto.CommentBody
import com.hfad.atomgit.data.remote.dto.CommitDto
import com.hfad.atomgit.data.remote.dto.ContentFileDto
import com.hfad.atomgit.data.remote.dto.ContentItemDto
import com.hfad.atomgit.data.remote.dto.GitTreeDto
import com.hfad.atomgit.data.remote.dto.IssueCommentDto
import com.hfad.atomgit.data.remote.dto.IssueDto
import com.hfad.atomgit.data.remote.dto.NotificationsResponseDto
import com.hfad.atomgit.data.remote.dto.OrgDto
import com.hfad.atomgit.data.remote.dto.OrgMembershipDto
import com.hfad.atomgit.data.remote.dto.PullCommentDto
import com.hfad.atomgit.data.remote.dto.PullFileDto
import com.hfad.atomgit.data.remote.dto.PullFilesJsonDto
import com.hfad.atomgit.data.remote.dto.PullRequestDto
import com.hfad.atomgit.data.remote.dto.RepoDto
import com.hfad.atomgit.data.remote.dto.RepoEventsResponseDto
import com.hfad.atomgit.data.remote.dto.TagDto
import com.hfad.atomgit.data.remote.dto.UserDto
import com.hfad.atomgit.data.remote.dto.UserEventsResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * AtomGit OpenAPI v5。分页参数无全局默认值，由调用方按端点传入。
 * 返回 [Response] 的方法是为了读取 `total_count` / `total_page` 响应头。
 */
interface AtomGitV5Service {

    // region 会话

    @GET("user")
    suspend fun getAuthenticatedUser(): UserDto

    // endregion

    // region 待办

    /**
     * 各 filter/state 单独分页。终止条件是空数组；总数在响应头，缺失时不能当成 0。
     */
    @GET("user/issues")
    suspend fun listUserIssues(
        @Query("filter") filter: UserIssueFilter,
        @Query("state") state: IssueState,
        @Query("page") page: Int,
        @Query("per_page") perPage: Int,
    ): Response<List<IssueDto>>

    /**
     * 各 scope 单独分页。批准后 `need_my_approve` / `need_my_review` 仍可能含该 PR，
     * 待处理还要看审查人 `accept` / 评审人 `accept_status`。
     */
    @GET("user/pulls")
    suspend fun listUserPulls(
        @Query("scope") scope: UserPullScope,
        @Query("state") state: PullState,
        @Query("page") page: Int,
        @Query("per_page") perPage: Int,
    ): Response<List<PullRequestDto>>

    // endregion

    // region Issue

    @GET("repos/{owner}/{repo}/issues")
    suspend fun listRepoIssues(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("state") state: IssueState,
        @Query("page") page: Int,
        @Query("per_page") perPage: Int,
    ): Response<List<IssueDto>>

    @GET("repos/{owner}/{repo}/issues/{number}")
    suspend fun getIssue(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("number") number: Int,
    ): IssueDto

    @GET("repos/{owner}/{repo}/issues/{number}/comments")
    suspend fun listIssueComments(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("number") number: Int,
        @Query("page") page: Int,
        @Query("per_page") perPage: Int,
    ): List<IssueCommentDto>

    /** 评论 2xx 只表示评论已发表，不改变 Issue 完成分类。 */
    @POST("repos/{owner}/{repo}/issues/{number}/comments")
    suspend fun createIssueComment(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("number") number: Int,
        @Body body: CommentBody,
    ): IssueCommentDto

    // endregion

    // region Pull Request

    @GET("repos/{owner}/{repo}/pulls")
    suspend fun listRepoPulls(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("state") state: PullState,
        @Query("page") page: Int,
        @Query("per_page") perPage: Int,
    ): Response<List<PullRequestDto>>

    @GET("repos/{owner}/{repo}/pulls/{number}")
    suspend fun getPullRequest(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("number") number: Int,
    ): PullRequestDto

    /** 数组形态；文本 diff 在 `patch.diff`，另含 too_large / new_file / deleted_file / renamed_file。 */
    @GET("repos/{owner}/{repo}/pulls/{number}/files")
    suspend fun listPullFiles(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("number") number: Int,
    ): List<PullFileDto>

    /** 对象形态；用 `diffs[].statistic.type` 区分 text_type / binary_type。 */
    @GET("repos/{owner}/{repo}/pulls/{number}/files.json")
    suspend fun getPullFilesJson(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("number") number: Int,
    ): PullFilesJsonDto

    /** 只传 `body` 为普通 `pr_comment`；带 path/position/commit_id 会变成行内评论（扩展）。 */
    @POST("repos/{owner}/{repo}/pulls/{number}/comments")
    suspend fun createPullComment(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("number") number: Int,
        @Body body: CommentBody,
    ): PullCommentDto

    /**
     * 批准审查人。实测 204 无响应体；须回读 PR 看 `accept` / `accept_status`。
     * 2xx 不表示已合并，也不表示评审人已通过。
     */
    @POST("repos/{owner}/{repo}/pulls/{number}/review")
    suspend fun approvePullRequest(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("number") number: Int,
        @Body body: ApprovePullRequestBody = ApprovePullRequestBody.approve(),
    ): Response<Unit>

    // endregion

    // region 动态

    /**
     * 翻页只用 [next] 游标。不暴露 limit：实测 `limit=2` 仍返回约 20 条。
     */
    @GET("users/{username}/events")
    suspend fun listUserEvents(
        @Path("username") username: String,
        @Query("next") next: String? = null,
    ): UserEventsResponseDto

    @GET("repos/{owner}/{repo}/events")
    suspend fun listRepoEvents(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("page") page: Int,
        @Query("per_page") perPage: Int,
    ): RepoEventsResponseDto

    // endregion

    // region 通知

    /**
     * 默认列表像未读列表；标已读后须用 [unread]=false 回读，
     * 不能把「默认列表里没有」理解成记录被删除。
     */
    @GET("repos/{owner}/{repo}/notifications")
    suspend fun listRepoNotifications(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("unread") unread: Boolean,
        @Query("page") page: Int,
        @Query("per_page") perPage: Int,
    ): Response<NotificationsResponseDto>

    /**
     * [ids] 必填，逗号分隔。不提供无 ids 的重载：实测空对象会把该仓库当前列表整批标已读。
     */
    @PUT("repos/{owner}/{repo}/notifications")
    suspend fun markRepoNotificationsRead(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("ids") ids: String,
    ): Response<Unit>

    // endregion

    // region 项目与组织

    @GET("user/repos")
    suspend fun listUserRepos(
        @Query("page") page: Int,
        @Query("per_page") perPage: Int,
    ): Response<List<RepoDto>>

    @GET("repos/{owner}/{repo}")
    suspend fun getRepo(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
    ): RepoDto

    /** 当前用户所属组织；公开组织搜索结果不要放进「我的组织」。 */
    @GET("users/orgs")
    suspend fun listAuthenticatedUserOrgs(
        @Query("page") page: Int,
        @Query("per_page") perPage: Int,
    ): Response<List<OrgDto>>

    @GET("orgs/{org}")
    suspend fun getOrg(
        @Path("org") org: String,
    ): OrgDto

    @GET("orgs/{org}/repos")
    suspend fun listOrgRepos(
        @Path("org") org: String,
        @Query("page") page: Int,
        @Query("per_page") perPage: Int,
    ): Response<List<RepoDto>>

    @GET("user/memberships/orgs/{org}")
    suspend fun getOrgMembership(
        @Path("org") org: String,
    ): OrgMembershipDto

    // endregion

    // region 仓库内容

    @GET("repos/{owner}/{repo}/branches")
    suspend fun listBranches(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("page") page: Int,
        @Query("per_page") perPage: Int,
    ): Response<List<BranchDto>>

    /** 空列表实测也可能是 201，仍按成功解析。 */
    @GET("repos/{owner}/{repo}/tags")
    suspend fun listTags(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("page") page: Int,
        @Query("per_page") perPage: Int,
    ): Response<List<TagDto>>

    @GET("repos/{owner}/{repo}/commits")
    suspend fun listCommits(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("sha") sha: String? = null,
        @Query("page") page: Int,
        @Query("per_page") perPage: Int,
    ): Response<List<CommitDto>>

    /**
     * 单文件对象。路径由调用方编码斜杠（encoded=true）。
     * 不用 `/file_list`：错误 ref 仍可能返回 README，不能做文件树。
     */
    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun getContentFile(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path(value = "path", encoded = true) path: String,
        @Query("ref") ref: String,
    ): ContentFileDto

    /** 当前目录一层；与 [getContentFile] 同路径，响应形态不同。 */
    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun getContentDirectory(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path(value = "path", encoded = true) path: String,
        @Query("ref") ref: String,
    ): List<ContentItemDto>

    /**
     * 按层懒加载；[sha] 可以是分支名或 tree SHA。不默认 recursive。
     */
    @GET("repos/{owner}/{repo}/git/trees/{sha}")
    suspend fun getGitTree(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("sha") sha: String,
    ): GitTreeDto

    // endregion
}
