package com.hfad.atomgit.data.remote.api

import com.hfad.atomgit.data.remote.dto.UserDto
import javax.inject.Inject

/**
 * 会话校验
 */
interface CurrentUserApi {
    suspend fun currentUser(): UserDto
}

class RetrofitCurrentUserApi @Inject constructor(
    private val service: AtomGitV5Service,
) : CurrentUserApi {
    override suspend fun currentUser(): UserDto = service.getAuthenticatedUser()
}
