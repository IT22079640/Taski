package com.example.taski.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateUtils {
    fun formatDisplay(millis: Long): String {
        return SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(millis))
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
