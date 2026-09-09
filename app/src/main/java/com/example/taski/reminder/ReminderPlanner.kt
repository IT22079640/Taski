package com.example.taski.reminder

/**
 * Deterministic reminder times from a deadline. Does not touch Android APIs.
 */
object ReminderPlanner {
    const val DAY_MS = 24 * 60 * 60 * 1000L
    const val TWO_HOURS_MS = 2 * 60 * 60 * 1000L
    const val HOUR_MS = 60 * 60 * 1000L
    const val THIRTY_MINUTES_MS = 30 * 60 * 1000L
    const val FIFTEEN_MINUTES_MS = 15 * 60 * 1000L
    const val TEN_MINUTES_MS = 10 * 60 * 1000L
    const val MINUTE_MS = 60 * 1000L

    fun plan(deadlineMillis: Long, nowMillis: Long, completed: Boolean): ReminderPlan {
        if (completed) {
            return ReminderPlan(upcomingAt = null, overdueAt = null)
        }
        return ReminderPlan(
            upcomingAt = upcomingAt(deadlineMillis, nowMillis),
            overdueAt = overdueAt(deadlineMillis, nowMillis)
        )
    }

    fun upcomingAt(deadlineMillis: Long, nowMillis: Long): Long? {
        val remaining = deadlineMillis - nowMillis
        if (remaining <= 0L) return null
        val trigger = when {
            remaining >= DAY_MS -> deadlineMillis - DAY_MS
            remaining >= TWO_HOURS_MS -> deadlineMillis - HOUR_MS
            remaining >= THIRTY_MINUTES_MS -> deadlineMillis - FIFTEEN_MINUTES_MS
            remaining >= TEN_MINUTES_MS -> nowMillis + MINUTE_MS
            else -> return null
        }
        return trigger.takeIf { it >= nowMillis && it < deadlineMillis }
    }

    fun overdueAt(deadlineMillis: Long, nowMillis: Long): Long? {
        return if (deadlineMillis > nowMillis) {
            deadlineMillis
        } else {
            nowMillis + MINUTE_MS
        }
    }
}
