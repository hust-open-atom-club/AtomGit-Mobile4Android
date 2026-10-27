package com.hfad.atomgit.data.remote.auth

import com.hfad.atomgit.data.remote.AtomGitHosts
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException
import java.util.concurrent.TimeUnit

class AuthInterceptorTest {

    @Test
    fun addsBearerOnlyOnApiHost() {
        val provider = AccessTokenProvider().apply { setToken("pat-secret") }
        val interceptor = AuthInterceptor(provider, AtomGitHosts.API)

        val apiChain = RecordingChain(
            Request.Builder().url("https://api.atomgit.com/api/v5/user").build(),
        )
        interceptor.intercept(apiChain)
        assertEquals("Bearer pat-secret", apiChain.proceeded?.header("Authorization"))
        assertNull(apiChain.proceeded?.url?.queryParameter("access_token"))

        val otherChain = RecordingChain(
            Request.Builder().url("https://cdn.example.com/avatar.png").build(),
        )
        interceptor.intercept(otherChain)
        assertNull(otherChain.proceeded?.header("Authorization"))
    }

    @Test
    fun throwsWhenTokenMissingOnApiHost() {
        val interceptor = AuthInterceptor(AccessTokenProvider(), AtomGitHosts.API)
        val chain = RecordingChain(
            Request.Builder().url("https://api.atomgit.com/api/v5/user").build(),
        )
        try {
            interceptor.intercept(chain)
            fail("expected IOException")
        } catch (e: IOException) {
            assertTrue(e.message!!.contains("Auth token unavailable"))
        }
        assertNull(chain.proceeded)
    }

    private class RecordingChain(
        private val original: Request,
    ) : Interceptor.Chain {
        var proceeded: Request? = null

        override fun request(): Request = original

        override fun proceed(request: Request): Response {
            proceeded = request
            return Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("{}".toResponseBody("application/json".toMediaType()))
                .build()
        }

        override fun connection() = null
        override fun call() = error("unused")
        override fun connectTimeoutMillis() = 0
        override fun withConnectTimeout(timeout: Int, unit: TimeUnit) = this
        override fun readTimeoutMillis() = 0
        override fun withReadTimeout(timeout: Int, unit: TimeUnit) = this
        override fun writeTimeoutMillis() = 0
        override fun withWriteTimeout(timeout: Int, unit: TimeUnit) = this
    }
}
