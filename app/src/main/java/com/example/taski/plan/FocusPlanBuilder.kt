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
 * 2. Deadline urgency (overdue > today > tomorrow/soon > later)
 * 3. Stored AI priority score
 * 4. Smaller estimated effort as a tie-breaker
 * 5. Earlier deadline
 * 6. Stable id
 *
 * Packing then fills the daily capacity in that order without exceeding it.
 * Tasks that do not fit remaining time are left out of the plan.
 */
object FocusPlanBuilder {
    const val PRESET_THIRTY_MINUTES = 30
    const val PRESET_ONE_HOUR_MINUTES = 60
    const val PRESET_TWO_HOURS_MINUTES = 120
    const val PRESET_THREE_HOURS_MINUTES = 180
    const val PRESET_FOUR_HOURS_MINUTES = 240
    const val DEFAULT_AVAILABLE_MINUTES = PRESET_TWO_HOURS_MINUTES
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
                items = emptyList(),
                availableMinutes = budget,
                totalPlannedMinutes = 0,
                pendingCount = 0,
                remainingMinutes = budget,
                nextOverflowTask = null
            )
        }

        val ranked = pending.sortedWith(planComparator(nowMillis, timeZone))
        val selected = mutableListOf<PlannedTask>()
        var remaining = budget

        for (task in ranked) {
            val effort = task.estimatedEffort.coerceAtLeast(0)
            if (effort in 1..remaining) {
                selected += PlannedTask(task = task, plannedMinutes = effort)
                remaining -= effort
            }
        }

        val selectedIds = selected.map { it.task.id }.toSet()
        val overflow = ranked.firstOrNull { it.id !in selectedIds }

        return FocusPlan(
            items = selected.toList(),
            availableMinutes = budget,
            totalPlannedMinutes = selected.sumOf { it.plannedMinutes },
            pendingCount = pending.size,
            remainingMinutes = remaining,
            nextOverflowTask = overflow
        )
    }

    fun isPresetMinutes(minutes: Int): Boolean = minutes == PRESET_THIRTY_MINUTES ||
        minutes == PRESET_ONE_HOUR_MINUTES ||
        minutes == PRESET_TWO_HOURS_MINUTES ||
        minutes == PRESET_THREE_HOURS_MINUTES ||
        minutes == PRESET_FOUR_HOURS_MINUTES

    fun windowLabel(minutes: Int): String = when (minutes) {
        PRESET_THIRTY_MINUTES -> "30 minutes"
        PRESET_ONE_HOUR_MINUTES -> "1 hour"
        PRESET_TWO_HOURS_MINUTES -> "2 hours"
        PRESET_THREE_HOURS_MINUTES -> "3 hours"
        PRESET_FOUR_HOURS_MINUTES -> "4 hours"
        1 -> "1 minute"
        else -> if (minutes > 0 && minutes % 60 == 0) {
            "${minutes / 60} hours"
        } else {
            "$minutes minutes"
        }
    }

    fun windowAdjective(minutes: Int): String = when (minutes) {
        PRESET_THIRTY_MINUTES -> "30-minute"
        PRESET_ONE_HOUR_MINUTES -> "1-hour"
        PRESET_TWO_HOURS_MINUTES -> "2-hour"
        PRESET_THREE_HOURS_MINUTES -> "3-hour"
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
        nowMillis: Long,
        timeZone: TimeZone
    ): Comparator<Task> = compareByDescending<Task> {
        urgencyRank(it.deadline, nowMillis, timeZone)
    }
        .thenByDescending { it.priorityScore }
        .thenBy { it.estimatedEffort.coerceAtLeast(0) }
        .thenBy { it.deadline }
        .thenBy { it.id }

    private const val URGENCY_OVERDUE = 4
    private const val URGENCY_TODAY = 3
    private const val URGENCY_SOON = 2
    private const val URGENCY_LATER = 1
}
