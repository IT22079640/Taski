package com.example.taski.plan

import com.example.taski.data.entity.Task

data class FocusPlan(
    val selectedTasks: List<Task>,
    val availableMinutes: Int,
    val totalPlannedMinutes: Int
) {
    val taskCount: Int get() = selectedTasks.size

    val isCaughtUp: Boolean get() = selectedTasks.isEmpty()

    val exceedsAvailableTime: Boolean
        get() = selectedTasks.isNotEmpty() && totalPlannedMinutes > availableMinutes
}
