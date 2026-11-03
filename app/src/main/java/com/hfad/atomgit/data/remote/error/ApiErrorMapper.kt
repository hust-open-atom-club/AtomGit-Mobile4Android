package com.hfad.atomgit.data.remote.error

import com.hfad.atomgit.data.remote.auth.MissingTokenException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

private const val MAX_ERROR_BODY_CHARS = 8 * 1024
private const val MAX_MESSAGE_LENGTH = 300

/** 读请求异常转 [ApiError]。 */
fun Throwable.toApiError(): ApiError = when (this) {
    is HttpException -> response()?.toApiError() ?: ApiError.Unexpected(code(), message)
    is MissingTokenException -> ApiError.Unauthorized
    is EmptyResponseBodyException -> ApiError.Parse
    is SerializationException -> ApiError.Parse
    is SocketTimeoutException -> ApiError.Network
    is ConnectException, is UnknownHostException, is SSLException -> ApiError.Network
    is IOException -> ApiError.Network
    else -> ApiError.Unexpected(message = message)
}

/** 写请求异常转 [ApiError]；结果不明归为 [ApiError.WriteUncertain]。 */
fun Throwable.toWriteApiError(): ApiError = when (this) {
    is HttpException -> response()?.toApiError() ?: ApiError.Unexpected(code(), message)
    is MissingTokenException -> ApiError.Unauthorized
    is EmptyResponseBodyException, is SerializationException -> ApiError.WriteUncertain
    is SocketTimeoutException -> ApiError.WriteUncertain
    is ConnectException, is UnknownHostException, is SSLException -> ApiError.Network
    is IOException -> ApiError.WriteUncertain
    else -> ApiError.Unexpected(message = message)
}

/** 错误响应转 [ApiError]。 */
fun Response<*>.toApiError(): ApiError {
    val message = readErrorMessage()
    return when (code()) {
        401 -> ApiError.Unauthorized
        403 -> ApiError.Forbidden
        404 -> ApiError.NotFound
        429 -> ApiError.RateLimited(parseRetryAfterSeconds(headers()["Retry-After"]))
        in 500..599 -> ApiError.Server(code(), message)
        else -> ApiError.Unexpected(code(), message)
    }
}

/** 仅支持 Retry-After 的秒数形式。 */
internal fun parseRetryAfterSeconds(value: String?): Long? =
    value?.trim()?.toLongOrNull()?.takeIf { it >= 0 }

private fun Response<*>.readErrorMessage(): String? {
    val raw = runCatching { errorBody()?.string() }.getOrNull() ?: return null
    if (raw.isBlank()) return null
    return runCatching {
        val obj = Json.parseToJsonElement(raw.take(MAX_ERROR_BODY_CHARS)).jsonObject
        (obj["message"] ?: obj["error"])?.jsonPrimitive?.contentOrNull
    }.getOrNull()
        ?.trim()
        ?.take(MAX_MESSAGE_LENGTH)
        ?.takeIf { it.isNotEmpty() }
}
