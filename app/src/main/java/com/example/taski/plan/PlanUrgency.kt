package com.example.taski.plan

import com.example.taski.data.entity.Task
import com.example.taski.priority.PriorityLevel
import com.example.taski.progress.LocalDates
import java.util.TimeZone

/**
 * Deterministic Plan-screen urgency: overdue, due today, or high AI priority.
 */
object PlanUrgency {
    fun isUrgent(
        task: Task,
        nowMillis: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): Boolean {
        val days = LocalDates.calendarDaysUntil(task.deadline, nowMillis, timeZone)
        val high = PriorityLevel.fromScore(task.priorityScore) == PriorityLevel.HIGH
        return days < 0 || days == 0 || high
    }

    fun label(
        task: Task,
        nowMillis: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        val days = LocalDates.calendarDaysUntil(task.deadline, nowMillis, timeZone)
        return when {
            days < 0 -> "Overdue"
            days == 0 -> "Due today"
            days == 1 -> "Due tomorrow"
            days in 2..3 -> "Due soon"
            else -> "Due later"
        }
    }
}
