package com.example.taski.progress

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class StreakCalculatorTest {

    private val utc: TimeZone = TimeZone.getTimeZone("UTC")

    @Test
    fun emptyCompletions_haveZeroStreak() {
        assertEquals(0, StreakCalculator.currentStreak(emptyList(), utcMillis(2026, 9, 9), utc))
    }

    @Test
    fun todayProductive_countsConsecutiveDaysEndingToday() {
        val times = listOf(
            utcMillis(2026, 9, 7, 8),
            utcMillis(2026, 9, 8, 12),
            utcMillis(2026, 9, 9, 18)
        )
        assertEquals(3, StreakCalculator.currentStreak(times, utcMillis(2026, 9, 9, 20), utc))
    }

    @Test
    fun todayNotProductive_usesConsecutiveDaysEndingYesterday() {
        val times = listOf(
            utcMillis(2026, 9, 7, 9),
            utcMillis(2026, 9, 8, 9)
        )
        assertEquals(2, StreakCalculator.currentStreak(times, utcMillis(2026, 9, 9, 10), utc))
    }

    @Test
    fun gapBreaksStreak() {
        val times = listOf(
            utcMillis(2026, 9, 5, 9),
            utcMillis(2026, 9, 8, 9),
            utcMillis(2026, 9, 9, 9)
        )
        assertEquals(2, StreakCalculator.currentStreak(times, utcMillis(2026, 9, 9, 21), utc))
    }

    @Test
    fun todayAndYesterdayEmpty_streakIsZero() {
        val times = listOf(utcMillis(2026, 9, 6, 9))
        assertEquals(0, StreakCalculator.currentStreak(times, utcMillis(2026, 9, 9, 10), utc))
    }

    @Test
    fun multipleCompletionsSameDay_countAsOneDay() {
        val times = listOf(
            utcMillis(2026, 9, 9, 8),
            utcMillis(2026, 9, 9, 14),
            utcMillis(2026, 9, 9, 22)
        )
        assertEquals(1, StreakCalculator.currentStreak(times, utcMillis(2026, 9, 9, 23), utc))
    }

    @Test
    fun onlyYesterday_isOneDayStreak() {
        val times = listOf(utcMillis(2026, 9, 8, 16))
        assertEquals(1, StreakCalculator.currentStreak(times, utcMillis(2026, 9, 9, 8), utc))
    }

    private fun utcMillis(
        year: Int,
        month: Int,
        day: Int,
        hour: Int = 12
    ): Long {
        val calendar = Calendar.getInstance(utc)
        calendar.clear()
        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.MONTH, month - 1)
        calendar.set(Calendar.DAY_OF_MONTH, day)
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        return calendar.timeInMillis
    }
}

class FocusTimeFormatterTest {
    @Test
    fun zero_isZeroMinutes() {
        val parts = FocusTimeFormatter.parts(0L)
        assertEquals(0, parts.hours)
        assertEquals(0, parts.minutes)
        assertFalse(parts.underOneMinute)
    }

    @Test
    fun underOneMinute_usesShortLabel() {
        val parts = FocusTimeFormatter.parts(45_000L)
        assertTrue(parts.underOneMinute)
    }

    @Test
    fun shortDurations_areMinutesOnly() {
        val parts = FocusTimeFormatter.parts(25 * 60_000L)
        assertEquals(0, parts.hours)
        assertEquals(25, parts.minutes)
        assertFalse(parts.underOneMinute)
    }

    @Test
    fun exactHour_hasNoRemainingMinutes() {
        val parts = FocusTimeFormatter.parts(2 * 60 * 60_000L)
        assertEquals(2, parts.hours)
        assertEquals(0, parts.minutes)
    }

    @Test
    fun mixedHoursAndMinutes() {
        val parts = FocusTimeFormatter.parts((1 * 60 + 10) * 60_000L)
        assertEquals(1, parts.hours)
        assertEquals(10, parts.minutes)
    }
}

class LocalDatesTest {
    private val utc: TimeZone = TimeZone.getTimeZone("UTC")

    @Test
    fun dayRange_coversLocalDay() {
        val calendar = Calendar.getInstance(utc)
        calendar.clear()
        calendar.set(2026, Calendar.SEPTEMBER, 9, 15, 30, 0)
        val range = LocalDates.dayRangeContaining(calendar.timeInMillis, utc)

        val start = Calendar.getInstance(utc)
        start.timeInMillis = range.startInclusive
        assertEquals(0, start.get(Calendar.HOUR_OF_DAY))
        assertEquals(9, start.get(Calendar.DAY_OF_MONTH))

        val end = Calendar.getInstance(utc)
        end.timeInMillis = range.endExclusive
        assertEquals(10, end.get(Calendar.DAY_OF_MONTH))
        assertTrue(calendar.timeInMillis >= range.startInclusive)
        assertTrue(calendar.timeInMillis < range.endExclusive)
    }

    @Test
    fun previousStartOfDay_movesBackOneLocalDay() {
        val calendar = Calendar.getInstance(utc)
        calendar.clear()
        calendar.set(2026, Calendar.SEPTEMBER, 9, 15, 0, 0)
        val start = LocalDates.startOfDay(calendar.timeInMillis, utc)
        val previous = LocalDates.previousStartOfDay(start, utc)
        assertEquals(24 * 60 * 60 * 1000L, start - previous)
    }

    @Test
    fun calendarDaysUntil_sameLocalDayIsZero() {
        val pacific = TimeZone.getTimeZone("America/Los_Angeles")
        val now = localMillis(pacific, 2026, Calendar.SEPTEMBER, 10, 20)
        val deadline = localMillis(pacific, 2026, Calendar.SEPTEMBER, 10, 0)
        assertEquals(0, LocalDates.calendarDaysUntil(deadline, now, pacific))
    }

    @Test
    fun calendarDaysUntil_nextLocalDayIsOne() {
        val pacific = TimeZone.getTimeZone("America/Los_Angeles")
        val now = localMillis(pacific, 2026, Calendar.SEPTEMBER, 10, 20)
        val deadline = localMillis(pacific, 2026, Calendar.SEPTEMBER, 11, 0)
        assertEquals(1, LocalDates.calendarDaysUntil(deadline, now, pacific))
    }

    @Test
    fun calendarDaysUntil_previousLocalDayIsNegative() {
        val pacific = TimeZone.getTimeZone("America/Los_Angeles")
        val now = localMillis(pacific, 2026, Calendar.SEPTEMBER, 10, 8)
        val deadline = localMillis(pacific, 2026, Calendar.SEPTEMBER, 9, 0)
        assertEquals(-1, LocalDates.calendarDaysUntil(deadline, now, pacific))
    }

    private fun localMillis(
        timeZone: TimeZone,
        year: Int,
        month: Int,
        day: Int,
        hour: Int
    ): Long {
        val calendar = Calendar.getInstance(timeZone)
        calendar.clear()
        calendar.set(year, month, day, hour, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
