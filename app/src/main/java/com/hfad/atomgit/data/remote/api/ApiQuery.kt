package com.hfad.atomgit.data.remote.api

/**
 * 待办 Issue 的 filter。不用 `all`：它的结果整体不是「待我处理」。
 */
enum class UserIssueFilter(private val wire: String) {
    Created("created"),
    Assigned("assigned");

    override fun toString(): String = wire
}

enum class IssueState(private val wire: String) {
    Open("open"),
    Closed("closed"),
    All("all");

    override fun toString(): String = wire
}

/**
 * 待办 PR 的 scope。不含 `assigned_to_me`：实测它不会因审查人/评审人指派而出现 PR。
 */
enum class UserPullScope(private val wire: String) {
    CreatedByMe("created_by_me"),
    NeedMyApprove("need_my_approve"),
    NeedMyReview("need_my_review");

    override fun toString(): String = wire
}

enum class PullState(private val wire: String) {
    Open("open"),
    Closed("closed"),
    All("all");

    override fun toString(): String = wire
}
