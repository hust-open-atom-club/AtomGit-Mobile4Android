package com.hfad.atomgit.data.model

/** 页码分页的列表页。总数头缺失时 [totalCount] / [totalPage] 为 null，不能当成 0。 */
data class PagedItems<T>(
    val items: List<T>,
    val totalCount: Long? = null,
    val totalPage: Int? = null,
)
