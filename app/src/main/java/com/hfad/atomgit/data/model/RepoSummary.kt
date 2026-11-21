package com.hfad.atomgit.data.model

/** 仓库列表项。路径用 [ownerPath] + [path]，不用 full_name。 */
data class RepoSummary(
    val ownerPath: String,
    val path: String,
    val name: String,
    val description: String? = null,
    val stars: Int = 0,
    val isPrivate: Boolean = false,
    val language: String? = null,
    /** `#RRGGBB`，接口直接给的颜色。 */
    val languageColor: String? = null,
    val updatedAt: String? = null,
    val htmlUrl: String? = null,
)

enum class RepoVisibility(val param: String) {
    All("all"),
    Public("public"),
    Private("private"),
}

/** 接口支持 `created` / `updated` / `pushed` / `full_name` 四种排序 */
enum class RepoSort(val param: String, val direction: String) {
    PushedDesc("pushed", "desc"),
    PushedAsc("pushed", "asc"),
    UpdatedDesc("updated", "desc"),
    UpdatedAsc("updated", "asc"),
    CreatedDesc("created", "desc"),
    CreatedAsc("created", "asc"),
    NameAsc("full_name", "asc"),
    NameDesc("full_name", "desc"),
}
