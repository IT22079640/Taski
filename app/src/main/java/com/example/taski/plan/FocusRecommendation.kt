package com.example.taski.plan

import com.example.taski.data.entity.Task
import com.example.taski.priority.PriorityLevel
import com.example.taski.progress.LocalDates
import com.example.taski.utils.DateUtils
import java.util.TimeZone

data class FocusRecommendation(
    val state: State,
    val recommendedTask: Task?,
    val whySummary: String,
    val orderedTasks: List<Task>,
    val plannedMinutes: Int = 0,
    val overflowTask: Task? = null
) {
    enum class State {
        CAUGHT_UP,
        RECOMMENDED,
        NOTHING_FITS
    }

    val isCaughtUp: Boolean get() = state == State.CAUGHT_UP
}

object FocusRecommendationBuilder {

    fun from(
        plan: FocusPlan,
        nowMillis: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): FocusRecommendation {
        if (plan.isCaughtUp) {
            return FocusRecommendation(
                state = FocusRecommendation.State.CAUGHT_UP,
                recommendedTask = null,
                whySummary = "",
                orderedTasks = emptyList(),
                plannedMinutes = 0,
                overflowTask = null
            )
        }
        val first = plan.items.firstOrNull()
        if (first == null) {
            return FocusRecommendation(
                state = FocusRecommendation.State.NOTHING_FITS,
                recommendedTask = null,
                whySummary = nothingFitsExplanation(),
                orderedTasks = emptyList(),
                plannedMinutes = 0,
                overflowTask = plan.nextOverflowTask
            )
        }
        return FocusRecommendation(
            state = FocusRecommendation.State.RECOMMENDED,
            recommendedTask = first.task,
            whySummary = whyRecommended(first.task, nowMillis, timeZone),
            orderedTasks = plan.selectedTasks,
            plannedMinutes = first.plannedMinutes,
            overflowTask = plan.nextOverflowTask
        )
    }

    fun nothingFitsExplanation(): String =
        "No more tasks fit within today's available time."

    internal fun whyRecommended(
        task: Task,
        nowMillis: Long,
        timeZone: TimeZone
    ): String {
        val level = PriorityLevel.fromScore(task.priorityScore)
        val days = LocalDates.calendarDaysUntil(task.deadline, nowMillis, timeZone)
        val priorityBit = when (level) {
            PriorityLevel.HIGH -> "High priority"
            PriorityLevel.MEDIUM -> "Medium priority"
            PriorityLevel.LOW -> "Lower priority"
        }
        val deadlineBit = when {
            days < 0 -> "overdue"
            days == 0 -> "due today"
            days == 1 -> "due tomorrow"
            days in 2..3 -> "due soon"
            else -> "due later"
        }
        return "$priorityBit and $deadlineBit. It fits your available focus time."
    }

    internal fun whySummary(
        task: Task,
        nowMillis: Long,
        timeZone: TimeZone
    ): String {
        val days = LocalDates.calendarDaysUntil(task.deadline, nowMillis, timeZone)
        val deadline = when {
            days < 0 -> "Overdue"
            days == 0 -> "Due today"
            days == 1 -> "Due tomorrow"
            days <= 7 -> "Due in $days days"
            else -> "Deadline further out"
        }
        return "$deadline • ${DateUtils.formatEffortHours(task.estimatedEffort)} estimated effort"
    }
}
