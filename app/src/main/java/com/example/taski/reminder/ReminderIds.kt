package com.example.taski.reminder

/**
 * Stable Int IDs so each task's reminder PendingIntents stay distinct.
 *
 * UPCOMING and DUE keep the previous request codes so leftover alarms from
 * older app versions are still cancelled on reschedule.
 * OVERDUE uses a separate bit so it cannot overwrite the due alarm.
 */
object ReminderIds {
    private const val OVERDUE_FLAG = 0x40000000

    fun notificationId(taskId: Long, kind: ReminderKind): Int {
        val bounded = (taskId and 0x3FFFFFFF).toInt()
        return when (kind) {
            ReminderKind.UPCOMING -> bounded shl 1
            ReminderKind.DUE -> (bounded shl 1) or 1
            ReminderKind.OVERDUE -> OVERDUE_FLAG or bounded
        }
    }
}
