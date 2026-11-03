package com.hfad.atomgit.data.repository.mapping

import com.hfad.atomgit.data.model.FeedEvent
import com.hfad.atomgit.data.remote.dto.EventDto

fun EventDto.toFeedEvent(accountId: String): FeedEvent = FeedEvent(
    accountId = accountId,
    action = action,
    actionName = actionName,
    createdAt = createdAt,
    projectId = projectId,
    projectName = projectName,
    title = title,
    authorUsername = authorUsername ?: author?.username ?: author?.login,
    noteId = note?.id,
    noteableType = note?.noteableType,
    noteableIid = note?.noteableIid,
    actionUrl = links?.actionType,
    projectUrl = links?.project,
)

/**
 * 本人动态按日期分组；日期键（yyyy-MM-dd）降序后摊平组内顺序。
 */
fun Map<String, List<EventDto>>.flattenUserEvents(accountId: String): List<FeedEvent> =
    entries
        .sortedByDescending { it.key }
        .flatMap { (_, events) -> events.map { it.toFeedEvent(accountId) } }
