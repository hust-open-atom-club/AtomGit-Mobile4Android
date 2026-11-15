package com.hfad.atomgit.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 粉丝列表没有 `following_url`，两个端点共用这个全可空 DTO。 */
@Serializable
data class UserSummaryDto(
    val id: String? = null,
    @SerialName("object_id") val objectId: String? = null,
    val login: String = "",
    val name: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("html_url") val htmlUrl: String? = null,
    @SerialName("followers_url") val followersUrl: String? = null,
    @SerialName("following_url") val followingUrl: String? = null,
    val type: String? = null,
    val url: String? = null,
)
