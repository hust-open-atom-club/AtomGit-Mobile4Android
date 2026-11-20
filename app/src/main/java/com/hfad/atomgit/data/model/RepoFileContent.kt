package com.hfad.atomgit.data.model

/** 仓库文件正文。 */
data class RepoFileContent(
    val path: String,
    val name: String,
    val content: String,
    val encoding: String?,
    val size: Long?,
    val htmlUrl: String?,
)
