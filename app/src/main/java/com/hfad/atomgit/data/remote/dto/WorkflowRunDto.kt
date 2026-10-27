package com.hfad.atomgit.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WorkflowRunsResponseDto(
    @SerialName("total_count") val totalCount: Int = 0,
    @SerialName("workflow_runs") val workflowRuns: List<WorkflowRunDto> = emptyList(),
)

/**
 * 运行对象没有 PR 编号。与 PR 对齐要比较 [headSha]、[headBranch] 和 PR 的 head；
 * 对上 base 的运行不能当成该 PR 的运行。
 */
@Serializable
data class WorkflowRunDto(
    @SerialName("workflow_run_id") val workflowRunId: String,
    @SerialName("workflow_id") val workflowId: String? = null,
    @SerialName("workflow_name") val workflowName: String? = null,
    @SerialName("file_path") val filePath: String? = null,
    val title: String? = null,
    val status: String? = null,
    val event: String? = null,
    @SerialName("run_number") val runNumber: Int? = null,
    @SerialName("head_branch") val headBranch: String? = null,
    @SerialName("head_sha") val headSha: String? = null,
    val actor: WorkflowActorDto? = null,
    @SerialName("start_time") val startTime: Long? = null,
    @SerialName("end_time") val endTime: Long? = null,
)

@Serializable
data class WorkflowActorDto(
    val id: String? = null,
    val login: String? = null,
    val name: String? = null,
)
