package com.hfad.atomgit.di

import com.hfad.atomgit.BuildConfig
import com.hfad.atomgit.data.remote.AtomGitHosts
import com.hfad.atomgit.data.remote.api.AtomGitV5Service
import com.hfad.atomgit.data.remote.api.AtomGitV8Service
import com.hfad.atomgit.data.remote.auth.AuthInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @ApiHost
    fun provideApiHost(): String = AtomGitHosts.API

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        val interceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        interceptor.redactHeader("Authorization")
        interceptor.redactHeader("Cookie")
        interceptor.redactHeader("Set-Cookie")
        return interceptor
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        loggingInterceptor: HttpLoggingInterceptor,
    ): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        // 评论、批准、标已读不能由 OkHttp 自动重发；读重试留给 Repository。
        .retryOnConnectionFailure(false)
        .addNetworkInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()

    @Provides
    @Singleton
    @V5
    fun provideV5Retrofit(
        okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit = Retrofit.Builder()
        .baseUrl(AtomGitHosts.V5_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    @V8
    fun provideV8Retrofit(
        okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit = Retrofit.Builder()
        .baseUrl(AtomGitHosts.V8_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideAtomGitV5Service(@V5 retrofit: Retrofit): AtomGitV5Service =
        retrofit.create(AtomGitV5Service::class.java)

    @Provides
    @Singleton
    fun provideAtomGitV8Service(@V8 retrofit: Retrofit): AtomGitV8Service =
        retrofit.create(AtomGitV8Service::class.java)
}
