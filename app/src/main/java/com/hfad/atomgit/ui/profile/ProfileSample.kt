package com.hfad.atomgit.ui.profile

import androidx.compose.ui.graphics.Color

data class SampleRepo(
    val owner: String,
    val name: String,
    val stars: Int,
    val language: String,
    val languageColor: Color,
)

data class SampleOrg(
    val name: String,
    val login: String,
    val repos: List<SampleRepo>,
)

object ProfileSample {
    private val kotlinColor = Color(0xFFA97BFF)
    private val markdownColor = Color(0xFF083FA1)

    val repos = listOf(
        SampleRepo("yesterdaylz", "to-do-APP", 1, "Kotlin", kotlinColor),
        SampleRepo("yesterdaylz", "echo", 1, "Kotlin", kotlinColor),
        SampleRepo("yesterdaylz", "README", 0, "Markdown", markdownColor),
        SampleRepo("yesterdaylz", "atomgit-notes", 3, "Kotlin", kotlinColor),
    )

    val orgs = listOf(
        SampleOrg(
            name = "111",
            login = "donk945",
            repos = listOf(
                SampleRepo("donk945", "mobile-org-probe", 0, "Kotlin", kotlinColor),
            ),
        ),
        SampleOrg(
            name = "开放原子",
            login = "openatom",
            repos = listOf(
                SampleRepo("openatom", "community", 12, "Markdown", markdownColor),
                SampleRepo("openatom", "mobile", 4, "Kotlin", kotlinColor),
            ),
        ),
    )
}
