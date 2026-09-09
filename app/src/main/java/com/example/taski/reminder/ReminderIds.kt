package com.example.taski.reminder

/**
 * Stable Int IDs so each task's upcoming and overdue reminders stay distinct.
 */
object ReminderIds {
    fun notificationId(taskId: Long, kind: ReminderKind): Int {
        val bounded = (taskId and 0x3FFFFFFF).toInt()
        return when (kind) {
            ReminderKind.UPCOMING -> bounded shl 1
            ReminderKind.OVERDUE -> (bounded shl 1) or 1
        }
    }
}
