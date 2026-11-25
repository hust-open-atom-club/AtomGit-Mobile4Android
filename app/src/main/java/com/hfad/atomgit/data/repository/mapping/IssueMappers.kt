package com.hfad.atomgit.data.repository.mapping

import com.hfad.atomgit.data.model.IssueComment
import com.hfad.atomgit.data.model.IssueDetail
import com.hfad.atomgit.data.model.IssueLabel
import com.hfad.atomgit.data.remote.dto.ActorDto
import com.hfad.atomgit.data.remote.dto.IssueCommentDto
import com.hfad.atomgit.data.remote.dto.IssueDto
import com.hfad.atomgit.data.remote.dto.IssueLabelDto
import com.hfad.atomgit.data.remote.dto.RepoDto

internal fun IssueDto.toIssueDetail(): IssueDetail = IssueDetail(
    number = number,
    title = title?.trim()?.takeIf { it.isNotEmpty() },
    body = body,
    state = state?.trim()?.takeIf { it.isNotEmpty() },
    issueState = issueState?.trim()?.takeIf { it.isNotEmpty() },
    htmlUrl = htmlUrl?.trim()?.takeIf { it.isNotEmpty() },
    createdAt = createdAt,
    updatedAt = updatedAt,
    authorLogin = user.loginOrNull(),
    authorName = user.nameOrNull(),
    authorAvatarUrl = user.avatarOrNull(),
    assignees = assignees.orEmpty().mapNotNull { it.loginOrNull() ?: it.nameOrNull() },
    repoPath = repository.toRepoPath(),
    repoAvatarUrl = repository.repoAvatarOrNull(),
    labels = labels.mapNotNull { it.toIssueLabel() },
)

internal fun IssueCommentDto.toIssueComment(): IssueComment = IssueComment(
    id = id,
    body = body,
    authorLogin = user.loginOrNull(),
    authorName = user.nameOrNull(),
    authorAvatarUrl = user.avatarOrNull(),
    createdAt = createdAt,
)

private fun ActorDto?.loginOrNull(): String? = this?.login?.trim()?.takeIf { it.isNotEmpty() }

private fun ActorDto?.nameOrNull(): String? = this?.name?.trim()?.takeIf { it.isNotEmpty() }

private fun ActorDto?.avatarOrNull(): String? = this?.avatarUrl?.trim()?.takeIf { it.isNotEmpty() }

private fun RepoDto?.repoAvatarOrNull(): String? {
    if (this == null) return null
    return namespace?.avatarUrl?.trim()?.takeIf { it.isNotEmpty() }
        ?: owner?.avatarUrl?.trim()?.takeIf { it.isNotEmpty() }
}

private fun IssueLabelDto.toIssueLabel(): IssueLabel? {
    val text = name?.trim()?.takeIf { it.isNotEmpty() }
        ?: title?.trim()?.takeIf { it.isNotEmpty() }
        ?: return null
    return IssueLabel(name = text, color = color?.trim()?.takeIf { it.isNotEmpty() })
}
