package com.hfad.atomgit.data.repository

import com.hfad.atomgit.data.model.ProfileCounts
import com.hfad.atomgit.data.model.UserProfile
import com.hfad.atomgit.data.remote.api.AtomGitV5Service
import com.hfad.atomgit.data.remote.error.ApiResult
import com.hfad.atomgit.data.remote.error.apiCall
import com.hfad.atomgit.data.remote.error.getOrNull
import com.hfad.atomgit.data.remote.totalCountHeader
import com.hfad.atomgit.data.repository.mapping.toUserProfile
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

/** 他人资料与数量，只读，不需要 token。 */
interface UserProfileRepository {
    suspend fun get(login: String): ApiResult<UserProfile>

    suspend fun counts(login: String): ProfileCounts
}

@Singleton
class DefaultUserProfileRepository @Inject constructor(
    private val api: AtomGitV5Service,
) : UserProfileRepository {

    override suspend fun get(login: String): ApiResult<UserProfile> = apiCall {
        api.getUser(login).toUserProfile()
    }

    override suspend fun counts(login: String): ProfileCounts = ProfileCounts(
        repos = apiCall { api.listUserRepos(login, 1, 1) }.totalCountOf(),
        orgs = apiCall { api.listUserOrgs(login, 1, 1) }.totalCountOf(),
        starred = apiCall { api.listUserStarred(login, 1, 1) }.totalCountOf(),
    )
}

private fun ApiResult<Response<*>>.totalCountOf(): Long? = getOrNull()?.totalCountHeader()
