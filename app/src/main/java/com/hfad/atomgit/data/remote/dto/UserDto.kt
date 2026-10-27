package com.hfad.atomgit.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * [id] 是字符串（如 `6a7760a0533376275c1e1ccd`），缓存键用它。
 * 不要和动态里数字型的 `author.id` / 响应头 `x-arts-id` 混用。
 */
@Serializable
data class UserDto(
    val id: String,
    val login: String,
    val name: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("html_url") val htmlUrl: String? = null,
)

@Serializable
data class ActorDto(
    val id: Long? = null,
    val login: String? = null,
    val username: String? = null,
    val name: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("web_url") val webUrl: String? = null,
)
