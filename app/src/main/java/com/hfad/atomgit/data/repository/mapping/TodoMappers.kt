package com.hfad.atomgit.data.repository.mapping

import com.hfad.atomgit.data.model.TodoAssignee
import com.hfad.atomgit.data.model.TodoItem
import com.hfad.atomgit.data.model.TodoKind
import com.hfad.atomgit.data.model.TodoReviewer
import com.hfad.atomgit.data.remote.dto.IssueDto
import com.hfad.atomgit.data.remote.dto.PullRequestDto

fun IssueDto.toTodoItem(accountId: String): TodoItem = TodoItem(
    accountId = accountId,
    kind = TodoKind.Issue,
    number = number,
    title = title,
    repoPath = repository.toRepoPath(),
    htmlUrl = htmlUrl,
    updatedAt = updatedAt,
    state = state,
    issueState = issueState,
)

fun PullRequestDto.toTodoItem(accountId: String): TodoItem = TodoItem(
    accountId = accountId,
    kind = TodoKind.PullRequest,
    number = number,
    title = title,
    repoPath = repository.toRepoPath(),
    htmlUrl = htmlUrl,
    updatedAt = updatedAt,
    state = state,
    assignees = assignees.orEmpty().map {
        TodoAssignee(login = it.login, accept = it.accept)
    },
    approvalReviewers = approvalReviewers.orEmpty().map {
        TodoReviewer(
            login = it.login,
            accept = it.accept,
            acceptStatus = it.acceptStatus,
        )
    },
)
