package com.example.taski.reminder

/**
 * Stable Int IDs so each task's reminder PendingIntents stay distinct.
 * UPCOMING and OVERDUE keep the previous request codes so leftover alarms
 * from older app versions are still cancelled on reschedule.
 */
object ReminderIds {
    fun notificationId(taskId: Long, kind: ReminderKind): Int {
        val bounded = (taskId and 0x3FFFFFFF).toInt()
        return when (kind) {
            ReminderKind.UPCOMING -> bounded shl 1
            ReminderKind.OVERDUE, ReminderKind.DUE -> (bounded shl 1) or 1
        }
    }
}
