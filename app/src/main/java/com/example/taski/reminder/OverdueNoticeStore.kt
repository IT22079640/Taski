package com.example.taski.reminder

import android.content.Context
import android.content.SharedPreferences

/**
 * Remembers that an overdue notification was already posted for a task so
 * app launch / reschedule does not spam the same overdue notice.
 */
class OverdueNoticeStore(private val prefs: SharedPreferences) {

    fun wasNotified(taskId: Long): Boolean {
        if (taskId <= 0L) return false
        return prefs.getBoolean(key(taskId), false)
    }

    fun markNotified(taskId: Long) {
        if (taskId <= 0L) return
        prefs.edit().putBoolean(key(taskId), true).apply()
    }

    fun clear(taskId: Long) {
        if (taskId <= 0L) return
        prefs.edit().remove(key(taskId)).apply()
    }

    companion object {
        const val PREFS_NAME = "taski_prefs"
        private const val KEY_PREFIX = "overdue_notified_"

        fun from(context: Context): OverdueNoticeStore =
            OverdueNoticeStore(
                context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            )

        internal fun key(taskId: Long): String = KEY_PREFIX + taskId
    }
}
