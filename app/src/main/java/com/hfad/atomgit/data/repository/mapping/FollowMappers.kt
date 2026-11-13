package com.hfad.atomgit.data.repository.mapping

import com.hfad.atomgit.data.model.UserSummary
import com.hfad.atomgit.data.remote.dto.UserSummaryDto

/** `type` 是开放字符串，未知值按用户处理，不用 `enumValueOf`。 */
internal fun UserSummaryDto.toUserSummary(): UserSummary = UserSummary(
    login = login,
    name = name?.takeIf { it.isNotBlank() },
    avatarUrl = avatarUrl,
    htmlUrl = htmlUrl,
    isOrganization = type.equals("Organization", ignoreCase = true),
)
