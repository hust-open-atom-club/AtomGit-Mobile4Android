package com.hfad.atomgit.data.model

/** 他人资料。字段只保留页面要展示的。 */
data class UserProfile(
    val login: String,
    val name: String? = null,
    val avatarUrl: String? = null,
    val bio: String? = null,
    val followers: Int = 0,
    val following: Int = 0,
    val company: String? = null,
    val location: String? = null,
    val website: String? = null,
    val topLanguages: List<String> = emptyList(),
)

/** 仓库、组织、已 star 的数量；总数头缺失时为 null。 */
data class ProfileCounts(
    val repos: Long? = null,
    val orgs: Long? = null,
    val starred: Long? = null,
)
