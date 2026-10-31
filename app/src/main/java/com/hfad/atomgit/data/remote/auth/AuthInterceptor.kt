package com.hfad.atomgit.data.remote.auth

import com.hfad.atomgit.di.ApiHost
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** 只给 [apiHost] 附加 Bearer；401 只广播，由会话层决定是否结束会话。 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenProvider: AccessTokenProvider,
    @param:ApiHost private val apiHost: String,
    private val expiryNotifier: SessionExpiryNotifier,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.url.host != apiHost) {
            return chain.proceed(request)
        }
        val token = tokenProvider.getToken()
            ?: throw IOException("Auth token unavailable")
        val authenticated = request.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
        val response = chain.proceed(authenticated)
        if (response.code == 401) {
            expiryNotifier.notifyExpired()
        }
        return response
    }
}
