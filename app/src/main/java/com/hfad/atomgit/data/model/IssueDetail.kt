package com.hfad.atomgit.data.model

/**
 * Issue 详情。完成态 [state] 与业务态 [issueState] 都保留：
 * 关闭一次后业务态仍可能是「待办的」，不能只用其中一个判断。
 */
data class IssueDetail(
    val number: Int,
    val title: String?,
    val body: String?,
    val state: String?,
    val issueState: String?,
    val htmlUrl: String?,
    val createdAt: String?,
    val updatedAt: String?,
    val authorLogin: String?,
    val authorName: String?,
    val authorAvatarUrl: String? = null,
    val assignees: List<String> = emptyList(),
    /** `namespace.path / path`；不用带空格的 full_name。 */
    val repoPath: String? = null,
    val repoAvatarUrl: String? = null,
    val labels: List<IssueLabel> = emptyList(),
) {
    val authorLabel: String
        get() = authorName.displayName() ?: authorLogin.displayName() ?: "未知用户"
}

data class IssueComment(
    val id: Long?,
    val body: String?,
    val authorLogin: String?,
    val authorName: String?,
    val authorAvatarUrl: String? = null,
    val createdAt: String?,
) {
    val authorLabel: String
        get() = authorName.displayName() ?: authorLogin.displayName() ?: "未知用户"
}

data class IssueLabel(
    val name: String,
    val color: String? = null,
)

private fun String?.displayName(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
