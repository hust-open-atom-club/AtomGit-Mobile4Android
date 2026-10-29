package com.hfad.atomgit.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.hfad.atomgit.ui.theme.AtomGitTheme
import org.junit.Rule
import org.junit.Test

/**
 * 导航骨架的测试：启动进动态、切到待办、进共享详情、返回后仍停在待办。
 */
class NavigationSmokeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun switchingTabThenOpeningDetailReturnsToSameTab() {
        composeRule.setContent {
            AtomGitTheme {
                AtomGitNavHost()
            }
        }

        composeRule.onNodeWithTag("screen-feed").assertIsDisplayed()

        composeRule.onNodeWithTag("tab-Todo").performClick()
        composeRule.onNodeWithTag("screen-todo").assertIsDisplayed()

        composeRule.onNodeWithText("打开 Issue 详情").performClick()
        composeRule.onNodeWithTag("screen-issue-detail").assertIsDisplayed()
        composeRule.onNodeWithTag("tab-Todo").assertIsDisplayed()

        composeRule.onNodeWithText("返回").performClick()
        composeRule.onNodeWithTag("screen-todo").assertIsDisplayed()
        composeRule.onNodeWithTag("tab-Todo").assertIsDisplayed()
    }
}
