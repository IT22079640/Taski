package com.example.taski.priority

import com.example.taski.data.entity.Importance
import com.example.taski.data.entity.Task
import kotlin.math.roundToInt

/**
 * Offline rule-based priority engine.
 *
 * Weighted score:
 * - Urgency 40% (deadline proximity)
 * - Importance 40%
 * - Effort 20%
 *
 * Final [PriorityResult.score] is an integer in 0–100.
 *
 * Priority levels:
 * - High: 70–100
 * - Medium: 40–69
 * - Low: 0–39
 *
 * The same deadline, importance, effort, and [nowMillis] always produce the same score.
 */
class PriorityCalculator {

    fun calculate(task: Task, nowMillis: Long = System.currentTimeMillis()): PriorityResult {
        return calculate(task.deadline, task.importance, task.estimatedEffort, nowMillis)
    }

    fun calculate(
        deadlineMillis: Long,
        importance: Importance,
        estimatedEffortMinutes: Int,
        nowMillis: Long = System.currentTimeMillis()
    ): PriorityResult {
        val daysUntilDeadline = daysUntil(deadlineMillis, nowMillis)
        val urgency = urgencyScore(daysUntilDeadline)
        val importanceScore = importanceScore(importance)
        val effort = effortScore(estimatedEffortMinutes)

        val weighted = urgency * WEIGHT_URGENCY +
            importanceScore * WEIGHT_IMPORTANCE +
            effort * WEIGHT_EFFORT
        val score = weighted.roundToInt().coerceIn(SCORE_MIN, SCORE_MAX)

        return PriorityResult(
            score = score,
            priorityLevel = priorityLevel(score),
            reasons = listOf(
                urgencyReason(daysUntilDeadline),
                importanceReason(importance),
                effortReason(estimatedEffortMinutes)
            ),
            urgencyScore = urgency,
            importanceScore = importanceScore,
            effortScore = effort
        )
    }

    private fun daysUntil(deadlineMillis: Long, nowMillis: Long): Int {
        return ((deadlineMillis / DAY_MS) - (nowMillis / DAY_MS)).toInt()
    }

    private fun urgencyScore(daysUntilDeadline: Int): Int = when {
        daysUntilDeadline < 0 -> 100
        daysUntilDeadline == 0 -> 95
        daysUntilDeadline == 1 -> 88
        daysUntilDeadline in 2..3 -> 75
        daysUntilDeadline in 4..7 -> 55
        daysUntilDeadline in 8..14 -> 32
        else -> 12
    }

    private fun importanceScore(importance: Importance): Int = when (importance) {
        Importance.HIGH -> 100
        Importance.MEDIUM -> 58
        Importance.LOW -> 22
    }

    /**
     * Moderate effort scores highest in this factor. Very small tasks get a smaller
     * boost so they are not automatically ranked above important or urgent work.
     */
    private fun effortScore(estimatedEffortMinutes: Int): Int {
        val minutes = estimatedEffortMinutes.coerceAtLeast(0)
        return when {
            minutes <= 60 -> 72
            minutes <= 180 -> 88
            minutes <= 360 -> 52
            else -> 30
        }
    }

    private fun urgencyReason(daysUntilDeadline: Int): String = when {
        daysUntilDeadline < 0 -> "This task is overdue"
        daysUntilDeadline == 0 -> "Deadline is today"
        daysUntilDeadline == 1 -> "Deadline is tomorrow"
        daysUntilDeadline in 2..3 -> "Deadline is very close"
        daysUntilDeadline in 4..7 -> "Deadline is approaching"
        daysUntilDeadline in 8..14 -> "Deadline is several days away"
        else -> "Deadline is not soon"
    }

    private fun importanceReason(importance: Importance): String = when (importance) {
        Importance.HIGH -> "High importance"
        Importance.MEDIUM -> "Medium importance"
        Importance.LOW -> "Low importance"
    }

    private fun effortReason(estimatedEffortMinutes: Int): String {
        val minutes = estimatedEffortMinutes.coerceAtLeast(0)
        return when {
            minutes <= 60 -> "Estimated effort is low, so it can be finished quickly"
            minutes <= 180 -> "Estimated effort is manageable"
            minutes <= 360 -> "Estimated effort is substantial"
            else -> "Estimated effort is high"
        }
    }

    private fun priorityLevel(score: Int): PriorityLevel = when {
        score >= LEVEL_HIGH_MIN -> PriorityLevel.HIGH
        score >= LEVEL_MEDIUM_MIN -> PriorityLevel.MEDIUM
        else -> PriorityLevel.LOW
    }

    companion object {
        const val WEIGHT_URGENCY = 0.40
        const val WEIGHT_IMPORTANCE = 0.40
        const val WEIGHT_EFFORT = 0.20
        const val SCORE_MIN = 0
        const val SCORE_MAX = 100
        const val LEVEL_HIGH_MIN = 70
        const val LEVEL_MEDIUM_MIN = 40
        private const val DAY_MS = 86_400_000L
    }
}
