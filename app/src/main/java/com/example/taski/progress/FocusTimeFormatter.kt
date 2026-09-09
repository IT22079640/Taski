package com.example.taski.progress

data class FocusTimeParts(
    val hours: Int,
    val minutes: Int,
    val underOneMinute: Boolean
)

object FocusTimeFormatter {
    fun parts(durationMillis: Long): FocusTimeParts {
        val safeMillis = durationMillis.coerceAtLeast(0L)
        if (safeMillis == 0L) {
            return FocusTimeParts(hours = 0, minutes = 0, underOneMinute = false)
        }
        if (safeMillis < 60_000L) {
            return FocusTimeParts(hours = 0, minutes = 0, underOneMinute = true)
        }
        val totalMinutes = (safeMillis / 60_000L).toInt()
        return FocusTimeParts(
            hours = totalMinutes / 60,
            minutes = totalMinutes % 60,
            underOneMinute = false
        )
    }
}
