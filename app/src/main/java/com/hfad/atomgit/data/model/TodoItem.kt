package com.hfad.atomgit.data.model

enum class TodoKind {
    Issue,
    PullRequest,
}

data class TodoAssignee(
    val login: String?,
    val accept: Boolean?,
    /** 实测审查人条目里也有该字段，常为 null；评审人的状态在 [TodoReviewer.acceptStatus]。 */
    val acceptStatus: String? = null,
)

data class TodoReviewer(
    val login: String?,
    val accept: Boolean?,
    val acceptStatus: String?,
)

/**
 * 待办列表项。Issue 同时保留 [state] 与 [issueState]；
 * PR 同时保留 [state]、审查人 [assignees]、评审人 [approvalReviewers]。
 */
data class TodoItem(
    val accountId: String,
    val kind: TodoKind,
    val number: Int,
    val title: String?,
    /** `namespace.path/path`，不用带空格的 full_name。 */
    val repoPath: String?,
    val htmlUrl: String?,
    val updatedAt: String?,
    val state: String?,
    val issueState: String? = null,
    val assignees: List<TodoAssignee> = emptyList(),
    val approvalReviewers: List<TodoReviewer> = emptyList(),
) {
    val dedupeKey: String
        get() = "$accountId|${repoPath.orEmpty()}|$kind|$number"
}

/**
 * 待办分页结果。头缺失时 [totalCount] / [totalPage] 为 null，不能当成 0。
 * 空 [items] 表示没有下一页。
 */
data class TodoPage(
    val items: List<TodoItem>,
    val totalCount: Long?,
    val totalPage: Int?,
)
