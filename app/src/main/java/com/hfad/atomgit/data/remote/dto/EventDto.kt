package com.hfad.atomgit.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 本人动态：`events` 按日期分组的对象，不是扁平数组；翻页只用 [next] 游标。
 * 没有稳定事件 id；去重可用账号、[projectId]、[createdAt]、[action]，评论再加 [note].id。
 */
@Serializable
data class UserEventsResponseDto(
    val events: Map<String, List<EventDto>> = emptyMap(),
    val next: String? = null,
)

/**
 * 仓库动态：`events` 是数组；[hasNextPage] 为整数，非 0 表示还有下一页。
 */
@Serializable
data class RepoEventsResponseDto(
    val events: List<EventDto> = emptyList(),
    @SerialName("has_next_page") val hasNextPage: Int = 0,
)

@Serializable
data class EventDto(
    val action: Int? = null,
    @SerialName("action_name") val actionName: String? = null,
    val author: ActorDto? = null,
    @SerialName("author_id") val authorId: Long? = null,
    @SerialName("author_username") val authorUsername: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("project_id") val projectId: Long? = null,
    @SerialName("project_name") val projectName: String? = null,
    val title: String? = null,
    val project: EventProjectDto? = null,
    val note: EventNoteDto? = null,
    @SerialName("push_data") val pushData: EventPushDataDto? = null,
    @SerialName("_links") val links: EventLinksDto? = null,
)

@Serializable
data class EventProjectDto(
    @SerialName("star_count") val starCount: Int? = null,
    @SerialName("forks_count") val forksCount: Int? = null,
    val stared: Boolean? = null,
)

@Serializable
data class EventNoteDto(
    val id: Long? = null,
    val body: String? = null,
    @SerialName("noteable_type") val noteableType: String? = null,
    @SerialName("noteable_iid") val noteableIid: Int? = null,
    @SerialName("noteable_id") val noteableId: Long? = null,
    @SerialName("discussion_id") val discussionId: String? = null,
)

@Serializable
data class EventPushDataDto(
    @SerialName("commit_count") val commitCount: Int? = null,
    val action: String? = null,
    @SerialName("ref_type") val refType: String? = null,
    val ref: String? = null,
    @SerialName("commit_title") val commitTitle: String? = null,
)

@Serializable
data class EventLinksDto(
    val project: String? = null,
    @SerialName("action_type") val actionType: String? = null,
)
