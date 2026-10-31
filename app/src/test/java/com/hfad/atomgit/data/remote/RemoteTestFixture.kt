package com.hfad.atomgit.data.remote

import com.hfad.atomgit.data.remote.api.AtomGitV5Service
import com.hfad.atomgit.data.remote.api.AtomGitV8Service
import com.hfad.atomgit.data.remote.auth.AccessTokenProvider
import com.hfad.atomgit.data.remote.auth.AuthInterceptor
import com.hfad.atomgit.data.remote.auth.SessionExpiryNotifier
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

internal class RemoteTestFixture(
    val server: MockWebServer = MockWebServer(),
    val tokenProvider: AccessTokenProvider = AccessTokenProvider(),
) {
    val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    val authInterceptor = AuthInterceptor(
        tokenProvider = tokenProvider,
        apiHost = server.hostName,
        expiryNotifier = SessionExpiryNotifier(),
    )

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)
        .addNetworkInterceptor(authInterceptor)
        .build()

    fun v5Service(): AtomGitV5Service =
        Retrofit.Builder()
            .baseUrl(server.url("/api/v5/"))
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(AtomGitV5Service::class.java)

    fun v8Service(): AtomGitV8Service =
        Retrofit.Builder()
            .baseUrl(server.url("/api/v8/"))
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(AtomGitV8Service::class.java)

    fun close() {
        server.shutdown()
    }
}
