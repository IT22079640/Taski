package com.example.taski.plan

import com.example.taski.data.entity.Importance
import com.example.taski.data.entity.Task

/**
 * Builds today's focus plan from stored Room fields only.
 * Does not recalculate or write priority scores.
 */
object FocusPlanBuilder {
    const val PRESET_ONE_HOUR_MINUTES = 60
    const val PRESET_TWO_HOURS_MINUTES = 120
    const val PRESET_FOUR_HOURS_MINUTES = 240
    const val DEFAULT_AVAILABLE_MINUTES = PRESET_TWO_HOURS_MINUTES
    const val MAX_AVAILABLE_MINUTES = 24 * 60

    fun build(tasks: List<Task>, availableMinutes: Int): FocusPlan {
        val budget = availableMinutes.coerceIn(0, MAX_AVAILABLE_MINUTES)
        val pending = tasks.filter { !it.completed }
        if (pending.isEmpty()) {
            return FocusPlan(
                selectedTasks = emptyList(),
                availableMinutes = budget,
                totalPlannedMinutes = 0
            )
        }

        val ranked = pending.sortedWith(taskComparator)
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
            selected += ranked.first()
        }

        return FocusPlan(
            selectedTasks = selected.toList(),
            availableMinutes = budget,
            totalPlannedMinutes = selected.sumOf { it.estimatedEffort.coerceAtLeast(0) }
        )
    }

    fun isPresetMinutes(minutes: Int): Boolean = minutes == PRESET_ONE_HOUR_MINUTES ||
        minutes == PRESET_TWO_HOURS_MINUTES ||
        minutes == PRESET_FOUR_HOURS_MINUTES

    private val taskComparator = compareByDescending<Task> { it.priorityScore }
        .thenBy { it.deadline }
        .thenByDescending { importanceRank(it.importance) }
        .thenBy { it.estimatedEffort.coerceAtLeast(0) }
        .thenBy { it.id }

    private fun importanceRank(importance: Importance): Int = when (importance) {
        Importance.HIGH -> 3
        Importance.MEDIUM -> 2
        Importance.LOW -> 1
    }
}
