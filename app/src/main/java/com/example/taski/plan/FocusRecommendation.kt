package com.example.taski.plan

import com.example.taski.data.entity.Importance
import com.example.taski.data.entity.Task
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
        val state = if (plan.exceedsAvailableTime) {
            FocusRecommendation.State.OVERFLOW
        } else {
            FocusRecommendation.State.RECOMMENDED
        }
        return FocusRecommendation(
            state = state,
            recommendedTask = recommended,
            whySummary = whySummary(recommended, nowMillis, timeZone),
            orderedTasks = plan.selectedTasks
        )
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
        val importance = when (task.importance) {
            Importance.HIGH -> "High importance"
            Importance.MEDIUM -> "Medium importance"
            Importance.LOW -> "Low importance"
        }
        val effort = "${DateUtils.formatEffortHours(task.estimatedEffort)} estimated effort"
        return "$deadline • $importance • $effort"
    }
}
