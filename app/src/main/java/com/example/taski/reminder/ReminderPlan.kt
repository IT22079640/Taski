package com.example.taski.reminder

data class ReminderPlan(
    val triggerAt: Long?
) {
    val hasAlarms: Boolean get() = triggerAt != null
}
