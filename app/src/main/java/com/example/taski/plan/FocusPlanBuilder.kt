package com.example.taski.plan

import com.example.taski.data.entity.Task
import com.example.taski.progress.LocalDates
import java.util.TimeZone

/**
 * Builds today's focus plan from stored Room fields only.
 * Does not recalculate or write priority scores.
 *
 * Ranking (deterministic):
 * 1. Pending tasks only
 * 2. Tasks that fit the remaining/available window before tasks that do not
 * 3. Deadline urgency (overdue > today > tomorrow/soon > later)
 * 4. Stored priority score
 * 5. Smaller estimated effort as a tie-breaker
 *
 * Packing then fills the window in that order. If nothing fits, the highest-score
 * pending task is returned as a fallback.
 */
object FocusPlanBuilder {
    const val PRESET_THIRTY_MINUTES = 30
    const val PRESET_ONE_HOUR_MINUTES = 60
    const val PRESET_TWO_HOURS_MINUTES = 120
    const val PRESET_FOUR_HOURS_MINUTES = 240
    const val DEFAULT_AVAILABLE_MINUTES = PRESET_ONE_HOUR_MINUTES
    const val MAX_AVAILABLE_MINUTES = 24 * 60

    fun build(
        tasks: List<Task>,
        availableMinutes: Int,
        nowMillis: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): FocusPlan {
        val budget = availableMinutes.coerceIn(0, MAX_AVAILABLE_MINUTES)
        val pending = tasks.filter { !it.completed }
        if (pending.isEmpty()) {
            return FocusPlan(
                selectedTasks = emptyList(),
                availableMinutes = budget,
                totalPlannedMinutes = 0
            )
        }

        val ranked = pending.sortedWith(planComparator(budget, nowMillis, timeZone))
        val selected = mutableListOf<Task>()
        var remaining = budget

        for (task in ranked) {
            val effort = task.estimatedEffort.coerceAtLeast(0)
            if (effort <= remaining) {
                selected += task
                remaining -= effort
            }
        }

        if (selected.isEmpty()) {
            selected += pending.sortedWith(fallbackComparator(nowMillis, timeZone)).first()
        }

        return FocusPlan(
            selectedTasks = selected.toList(),
            availableMinutes = budget,
            totalPlannedMinutes = selected.sumOf { it.estimatedEffort.coerceAtLeast(0) }
        )
    }

    fun isPresetMinutes(minutes: Int): Boolean = minutes == PRESET_THIRTY_MINUTES ||
        minutes == PRESET_ONE_HOUR_MINUTES ||
        minutes == PRESET_TWO_HOURS_MINUTES ||
        minutes == PRESET_FOUR_HOURS_MINUTES

    fun windowLabel(minutes: Int): String = when (minutes) {
        PRESET_THIRTY_MINUTES -> "30 minutes"
        PRESET_ONE_HOUR_MINUTES -> "1 hour"
        PRESET_TWO_HOURS_MINUTES -> "2 hours"
        PRESET_FOUR_HOURS_MINUTES -> "4 hours"
        else -> "$minutes minutes"
    }

    fun windowAdjective(minutes: Int): String = when (minutes) {
        PRESET_THIRTY_MINUTES -> "30-minute"
        PRESET_ONE_HOUR_MINUTES -> "1-hour"
        PRESET_TWO_HOURS_MINUTES -> "2-hour"
        PRESET_FOUR_HOURS_MINUTES -> "4-hour"
        else -> "$minutes-minute"
    }

    internal fun urgencyRank(
        deadlineMillis: Long,
        nowMillis: Long,
        timeZone: TimeZone
    ): Int {
        val days = LocalDates.calendarDaysUntil(deadlineMillis, nowMillis, timeZone)
        return when {
            days < 0 -> URGENCY_OVERDUE
            days == 0 -> URGENCY_TODAY
            days in 1..3 -> URGENCY_SOON
            else -> URGENCY_LATER
        }
    }

    private fun planComparator(
        budget: Int,
        nowMillis: Long,
        timeZone: TimeZone
    ): Comparator<Task> = compareByDescending<Task> { fits(it, budget) }
        .thenByDescending { urgencyRank(it.deadline, nowMillis, timeZone) }
        .thenByDescending { it.priorityScore }
        .thenBy { it.estimatedEffort.coerceAtLeast(0) }
        .thenBy { it.deadline }
        .thenBy { it.id }

    private fun fallbackComparator(
        nowMillis: Long,
        timeZone: TimeZone
    ): Comparator<Task> = compareByDescending<Task> { it.priorityScore }
        .thenByDescending { urgencyRank(it.deadline, nowMillis, timeZone) }
        .thenBy { it.deadline }
        .thenBy { it.id }

    private fun fits(task: Task, budget: Int): Boolean =
        budget > 0 && task.estimatedEffort.coerceAtLeast(0) <= budget

    private const val URGENCY_OVERDUE = 4
    private const val URGENCY_TODAY = 3
    private const val URGENCY_SOON = 2
    private const val URGENCY_LATER = 1
}
