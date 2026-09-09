package com.example.taski.progress

import com.example.taski.data.entity.FocusSessionWithTask
import com.example.taski.data.entity.Task

data class ProgressDashboard(
    val completedTasks: Int,
    val pendingTasks: Int,
    val totalFocusMillis: Long,
    val completedFocusSessions: Int,
    val todayCompletedTasks: Int,
    val todayFocusMillis: Long,
    val streakDays: Int,
    val recentCompletedTasks: List<Task>,
    val recentFocusSessions: List<FocusSessionWithTask>
) {
    val hasCompletedTasks: Boolean get() = completedTasks > 0
    val hasFocusSessions: Boolean get() = recentFocusSessions.isNotEmpty() || completedFocusSessions > 0 || totalFocusMillis > 0
    val hasRecentTaskActivity: Boolean get() = recentCompletedTasks.isNotEmpty()
    val hasRecentFocusActivity: Boolean get() = recentFocusSessions.isNotEmpty()
    val hasAnyActivity: Boolean get() = hasCompletedTasks || hasFocusSessions
}
