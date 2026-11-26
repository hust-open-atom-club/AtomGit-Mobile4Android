package com.hfad.atomgit.ui.notification

import com.hfad.atomgit.data.remote.error.ApiError
import com.hfad.atomgit.data.remote.error.SourceFailure

data class NotificationRepoOption(
    val owner: String,
    val path: String,
) {
    val key: String get() = "$owner/$path"
    val label: String get() = "$owner/$path"
}

data class NotificationUiState(
    /** 只看被 @ 的通知。接口没有单独的重点字段，用类型 `referer`。 */
    val importantOnly: Boolean = false,
    val kindFilter: NotificationKindFilter = NotificationKindFilter.All,
    val repos: List<NotificationRepoOption> = emptyList(),
    val selectedRepoKey: String? = null,
    val threads: List<NotificationThread> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val endReached: Boolean = false,
    val error: ApiError? = null,
    val partialFailures: List<SourceFailure> = emptyList(),
    val notice: String? = null,
) {
    val hasActiveFilter: Boolean
        get() = importantOnly ||
            kindFilter != NotificationKindFilter.All ||
            selectedRepoKey != null
}
