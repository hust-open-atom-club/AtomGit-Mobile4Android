package com.hfad.atomgit.ui.issue

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IssueTimeTest {

    @Test
    fun formatIssueDisplayTime_includesAbsoluteAndRelative() {
        val now = 1_764_000_000_000L
        val created = "2025-11-20T10:00:00.000Z"
        val label = formatIssueDisplayTime(created, now)
        assertNotNull(label)
        assertTrue(label!!.contains("2025-11-20"))
        assertTrue(label.contains("（"))
    }

    @Test
    fun formatIssueAbsoluteTime_parsesIso() {
        assertEquals(
            "2025-11-20",
            formatIssueAbsoluteTime("2025-11-20T10:00:00.000Z")?.substringBefore(" "),
        )
    }
}