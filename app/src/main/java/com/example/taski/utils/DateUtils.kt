package com.example.taski.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateUtils {
    fun formatDisplay(millis: Long): String {
        return SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(millis))
    }

    /**
     * MaterialDatePicker returns UTC midnight for the selected calendar date.
     * Convert that to the start of the same calendar date in the local timezone
     * so display, priority, and reminders all treat "today" as the user's today.
     */
    fun utcMidnightToLocalStartOfDay(utcMidnightMillis: Long): Long {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        utc.timeInMillis = utcMidnightMillis
        val local = Calendar.getInstance()
        local.clear()
        local.set(
            utc.get(Calendar.YEAR),
            utc.get(Calendar.MONTH),
            utc.get(Calendar.DAY_OF_MONTH),
            0,
            0,
            0
        )
        local.set(Calendar.MILLISECOND, 0)
        return local.timeInMillis
    }

    fun formatEffortHours(minutes: Int): String {
        val hours = minutes / 60.0
        return if (minutes % 60 == 0) {
            "${minutes / 60}h"
        } else {
            String.format(Locale.getDefault(), "%.1fh", hours)
        }
    }

    fun hoursToMinutes(hours: Double): Int = (hours * 60.0).toInt()

    fun minutesToHoursText(minutes: Int): String {
        val hours = minutes / 60.0
        return if (minutes % 60 == 0) {
            (minutes / 60).toString()
        } else {
            String.format(Locale.getDefault(), "%.1f", hours)
        }
    }
}
