package com.hfad.atomgit.data.remote

import retrofit2.Response

/**
 * 部分列表把总数放在响应头。缺失时返回 null，不能当成 0 条。
 * 这些列表的分页终止条件仍是空数组。
 */
fun Response<*>.totalCountHeader(): Long? =
    headers()["total_count"]?.toLongOrNull()

fun Response<*>.totalPageHeader(): Int? =
    headers()["total_page"]?.toIntOrNull()
