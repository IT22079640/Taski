package com.example.taski.utils

import android.content.Context
import android.text.format.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateUtils {
    const val DEFAULT_DEADLINE_HOUR = 18
    const val DEFAULT_DEADLINE_MINUTE = 0

    fun formatDisplay(millis: Long, timeZone: TimeZone = TimeZone.getDefault()): String {
        val formatter = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
        formatter.timeZone = timeZone
        return formatter.format(Date(millis))
    }

    fun formatTime(millis: Long, timeZone: TimeZone = TimeZone.getDefault()): String {
        val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
        formatter.timeZone = timeZone
        return formatter.format(Date(millis))
    }

    fun formatTime(hourOfDay: Int, minute: Int, timeZone: TimeZone = TimeZone.getDefault()): String {
        val calendar = Calendar.getInstance(timeZone)
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return formatTime(calendar.timeInMillis, timeZone)
    }

    fun formatDateTime(millis: Long, timeZone: TimeZone = TimeZone.getDefault()): String {
        return "${formatDisplay(millis, timeZone)}, ${formatTime(millis, timeZone)}"
    }

    fun is24HourFormat(context: Context): Boolean = DateFormat.is24HourFormat(context)

    /**
     * MaterialDatePicker returns UTC midnight for the selected calendar date.
     * Convert that to the start of the same calendar date in the given timezone
     * so display, priority, and reminders all treat "today" as the user's today.
     */
    fun utcMidnightToLocalStartOfDay(
        utcMidnightMillis: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Long {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        utc.timeInMillis = utcMidnightMillis
        val local = Calendar.getInstance(timeZone)
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

    fun localMillisToUtcPickerDate(
        millis: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Long {
        val local = Calendar.getInstance(timeZone)
        local.timeInMillis = millis
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        utc.clear()
        utc.set(
            local.get(Calendar.YEAR),
            local.get(Calendar.MONTH),
            local.get(Calendar.DAY_OF_MONTH),
            0,
            0,
            0
        )
        utc.set(Calendar.MILLISECOND, 0)
        return utc.timeInMillis
    }

    fun withLocalTime(
        millis: Long,
        hourOfDay: Int,
        minute: Int,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Long {
        val calendar = Calendar.getInstance(timeZone)
        calendar.timeInMillis = millis
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    fun combineUtcPickerDateWithLocalTime(
        utcMidnightMillis: Long,
        hourOfDay: Int,
        minute: Int,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Long {
        val localDate = utcMidnightToLocalStartOfDay(utcMidnightMillis, timeZone)
        return withLocalTime(localDate, hourOfDay, minute, timeZone)
    }

    fun localDateTimeMillis(
        year: Int,
        month: Int,
        day: Int,
        hourOfDay: Int,
        minute: Int,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Long {
        val calendar = Calendar.getInstance(timeZone)
        calendar.clear()
        calendar.set(year, month, day, hourOfDay, minute, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    fun hourOfDay(millis: Long, timeZone: TimeZone = TimeZone.getDefault()): Int {
        val calendar = Calendar.getInstance(timeZone)
        calendar.timeInMillis = millis
        return calendar.get(Calendar.HOUR_OF_DAY)
    }

    fun minuteOfHour(millis: Long, timeZone: TimeZone = TimeZone.getDefault()): Int {
        val calendar = Calendar.getInstance(timeZone)
        calendar.timeInMillis = millis
        return calendar.get(Calendar.MINUTE)
    }

    fun isLocalStartOfDay(millis: Long, timeZone: TimeZone = TimeZone.getDefault()): Boolean {
        val calendar = Calendar.getInstance(timeZone)
        calendar.timeInMillis = millis
        return calendar.get(Calendar.HOUR_OF_DAY) == 0 &&
            calendar.get(Calendar.MINUTE) == 0 &&
            calendar.get(Calendar.SECOND) == 0 &&
            calendar.get(Calendar.MILLISECOND) == 0
    }

    /**
     * Date-only deadlines were stored as local midnight. Map those to 6:00 PM
     * on the same local calendar day. Values that already have a time are kept.
     */
    fun withDefaultEveningIfDateOnly(
        millis: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Long {
        if (!isLocalStartOfDay(millis, timeZone)) return millis
        return withLocalTime(millis, DEFAULT_DEADLINE_HOUR, DEFAULT_DEADLINE_MINUTE, timeZone)
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
