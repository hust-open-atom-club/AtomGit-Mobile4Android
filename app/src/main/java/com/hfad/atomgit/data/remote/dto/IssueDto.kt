package com.hfad.atomgit.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class IssueDto(
    val id: Long? = null,
    val number: Int,
    val title: String? = null,
    val body: String? = null,
    /**
     * 英文完成态，如 `open` / `closed`。须与 [issueState] 同时回读，不能只看其中一个。
     */
    val state: String? = null,
    /**
     * 中文业务态（如「待办的」「已完成」）。关闭一次后可能仍是「待办的」。
     */
    @SerialName("issue_state") val issueState: String? = null,
    @SerialName("html_url") val htmlUrl: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val user: ActorDto? = null,
    val assignees: List<ActorDto>? = null,
    val repository: RepoDto? = null,
)

@Serializable
data class IssueCommentDto(
    val id: Long? = null,
    val body: String? = null,
    val user: ActorDto? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("html_url") val htmlUrl: String? = null,
)

@Serializable
data class CommentBody(
    val body: String,
)
