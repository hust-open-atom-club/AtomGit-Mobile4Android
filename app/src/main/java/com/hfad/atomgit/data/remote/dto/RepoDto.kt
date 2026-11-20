package com.hfad.atomgit.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RepoDto(
    val id: Long? = null,
    val name: String? = null,
    /**
     * 组织仓库可能是带空格的展示名（如 `111 / mobile-org-probe`），
     * 不能拿来拼接口路径；路径用 [namespace].path + [path]。
     */
    @SerialName("full_name") val fullName: String? = null,
    val path: String? = null,
    val description: String? = null,
    @SerialName("private") val isPrivate: Boolean? = null,
    @SerialName("default_branch") val defaultBranch: String? = null,
    @SerialName("html_url") val htmlUrl: String? = null,
    @SerialName("web_url") val webUrl: String? = null,
    @SerialName("stargazers_count") val stargazersCount: Int? = null,
    @SerialName("forks_count") val forksCount: Int? = null,
    val language: String? = null,
    /** 形如 `["Java", "#b07219"]`：语言名 + 颜色。 */
    @SerialName("main_repository_language") val mainRepositoryLanguage: List<String> = emptyList(),
    @SerialName("pushed_at") val pushedAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    val fork: Boolean? = null,
    val namespace: NamespaceDto? = null,
    val owner: RepoOwnerDto? = null,
)

@Serializable
data class NamespaceDto(
    val path: String? = null,
    val name: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
)

@Serializable
data class RepoOwnerDto(
    val login: String? = null,
    val name: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
)

@Serializable
data class OrgDto(
    val id: Long? = null,
    val login: String? = null,
    val path: String? = null,
    val name: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("html_url") val htmlUrl: String? = null,
)

@Serializable
data class OrgMembershipDto(
    val role: String? = null,
    val state: String? = null,
    val organization: OrgDto? = null,
)
