package com.hfad.atomgit.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PullRequestDto(
    val id: Long? = null,
    val number: Int,
    val title: String? = null,
    val body: String? = null,
    /** 仍为 `open` 时不表示已合并；批准成功也不改变此项。 */
    val state: String? = null,
    @SerialName("html_url") val htmlUrl: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("merged_at") val mergedAt: String? = null,
    val mergeable: Boolean? = null,
    val user: ActorDto? = null,
    val head: PullRefDto? = null,
    val base: PullRefDto? = null,
    /** 审查人；批准后看条目的 [PullAssigneeDto.accept]。 */
    val assignees: List<PullAssigneeDto>? = null,
    /** 评审人；首版只能展示 [PullReviewerDto.acceptStatus]，不能标为已通过。 */
    @SerialName("approval_reviewers") val approvalReviewers: List<PullReviewerDto>? = null,
    val repository: RepoDto? = null,
)

@Serializable
data class PullRefDto(
    val label: String? = null,
    val ref: String? = null,
    val sha: String? = null,
)

@Serializable
data class PullAssigneeDto(
    val login: String? = null,
    val name: String? = null,
    val accept: Boolean? = null,
)

@Serializable
data class PullReviewerDto(
    val login: String? = null,
    val name: String? = null,
    val accept: Boolean? = null,
    @SerialName("accept_status") val acceptStatus: String? = null,
)

@Serializable
data class PullFileDto(
    val sha: String? = null,
    val filename: String? = null,
    val status: String? = null,
    val additions: Int? = null,
    val deletions: Int? = null,
    val changes: Int? = null,
    @SerialName("blob_url") val blobUrl: String? = null,
    @SerialName("raw_url") val rawUrl: String? = null,
    @SerialName("contents_url") val contentsUrl: String? = null,
    val patch: PullPatchDto? = null,
    @SerialName("too_large") val tooLarge: Boolean? = null,
    @SerialName("renamed_file") val renamedFile: Boolean? = null,
    @SerialName("new_file") val newFile: Boolean? = null,
    @SerialName("deleted_file") val deletedFile: Boolean? = null,
)

@Serializable
data class PullPatchDto(
    val diff: String? = null,
)

/**
 * `GET .../pulls/{number}/files.json` 的对象形态（不是数组）。
 * 用 [PullDiffEntryDto.statistic].type 区分 `text_type` / `binary_type`。
 */
@Serializable
data class PullFilesJsonDto(
    val diffs: List<PullDiffEntryDto>? = null,
    @SerialName("diff_refs") val diffRefs: PullDiffRefsDto? = null,
)

@Serializable
data class PullDiffEntryDto(
    val path: String? = null,
    val newPath: String? = null,
    val oldPath: String? = null,
    val diff: String? = null,
    val statistic: PullDiffStatisticDto? = null,
)

@Serializable
data class PullDiffStatisticDto(
    val type: String? = null,
)

@Serializable
data class PullDiffRefsDto(
    @SerialName("base_sha") val baseSha: String? = null,
    @SerialName("head_sha") val headSha: String? = null,
    @SerialName("start_sha") val startSha: String? = null,
)

@Serializable
data class PullCommentDto(
    val id: Long? = null,
    val body: String? = null,
    val user: ActorDto? = null,
    @SerialName("comment_type") val commentType: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("html_url") val htmlUrl: String? = null,
)

/**
 * 实测批准体。公开文档的 `{force}` 不能标成评审人通过；
 * 成功为 204 且无响应体，审查人 `accept` 变 true，PR `state` 仍可为 `open`。
 */
@Serializable
data class ApprovePullRequestBody(
    val action: String,
) {
    companion object {
        fun approve(): ApprovePullRequestBody = ApprovePullRequestBody(action = "approve")
    }
}
