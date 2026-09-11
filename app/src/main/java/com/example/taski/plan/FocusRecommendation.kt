package com.example.taski.plan

import com.example.taski.data.entity.Task
import com.example.taski.priority.PriorityExplainer
import com.example.taski.priority.PriorityLevel
import com.example.taski.progress.LocalDates
import com.example.taski.utils.DateUtils
import java.util.TimeZone

data class FocusRecommendation(
    val state: State,
    val recommendedTask: Task?,
    val whySummary: String,
    val orderedTasks: List<Task>
) {
    enum class State {
        CAUGHT_UP,
        RECOMMENDED,
        OVERFLOW
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
                orderedTasks = emptyList()
            )
        }
        val recommended = plan.selectedTasks.first()
        val overflow = plan.exceedsAvailableTime
        return FocusRecommendation(
            state = if (overflow) {
                FocusRecommendation.State.OVERFLOW
            } else {
                FocusRecommendation.State.RECOMMENDED
            },
            recommendedTask = recommended,
            whySummary = if (overflow) {
                overflowExplanation(plan.availableMinutes)
            } else {
                whyRecommended(recommended, plan.availableMinutes, nowMillis, timeZone)
            },
            orderedTasks = plan.selectedTasks
        )
    }

    fun overflowExplanation(availableMinutes: Int): String =
        "No tasks fit within ${FocusPlanBuilder.windowLabel(availableMinutes)}. " +
            "Taski recommends your highest-priority pending task."

    internal fun whyRecommended(
        task: Task,
        availableMinutes: Int,
        nowMillis: Long,
        timeZone: TimeZone
    ): String {
        val level = PriorityLevel.fromScore(task.priorityScore)
        val days = LocalDates.calendarDaysUntil(task.deadline, nowMillis, timeZone)
        val priorityBit = when (level) {
            PriorityLevel.HIGH -> "it is high priority"
            PriorityLevel.MEDIUM -> "it is medium priority"
            PriorityLevel.LOW -> "it is lower priority"
        }
        val deadlineBit = when {
            days < 0 -> "overdue"
            days == 0 -> "due today"
            days == 1 -> "due tomorrow"
            days in 2..3 -> "due soon"
            else -> "due later"
        }
        val fitBit = "fits your ${FocusPlanBuilder.windowAdjective(availableMinutes)} focus window"
        return "Recommended because ${PriorityExplainer.joinAnd(listOf(priorityBit, deadlineBit, fitBit))}."
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
