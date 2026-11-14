package com.hfad.atomgit.data.model

/** 粉丝 / 关注列表项；组织也会出现在列表里，用 [isOrganization] 区分。 */
data class UserSummary(
    val login: String,
    val name: String? = null,
    val avatarUrl: String? = null,
    val htmlUrl: String? = null,
    val isOrganization: Boolean = false,
)

enum class FollowKind {
    Followers,
    Following,
}
