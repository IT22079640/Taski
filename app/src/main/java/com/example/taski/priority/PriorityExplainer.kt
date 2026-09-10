package com.example.taski.priority

import com.example.taski.data.entity.Importance
import com.example.taski.data.entity.Task

/**
 * Deterministic, rule-based copy for "Why this task?" and the per-task recommendation.
 * Uses [PriorityResult] from [PriorityCalculator] — it does not invent a second score.
 */
object PriorityExplainer {

    fun explain(task: Task, result: PriorityResult): PriorityExplanation {
        val why = whyThisTask(result, task.importance, task.estimatedEffort)
        if (task.completed) {
            return PriorityExplanation(
                whyThisTask = why,
                recommendationTitle = "Task complete",
                recommendationBody = "This task is already complete. Its score still shows how Taski ranked it."
            )
        }
        val recommendation = recommendation(result)
        return PriorityExplanation(
            whyThisTask = why,
            recommendationTitle = recommendation.first,
            recommendationBody = recommendation.second
        )
    }

    internal fun whyThisTask(
        result: PriorityResult,
        importance: Importance,
        estimatedEffortMinutes: Int
    ): String {
        val urgency = urgencyKind(result.reasons)
        return when (result.priorityLevel) {
            PriorityLevel.HIGH -> highWhy(urgency, importance, estimatedEffortMinutes)
            PriorityLevel.MEDIUM -> mediumWhy(urgency, importance)
            PriorityLevel.LOW -> lowWhy(urgency, importance)
        }
    }

    internal fun recommendation(result: PriorityResult): Pair<String, String> {
        val urgency = urgencyKind(result.reasons)
        return when (result.priorityLevel) {
            PriorityLevel.HIGH -> highRecommendation(urgency)
            PriorityLevel.MEDIUM -> Pair(
                "Schedule this after higher-priority work.",
                "Its deadline and importance indicate moderate urgency."
            )
            PriorityLevel.LOW -> Pair(
                "This can wait.",
                "This task can be deferred while you focus on more urgent or important work."
            )
        }
    }

    private fun highWhy(
        urgency: UrgencyKind,
        importance: Importance,
        estimatedEffortMinutes: Int
    ): String {
        val clauses = mutableListOf<String>()
        deadlineClause(urgency)?.let { clauses += it }
        when (importance) {
            Importance.HIGH -> clauses += "it has high importance"
            Importance.MEDIUM -> clauses += "it has notable importance"
            Importance.LOW -> Unit
        }
        if (estimatedEffortMinutes > MANAGEABLE_EFFORT_MAX_MINUTES) {
            clauses += "it requires significant effort"
        }
        if (clauses.isEmpty()) {
            clauses += "deadline, importance, and effort all contribute strongly"
        }
        return "This task is highly prioritized because ${joinAnd(clauses)}."
    }

    private fun mediumWhy(urgency: UrgencyKind, importance: Importance): String {
        val deadlineBit = when (urgency) {
            UrgencyKind.OVERDUE -> "It is overdue"
            UrgencyKind.TODAY -> "Its deadline is today"
            UrgencyKind.TOMORROW -> "Its deadline is tomorrow"
            UrgencyKind.CLOSE, UrgencyKind.APPROACHING -> "Its deadline is approaching"
            UrgencyKind.SEVERAL, UrgencyKind.DISTANT -> "Its deadline is not immediate"
        }
        val importanceBit = when (importance) {
            Importance.HIGH -> "it remains important"
            Importance.MEDIUM -> "it has moderate importance"
            Importance.LOW -> "it has lower importance"
        }
        return "This task should be scheduled after higher-priority work. " +
            "$deadlineBit and $importanceBit indicate moderate urgency."
    }

    private fun lowWhy(urgency: UrgencyKind, importance: Importance): String {
        val extra = when {
            urgency == UrgencyKind.DISTANT && importance == Importance.LOW ->
                " Its deadline is not soon and it is marked as lower importance."
            urgency == UrgencyKind.DISTANT || urgency == UrgencyKind.SEVERAL ->
                " Its deadline is not soon."
            importance == Importance.LOW ->
                " It is marked as lower importance."
            else -> ""
        }
        return "This task can be deferred while you focus on more urgent or important work.$extra"
    }

    private fun highRecommendation(urgency: UrgencyKind): Pair<String, String> {
        return when (urgency) {
            UrgencyKind.OVERDUE, UrgencyKind.TODAY, UrgencyKind.TOMORROW, UrgencyKind.CLOSE -> Pair(
                "Start this task today.",
                "Its deadline and importance make it a higher priority than your other pending tasks."
            )
            UrgencyKind.APPROACHING, UrgencyKind.SEVERAL, UrgencyKind.DISTANT -> Pair(
                "Start this task soon.",
                "Its deadline and importance/effort make it a strong priority."
            )
        }
    }

    private fun deadlineClause(urgency: UrgencyKind): String? = when (urgency) {
        UrgencyKind.OVERDUE -> "its deadline has passed"
        UrgencyKind.TODAY -> "its deadline is today"
        UrgencyKind.TOMORROW -> "its deadline is tomorrow"
        UrgencyKind.CLOSE, UrgencyKind.APPROACHING -> "its deadline is approaching"
        UrgencyKind.SEVERAL, UrgencyKind.DISTANT -> null
    }

    internal fun urgencyKind(reasons: List<String>): UrgencyKind {
        val urgency = reasons.firstOrNull().orEmpty()
        return when {
            urgency.contains("overdue", ignoreCase = true) -> UrgencyKind.OVERDUE
            urgency.contains("today", ignoreCase = true) -> UrgencyKind.TODAY
            urgency.contains("tomorrow", ignoreCase = true) -> UrgencyKind.TOMORROW
            urgency.contains("very close", ignoreCase = true) -> UrgencyKind.CLOSE
            urgency.contains("approaching", ignoreCase = true) -> UrgencyKind.APPROACHING
            urgency.contains("several days", ignoreCase = true) -> UrgencyKind.SEVERAL
            else -> UrgencyKind.DISTANT
        }
    }

    internal fun joinAnd(parts: List<String>): String = when (parts.size) {
        0 -> ""
        1 -> parts[0]
        2 -> "${parts[0]} and ${parts[1]}"
        else -> parts.dropLast(1).joinToString(", ") + ", and " + parts.last()
    }

    internal enum class UrgencyKind {
        OVERDUE,
        TODAY,
        TOMORROW,
        CLOSE,
        APPROACHING,
        SEVERAL,
        DISTANT
    }

    private const val MANAGEABLE_EFFORT_MAX_MINUTES = 180
}
