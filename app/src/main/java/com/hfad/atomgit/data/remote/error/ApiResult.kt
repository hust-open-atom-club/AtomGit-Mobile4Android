package com.hfad.atomgit.data.remote.error

/** 数据层返回值。协程取消不封装，直接抛出。 */
sealed interface ApiResult<out T> {

    data class Success<T>(val value: T) : ApiResult<T>

    data class Failure(val error: ApiError) : ApiResult<Nothing>
}

fun <T> ApiResult<T>.getOrNull(): T? = when (this) {
    is ApiResult.Success -> value
    is ApiResult.Failure -> null
}

fun <T> ApiResult<T>.errorOrNull(): ApiError? = when (this) {
    is ApiResult.Success -> null
    is ApiResult.Failure -> error
}

inline fun <T, R> ApiResult<T>.map(transform: (T) -> R): ApiResult<R> = when (this) {
    is ApiResult.Success -> ApiResult.Success(transform(value))
    is ApiResult.Failure -> this
}
