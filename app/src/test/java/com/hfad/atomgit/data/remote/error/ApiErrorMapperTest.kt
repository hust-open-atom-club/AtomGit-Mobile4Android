package com.hfad.atomgit.data.remote.error

import com.hfad.atomgit.data.remote.auth.MissingTokenException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import okhttp3.Response as OkHttpResponse

class ApiErrorMapperTest {

    @Test
    fun mapsHttpStatuses() {
        assertEquals(ApiError.Unauthorized, errorResponse(401).toApiError())
        assertEquals(ApiError.Forbidden, errorResponse(403).toApiError())
        assertEquals(ApiError.NotFound, errorResponse(404).toApiError())
        assertEquals(ApiError.Server(500, "boom"), errorResponse(500, """{"message":"boom"}""").toApiError())
        assertEquals(ApiError.Unexpected(400, "bad"), errorResponse(400, """{"error":"bad"}""").toApiError())
        assertEquals(ApiError.Unexpected(418, null), errorResponse(418, "<html>teapot</html>").toApiError())
    }

    @Test
    fun parsesRetryAfterSecondsOnly() {
        assertEquals(
            ApiError.RateLimited(30),
            errorResponse(429, headers = mapOf("Retry-After" to "30")).toApiError(),
        )
        assertEquals(ApiError.RateLimited(null), errorResponse(429).toApiError())
        assertEquals(
            ApiError.RateLimited(null),
            errorResponse(429, headers = mapOf("Retry-After" to "Wed, 21 Oct 2015 07:28:00 GMT"))
                .toApiError(),
        )
        assertEquals(
            ApiError.RateLimited(null),
            errorResponse(429, headers = mapOf("Retry-After" to "-1")).toApiError(),
        )
    }

    @Test
    fun mapsReadExceptions() {
        assertEquals(ApiError.Network, IOException("offline").toApiError())
        assertEquals(ApiError.Network, SocketTimeoutException("timeout").toApiError())
        assertEquals(ApiError.Parse, SerializationException("bad json").toApiError())
        assertEquals(ApiError.Unauthorized, MissingTokenException().toApiError())
        assertEquals(ApiError.Forbidden, HttpException(errorResponse(403)).toApiError())
    }

    @Test
    fun writeTimeoutIsUncertainButConnectFailureIsNot() {
        assertEquals(ApiError.WriteUncertain, SocketTimeoutException("timeout").toWriteApiError())
        assertEquals(ApiError.WriteUncertain, IOException("reset").toWriteApiError())
        assertEquals(ApiError.WriteUncertain, SerializationException("bad body").toWriteApiError())
        assertEquals(ApiError.WriteUncertain, EmptyResponseBodyException().toWriteApiError())
        assertEquals(ApiError.Network, ConnectException("refused").toWriteApiError())
        assertEquals(ApiError.Unauthorized, MissingTokenException().toWriteApiError())
        assertEquals(ApiError.Forbidden, HttpException(errorResponse(403)).toWriteApiError())
    }

    @Test
    fun emptySuccessBodyIsParseError() {
        val response = Response.success<Unit>(null)

        var thrown: Exception? = null
        try {
            response.requireBody()
            fail("expected EmptyResponseBodyException")
        } catch (e: Exception) {
            thrown = e
        }

        assertTrue(thrown is EmptyResponseBodyException)
        assertEquals(ApiError.Parse, thrown!!.toApiError())
    }

    @Test
    fun apiCallMapsReadFailure() = runTest {
        val result = apiCall<Unit> { throw IOException("offline") }

        assertEquals(ApiResult.Failure(ApiError.Network), result)
    }

    @Test
    fun apiWriteCallMapsTimeoutToUncertain() = runTest {
        val result = apiWriteCall<Unit> { throw SocketTimeoutException("timeout") }

        assertEquals(ApiResult.Failure(ApiError.WriteUncertain), result)
    }

    @Test
    fun apiCallRethrowsCancellation() = runTest {
        var caught = false
        try {
            apiCall<Unit> { throw CancellationException("cancelled") }
        } catch (e: CancellationException) {
            caught = true
        }

        assertTrue(caught)
    }

    @Test
    fun apiResultMapKeepsFailure() {
        val success: ApiResult<Int> = ApiResult.Success(2)
        assertEquals(ApiResult.Success(4), success.map { it * 2 })

        val failure: ApiResult<Int> = ApiResult.Failure(ApiError.NotFound)
        assertEquals(failure, failure.map { it + 1 })
    }

    private fun errorResponse(
        code: Int,
        body: String = "{}",
        headers: Map<String, String> = emptyMap(),
    ): Response<Unit> {
        val raw = OkHttpResponse.Builder()
            .request(Request.Builder().url("https://api.atomgit.com/api/v5/user").build())
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("error")
            .apply { headers.forEach { (name, value) -> header(name, value) } }
            .build()
        return Response.error<Unit>(body.toResponseBody("application/json".toMediaType()), raw)
    }
}
