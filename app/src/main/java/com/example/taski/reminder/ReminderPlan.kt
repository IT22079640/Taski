package com.example.taski.reminder

data class ReminderPlan(
    val upcomingAt: Long?,
    val overdueAt: Long?
) {
    val hasAlarms: Boolean get() = upcomingAt != null || overdueAt != null
}
