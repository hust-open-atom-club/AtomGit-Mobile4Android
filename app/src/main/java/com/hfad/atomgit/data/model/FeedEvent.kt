package com.hfad.atomgit.data.model

/**
 * 动态条目。没有稳定事件 id 时，去重用 [dedupeKey]。
 */
data class FeedEvent(
    val accountId: String,
    val action: Int?,
    val actionName: String?,
    val createdAt: String?,
    val projectId: Long?,
    val projectName: String?,
    val title: String?,
    val authorUsername: String?,
    val noteId: Long?,
    val noteableType: String?,
    val noteableIid: Int?,
    val actionUrl: String?,
    val projectUrl: String?,
) {
    /** 账号 + 项目 + 时间 + 动作；评论再加 note id。 */
    val dedupeKey: String
        get() = buildString {
            append(accountId)
            append('|')
            append(projectId ?: "")
            append('|')
            append(createdAt.orEmpty())
            append('|')
            append(action ?: "")
            if (noteId != null) {
                append('|')
                append(noteId)
            }
        }
}

data class UserFeedPage(
    val items: List<FeedEvent>,
    /** 下一页游标；为 null 表示没有更多。 */
    val next: String?,
)

data class RepoFeedPage(
    val items: List<FeedEvent>,
    val hasNextPage: Boolean,
)
