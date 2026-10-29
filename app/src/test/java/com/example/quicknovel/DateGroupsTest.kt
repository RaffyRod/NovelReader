package com.example.quicknovel

import com.lagradost.quicknovel.util.DateGroup
import com.lagradost.quicknovel.util.DateGroups
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.TimeZone

class DateGroupsTest {
    private val utc = TimeZone.getTimeZone("UTC")
    private val hour = 3_600_000L
    private val day = 24 * hour
    private val now = 100 * day + 15 * hour

    @Test
    fun `earlier the same day is today`() {
        assertEquals(DateGroup.TODAY, DateGroups.of(100 * day + 1 * hour, now, utc))
    }

    @Test
    fun `just before midnight is yesterday`() {
        assertEquals(DateGroup.YESTERDAY, DateGroups.of(100 * day - 1, now, utc))
    }

    @Test
    fun `three days ago is this week`() {
        assertEquals(DateGroup.THIS_WEEK, DateGroups.of(97 * day + 5 * hour, now, utc))
    }

    @Test
    fun `a week ago or more is earlier`() {
        assertEquals(DateGroup.EARLIER, DateGroups.of(93 * day + 5 * hour, now, utc))
    }

    @Test
    fun `a time in the future counts as today`() {
        assertEquals(DateGroup.TODAY, DateGroups.of(now + 5 * day, now, utc))
    }

    @Test
    fun `the time zone decides where midnight is`() {
        val west = TimeZone.getTimeZone("GMT-04:00")
        val justAfterUtcMidnight = 100 * day + 1 * hour
        assertEquals(DateGroup.YESTERDAY, DateGroups.of(justAfterUtcMidnight, now, west))
        assertEquals(DateGroup.TODAY, DateGroups.of(justAfterUtcMidnight, now, utc))
    }
}
