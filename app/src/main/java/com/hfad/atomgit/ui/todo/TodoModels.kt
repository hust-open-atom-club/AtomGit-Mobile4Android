package com.hfad.atomgit.ui.todo

import com.hfad.atomgit.data.model.TodoItem
import com.hfad.atomgit.data.remote.error.ApiError

/** 待办顶部分组：待处理 / 跟进。 */
enum class TodoGroup {
    Pending,
    Following,
}

/**
 * 待办数据源。每一类单独分页，不混拼。
 */
enum class TodoSource {
    AssignedIssues,
    NeedMyApprove,
    NeedMyReview,
    CreatedIssues,
    CreatedPulls,
    ;

    val group: TodoGroup
        get() = when (this) {
            AssignedIssues, NeedMyApprove, NeedMyReview -> TodoGroup.Pending
            CreatedIssues, CreatedPulls -> TodoGroup.Following
        }

    companion object {
        fun defaultsFor(group: TodoGroup): TodoSource = when (group) {
            TodoGroup.Pending -> AssignedIssues
            TodoGroup.Following -> CreatedIssues
        }

        fun sourcesFor(group: TodoGroup): List<TodoSource> = when (group) {
            TodoGroup.Pending -> listOf(AssignedIssues, NeedMyApprove, NeedMyReview)
            TodoGroup.Following -> listOf(CreatedIssues, CreatedPulls)
        }
    }
}

data class TodoUiState(
    val group: TodoGroup = TodoGroup.Pending,
    val source: TodoSource = TodoSource.AssignedIssues,
    val items: List<TodoItem> = emptyList(),
    /** 响应头 total_count；缺失时为 null，界面只显示已加载条数。 */
    val totalCount: Long? = null,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    /** 上一页接口返回空数组，没有下一页了。 */
    val endReached: Boolean = false,
    val error: ApiError? = null,
)

/** 单个数据源的分页缓存，切 Tab 时保留。 */
internal data class TodoSourceCache(
    val items: List<TodoItem> = emptyList(),
    val totalCount: Long? = null,
    val page: Int = 0,
    val endReached: Boolean = false,
    val error: ApiError? = null,
)
