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
    val bio: String? = null,
    /** 与 [bio] 实测同值，某些用户只有其中一个有内容。 */
    val description: String? = null,
    val followers: Int = 0,
    val following: Int = 0,
    val blog: String? = null,
    val company: String? = null,
    val location: String? = null,
    val website: String? = null,
    @SerialName("top_languages") val topLanguages: List<String> = emptyList(),
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
