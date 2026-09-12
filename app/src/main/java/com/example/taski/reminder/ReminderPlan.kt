package com.example.taski.reminder

data class ReminderPlan(
    val upcomingAt: Long? = null,
    val dueAt: Long? = null,
    val overdueNow: Boolean = false
) {
    val hasAlarms: Boolean get() = upcomingAt != null || dueAt != null || overdueNow
}
