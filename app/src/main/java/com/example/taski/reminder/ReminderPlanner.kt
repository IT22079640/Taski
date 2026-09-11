package com.example.taski.reminder

/**
 * Deterministic reminder times from a deadline. Does not touch Android APIs.
 *
 * A reminder is scheduled only at the deadline itself, and only if that instant
 * is still in the future. Past deadlines stay overdue without a new alarm, so
 * Taski never fires an immediate/soon notification for already-late work.
 */
object ReminderPlanner {
    const val DAY_MS = 24 * 60 * 60 * 1000L
    const val HOUR_MS = 60 * 60 * 1000L
    const val MINUTE_MS = 60 * 1000L

    fun plan(deadlineMillis: Long, nowMillis: Long, completed: Boolean): ReminderPlan {
        return ReminderPlan(triggerAt = triggerAt(deadlineMillis, nowMillis, completed))
    }

    fun triggerAt(
        deadlineMillis: Long,
        nowMillis: Long,
        completed: Boolean = false
    ): Long? {
        if (completed) return null
        if (deadlineMillis <= nowMillis) return null
        return deadlineMillis
    }
}
