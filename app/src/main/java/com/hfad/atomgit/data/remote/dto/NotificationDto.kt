package com.hfad.atomgit.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NotificationsResponseDto(
    val total: Int? = null,
    val list: List<NotificationDto> = emptyList(),
)

@Serializable
data class NotificationDto(
    val id: String,
    val type: String? = null,
    val unread: Boolean? = null,
    /** 字段名是 `update_at`（不是 `updated_at`）。 */
    @SerialName("update_at") val updateAt: String? = null,
    val content: String? = null,
    @SerialName("html_url") val htmlUrl: String? = null,
    val actor: ActorDto? = null,
    val repository: NotificationRepoDto? = null,
)

@Serializable
data class NotificationRepoDto(
    val id: Long? = null,
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("human_name") val humanName: String? = null,
    val url: String? = null,
)
