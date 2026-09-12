package com.example.taski.plan

import com.example.taski.data.entity.Task

data class PlannedTask(
    val task: Task,
    val plannedMinutes: Int
)

data class FocusPlan(
    val items: List<PlannedTask>,
    val availableMinutes: Int,
    val totalPlannedMinutes: Int,
    val pendingCount: Int,
    val remainingMinutes: Int,
    val nextOverflowTask: Task? = null
) {
    val selectedTasks: List<Task> get() = items.map { it.task }

    val taskCount: Int get() = items.size

    val isCaughtUp: Boolean get() = pendingCount == 0

    val nothingFits: Boolean get() = pendingCount > 0 && items.isEmpty()

    val hasLeftoverTasks: Boolean get() = nextOverflowTask != null

    val exceedsAvailableTime: Boolean
        get() = items.isNotEmpty() && totalPlannedMinutes > availableMinutes
}
