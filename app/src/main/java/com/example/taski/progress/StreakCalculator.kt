package com.example.taski.progress

import java.util.TimeZone

/**
 * A day is productive if at least one task was completed that local day.
 * Streak is consecutive productive days ending today, or yesterday if today is not productive.
 */
object StreakCalculator {
    fun currentStreak(
        completionTimes: List<Long>,
        nowMillis: Long,
        timeZone: TimeZone
    ): Int {
        val productiveDays = completionTimes
            .map { LocalDates.startOfDay(it, timeZone) }
            .toSet()
        if (productiveDays.isEmpty()) return 0

        val todayStart = LocalDates.startOfDay(nowMillis, timeZone)
        var cursor = if (todayStart in productiveDays) {
            todayStart
        } else {
            LocalDates.previousStartOfDay(todayStart, timeZone)
        }
        if (cursor !in productiveDays) return 0

        var streak = 0
        while (cursor in productiveDays) {
            streak++
            cursor = LocalDates.previousStartOfDay(cursor, timeZone)
        }
        return streak
    }
}
