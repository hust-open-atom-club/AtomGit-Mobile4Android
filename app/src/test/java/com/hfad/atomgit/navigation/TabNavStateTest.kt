package com.hfad.atomgit.navigation

import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TabNavStateTest {

    private fun newState(initialTab: TopLevelTab = TopLevelTab.Feed): TabNavState {
        val stacks = TopLevelTab.entries.associateWith { NavBackStack<NavKey>(it.root) }
        return TabNavState(mutableStateOf(initialTab), stacks)
    }

    @Test
    fun pushAddsToSelectedStack() {
        val state = newState()

        state.push(IssueDetailRoute("owner", "repo", 1))

        assertEquals(2, state.currentStack.size)
        assertTrue(state.currentStack.last() is IssueDetailRoute)
    }

    @Test
    fun popReturnsToEntryRoot() {
        val state = newState()
        state.push(IssueDetailRoute("owner", "repo", 1))

        assertTrue(state.pop())
        assertEquals(1, state.currentStack.size)
    }

    @Test
    fun popAtEntryRootIsNotHandled() {
        val state = newState()

        assertFalse(state.pop())
        assertEquals(1, state.currentStack.size)
    }

    @Test
    fun switchingTabKeepsEachStackSeparate() {
        val state = newState()
        state.push(IssueDetailRoute("owner", "repo", 1))

        state.select(TopLevelTab.Todo)
        assertEquals(1, state.currentStack.size)
        state.push(IssueDetailRoute("owner", "repo", 2))
        assertEquals(2, state.currentStack.size)

        state.select(TopLevelTab.Feed)
        assertEquals(2, state.currentStack.size)

        state.select(TopLevelTab.Todo)
        assertEquals(2, state.currentStack.size)
    }

    @Test
    fun selectRightTabSetsTabToRightMotion() {
        val state = newState(TopLevelTab.Feed)

        state.select(TopLevelTab.Notification)

        assertEquals(TopLevelTab.Notification, state.selectedTab)
        assertEquals(NavMotion.TabToRight, state.motion)
    }

    @Test
    fun selectLeftTabSetsTabToLeftMotion() {
        val state = newState(TopLevelTab.Profile)

        state.select(TopLevelTab.Todo)

        assertEquals(TopLevelTab.Todo, state.selectedTab)
        assertEquals(NavMotion.TabToLeft, state.motion)
    }

    @Test
    fun selectSameTabDoesNotChangeMotion() {
        val state = newState(TopLevelTab.Todo)
        state.select(TopLevelTab.Profile)
        assertEquals(NavMotion.TabToRight, state.motion)

        state.select(TopLevelTab.Profile)

        assertEquals(TopLevelTab.Profile, state.selectedTab)
        assertEquals(NavMotion.TabToRight, state.motion)
    }

    @Test
    fun pushSetsPushMotion() {
        val state = newState()
        state.select(TopLevelTab.Todo)
        assertEquals(NavMotion.TabToRight, state.motion)

        state.push(IssueDetailRoute("owner", "repo", 1))

        assertEquals(NavMotion.Push, state.motion)
    }

    @Test
    fun popOnlyAffectsSelectedStack() {
        val state = newState()
        state.push(IssueDetailRoute("owner", "repo", 1))
        state.select(TopLevelTab.Todo)
        state.push(IssueDetailRoute("owner", "repo", 2))

        state.pop()

        assertEquals(1, state.currentStack.size)
        state.select(TopLevelTab.Feed)
        assertEquals(2, state.currentStack.size)
    }
}
