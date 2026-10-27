package com.hfad.atomgit.data.remote.api

import com.hfad.atomgit.data.remote.dto.WorkflowRunsResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * AtomGit OpenAPI v8（流水线）。路径打到 v5 会 404，必须走这套 Retrofit。
 */
interface AtomGitV8Service {

    /**
     * 不提供 `pull_request_id` 查询：实测运行对象没有 PR 编号。
     * 与 PR 对齐比较 `head_sha` / `head_branch`；对上 base 的运行不能当成该 PR 的运行。
     */
    @GET("repos/{owner}/{repo}/actions/runs")
    suspend fun listWorkflowRuns(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("page") page: Int,
        @Query("per_page") perPage: Int,
        @Query("status") status: String? = null,
        @Query("event") event: String? = null,
        @Query("branch") branch: String? = null,
    ): WorkflowRunsResponseDto
}
