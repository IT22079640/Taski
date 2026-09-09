package com.example.taski.progress

import java.util.Calendar
import java.util.TimeZone

data class DayRange(
    val startInclusive: Long,
    val endExclusive: Long
)

object LocalDates {
    fun dayRangeContaining(nowMillis: Long, timeZone: TimeZone): DayRange {
        val start = startOfDay(nowMillis, timeZone)
        val calendar = calendarAt(start, timeZone)
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        return DayRange(startInclusive = start, endExclusive = calendar.timeInMillis)
    }

    fun startOfDay(millis: Long, timeZone: TimeZone): Long {
        val calendar = calendarAt(millis, timeZone)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    fun previousStartOfDay(startOfDayMillis: Long, timeZone: TimeZone): Long {
        val calendar = calendarAt(startOfDayMillis, timeZone)
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        return calendar.timeInMillis
    }

    fun isSameDay(leftMillis: Long, rightMillis: Long, timeZone: TimeZone): Boolean =
        startOfDay(leftMillis, timeZone) == startOfDay(rightMillis, timeZone)

    private fun calendarAt(millis: Long, timeZone: TimeZone): Calendar {
        val calendar = Calendar.getInstance(timeZone)
        calendar.timeInMillis = millis
        return calendar
    }
}
