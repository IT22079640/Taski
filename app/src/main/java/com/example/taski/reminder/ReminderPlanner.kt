package com.example.taski.reminder

/**
 * Deterministic reminder times from a deadline. Does not touch Android APIs.
 *
 * Incomplete future tasks get:
 * - an upcoming reminder 24 hours before the deadline when more than a day remains,
 *   or 1 hour before when less than a day but more than an hour remains
 * - a due reminder at the deadline
 *
 * Incomplete past deadlines are overdue (no future upcoming/due alarms).
 * Completed tasks receive nothing.
 */
object ReminderPlanner {
    const val DAY_MS = 24 * 60 * 60 * 1000L
    const val HOUR_MS = 60 * 60 * 1000L
    const val MINUTE_MS = 60 * 1000L

    fun plan(deadlineMillis: Long, nowMillis: Long, completed: Boolean): ReminderPlan {
        if (completed) return ReminderPlan()
        if (deadlineMillis <= nowMillis) {
            return ReminderPlan(overdueNow = true)
        }
        val upcomingAt = upcomingTriggerAt(deadlineMillis, nowMillis)
        return ReminderPlan(
            upcomingAt = upcomingAt,
            dueAt = deadlineMillis,
            overdueNow = false
        )
    }

    fun upcomingTriggerAt(deadlineMillis: Long, nowMillis: Long): Long? {
        val remaining = deadlineMillis - nowMillis
        if (remaining <= HOUR_MS) return null
        val candidate = if (remaining > DAY_MS) {
            deadlineMillis - DAY_MS
        } else {
            deadlineMillis - HOUR_MS
        }
        return candidate.takeIf { it > nowMillis }
    }

    fun dueTriggerAt(deadlineMillis: Long, nowMillis: Long, completed: Boolean): Long? {
        if (completed) return null
        if (deadlineMillis <= nowMillis) return null
        return deadlineMillis
    }

    fun isOverdue(deadlineMillis: Long, nowMillis: Long, completed: Boolean): Boolean {
        return !completed && deadlineMillis <= nowMillis
    }
}
