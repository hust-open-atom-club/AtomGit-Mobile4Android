package com.hfad.atomgit.data.repository.mapping

import com.hfad.atomgit.data.model.RepoSummary
import com.hfad.atomgit.data.remote.dto.RepoDto

internal fun RepoDto.toRepoSummary(): RepoSummary = RepoSummary(
    ownerPath = namespace?.path.orEmpty(),
    path = path.orEmpty(),
    name = name ?: path.orEmpty(),
    description = description?.takeIf { it.isNotBlank() },
    stars = stargazersCount ?: 0,
    isPrivate = isPrivate == true,
    language = (language ?: mainRepositoryLanguage.firstOrNull())?.takeIf { it.isNotBlank() },
    languageColor = mainRepositoryLanguage.getOrNull(1),
    updatedAt = updatedAt,
    htmlUrl = htmlUrl,
)
