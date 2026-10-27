package com.hfad.atomgit.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BranchDto(
    val name: String,
    @SerialName("default_branch") val defaultBranch: Boolean? = null,
    val commit: BranchCommitDto? = null,
)

@Serializable
data class BranchCommitDto(
    val sha: String? = null,
    val url: String? = null,
)

@Serializable
data class TagDto(
    val name: String? = null,
    @SerialName("commit") val commit: BranchCommitDto? = null,
)

@Serializable
data class CommitDto(
    val sha: String,
    val commit: CommitDetailDto? = null,
    @SerialName("html_url") val htmlUrl: String? = null,
    val stats: CommitStatsDto? = null,
)

@Serializable
data class CommitDetailDto(
    val message: String? = null,
    val author: CommitAuthorDto? = null,
    val committer: CommitAuthorDto? = null,
)

@Serializable
data class CommitAuthorDto(
    val name: String? = null,
    val email: String? = null,
    val date: String? = null,
)

@Serializable
data class CommitStatsDto(
    val additions: Int? = null,
    val deletions: Int? = null,
    val total: Int? = null,
)

@Serializable
data class ContentFileDto(
    val type: String? = null,
    val encoding: String? = null,
    val size: Long? = null,
    val name: String? = null,
    val path: String? = null,
    /** base64 内容。 */
    val content: String? = null,
    val sha: String? = null,
    @SerialName("html_url") val htmlUrl: String? = null,
    @SerialName("download_url") val downloadUrl: String? = null,
)

@Serializable
data class ContentItemDto(
    val type: String? = null,
    val size: Long? = null,
    val name: String? = null,
    val path: String? = null,
    val sha: String? = null,
    @SerialName("html_url") val htmlUrl: String? = null,
    @SerialName("download_url") val downloadUrl: String? = null,
)

@Serializable
data class GitTreeDto(
    val sha: String? = null,
    val tree: List<GitTreeEntryDto> = emptyList(),
    val truncated: Boolean? = null,
)

@Serializable
data class GitTreeEntryDto(
    val sha: String? = null,
    val name: String? = null,
    /** `tree` 表示目录，`blob` 表示文件。 */
    val type: String? = null,
    val path: String? = null,
    val mode: String? = null,
    val size: Long? = null,
)
