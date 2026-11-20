package com.hfad.atomgit.data.repository.mapping

import com.hfad.atomgit.data.model.CommitSummary
import com.hfad.atomgit.data.model.RepoDetail
import com.hfad.atomgit.data.model.RepoEntry
import com.hfad.atomgit.data.model.RepoEntryKind
import com.hfad.atomgit.data.model.RepoRef
import com.hfad.atomgit.data.model.RepoRefKind
import com.hfad.atomgit.data.remote.dto.BranchDto
import com.hfad.atomgit.data.remote.dto.CommitDto
import com.hfad.atomgit.data.remote.dto.ContentItemDto
import com.hfad.atomgit.data.remote.dto.RepoDto
import com.hfad.atomgit.data.remote.dto.TagDto

internal fun RepoDto.toRepoDetail(owner: String, repo: String) = RepoDetail(
    owner = namespace?.path?.takeIf { it.isNotBlank() } ?: owner,
    path = path?.takeIf { it.isNotBlank() } ?: repo,
    name = name?.takeIf { it.isNotBlank() } ?: repo,
    description = description?.takeIf { it.isNotBlank() },
    stars = stargazersCount ?: 0,
    isPrivate = isPrivate == true,
    defaultBranch = defaultBranch?.takeIf { it.isNotBlank() },
    htmlUrl = htmlUrl?.takeIf { it.isNotBlank() } ?: webUrl?.takeIf { it.isNotBlank() },
)

internal fun ContentItemDto.toRepoEntry(): RepoEntry? {
    val entryPath = path?.takeIf { it.isNotBlank() } ?: return null
    return RepoEntry(
        name = name?.takeIf { it.isNotBlank() } ?: entryPath.substringAfterLast('/'),
        path = entryPath,
        kind = when (type) {
            "dir" -> RepoEntryKind.Directory
            "file" -> RepoEntryKind.File
            else -> RepoEntryKind.Unsupported
        },
    )
}

internal fun CommitDto.toCommitSummary() = CommitSummary(
    sha = sha,
    message = commit?.message?.lineSequence()?.firstOrNull()?.takeIf { it.isNotBlank() } ?: "无提交说明",
    date = commit?.author?.date ?: commit?.committer?.date,
)

internal fun BranchDto.toRepoRef() = RepoRef(
    name = name,
    kind = RepoRefKind.Branch,
    sha = commit?.sha?.takeIf { it.isNotBlank() },
    isDefault = defaultBranch == true,
)

internal fun TagDto.toRepoRef(): RepoRef? = name?.takeIf { it.isNotBlank() }?.let {
    RepoRef(it, RepoRefKind.Tag, commit?.sha?.takeIf { sha -> sha.isNotBlank() })
}
