package com.hfad.atomgit.data.remote.error

/** 接口错误的统一类型。 */
sealed interface ApiError {

    data object Unauthorized : ApiError

    data object Forbidden : ApiError

    data object NotFound : ApiError

    /** 仅解析 Retry-After 的秒数形式，缺失时为 null。 */
    data class RateLimited(val retryAfterSeconds: Long? = null) : ApiError

    data object Network : ApiError

    /** 2xx 但响应体为空或反序列化失败。 */
    data object Parse : ApiError

    data class Server(val code: Int, val message: String? = null) : ApiError

    data class Unexpected(val code: Int? = null, val message: String? = null) : ApiError

    /** 写请求结果不确定，需回读确认。 */
    data object WriteUncertain : ApiError

    /** 多来源部分失败。 */
    data class Partial(val failures: List<SourceFailure>) : ApiError
}

data class SourceFailure(val source: String, val error: ApiError)
