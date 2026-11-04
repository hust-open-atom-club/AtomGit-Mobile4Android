package com.hfad.atomgit.data.remote.error

import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

/** 2xx 但响应体为空。 */
class EmptyResponseBodyException : IOException("Empty response body")

/** 执行读请求，异常转为 [ApiResult]。 */
suspend fun <T> apiCall(block: suspend () -> T): ApiResult<T> = try {
    ApiResult.Success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    ApiResult.Failure(e.toApiError())
}

/** 执行写请求，异常转为 [ApiResult]。 */
suspend fun <T> apiWriteCall(block: suspend () -> T): ApiResult<T> = try {
    ApiResult.Success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    ApiResult.Failure(e.toWriteApiError())
}

/** 取出成功响应体。 */
fun <T> Response<T>.requireBody(): T {
    if (!isSuccessful) throw HttpException(this)
    return body() ?: throw EmptyResponseBodyException()
}
