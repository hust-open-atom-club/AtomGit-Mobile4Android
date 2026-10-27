package com.hfad.atomgit.data.remote.auth

import com.hfad.atomgit.di.ApiHost
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 网络拦截器：只给 [apiHost] 附加 Bearer。
 * 重定向到其他主机时这一跳不会带上令牌；令牌不进查询串。
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenProvider: AccessTokenProvider,
    @param:ApiHost private val apiHost: String,
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
        return chain.proceed(authenticated)
    }
}
