package com.hfad.atomgit.data.repository.mapping

import com.hfad.atomgit.data.model.UserProfile
import com.hfad.atomgit.data.remote.dto.UserDto

internal fun UserDto.toUserProfile(): UserProfile = UserProfile(
    login = login,
    name = name?.takeIf { it.isNotBlank() },
    avatarUrl = avatarUrl,
    bio = bio?.takeIf { it.isNotBlank() } ?: description?.takeIf { it.isNotBlank() },
    followers = followers,
    following = following,
    company = company?.takeIf { it.isNotBlank() },
    location = location?.takeIf { it.isNotBlank() },
    website = website?.takeIf { it.isNotBlank() } ?: blog?.takeIf { it.isNotBlank() },
    topLanguages = topLanguages,
)
