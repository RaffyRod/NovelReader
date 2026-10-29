package com.lagradost.quicknovel.util

import java.util.TimeZone

enum class DateGroup { TODAY, YESTERDAY, THIS_WEEK, EARLIER }

/** Puts a point in time into the section it belongs to in a list grouped by date. */
object DateGroups {
    private const val WEEK_DAYS = 7

    fun of(timeMs: Long, nowMs: Long, zone: TimeZone = TimeZone.getDefault()): DateGroup {
        val days = dayNumber(nowMs, zone) - dayNumber(timeMs, zone)
        return when {
            days <= 0 -> DateGroup.TODAY
            days == 1 -> DateGroup.YESTERDAY
            days < WEEK_DAYS -> DateGroup.THIS_WEEK
            else -> DateGroup.EARLIER
        }
    }

    /** Number of calendar days since the epoch, counted in [zone] so midnight is the boundary. */
    private fun dayNumber(timeMs: Long, zone: TimeZone): Int =
        Math.floorDiv(timeMs + zone.getOffset(timeMs), 86_400_000L).toInt()
}
