package com.example.taski.progress

import com.example.taski.data.entity.Task
import com.example.taski.priority.PriorityCalculator
import com.example.taski.priority.PriorityLevel
import java.util.TimeZone

/**
 * Offline, rule-based productivity copy from stored Room statistics.
 */
object ProductivityInsightGenerator {

    const val HIGH_EFFORT_MINUTES = 180

    fun generate(stats: InsightStats): ProductivityInsight {
        if (stats.completedTasks == 0 && stats.completedFocusSessions == 0 && stats.totalFocusMillis == 0L) {
            return ProductivityInsight(
                title = "Keep going",
                body = "Complete a few tasks and focus sessions to unlock personalized productivity insights.",
                kind = ProductivityInsight.Kind.EMPTY
            )
        }
        if (stats.pendingTasks == 0 && stats.completedTasks > 0) {
            return ProductivityInsight(
                title = "Great progress!",
                body = "You completed your highest-priority tasks. Keep the momentum going.",
                kind = ProductivityInsight.Kind.CAUGHT_UP
            )
        }
        if (stats.overduePendingCount >= 1 || stats.highPriorityPendingCount >= 3) {
            val count = maxOf(stats.highPriorityPendingCount, stats.overduePendingCount)
            return ProductivityInsight(
                title = "Taski Recommendation",
                body = "You have $count high-priority or overdue tasks waiting. " +
                    "Consider starting with urgent work first.",
                kind = ProductivityInsight.Kind.URGENT
            )
        }
        if (stats.highEffortPendingCount >= 2) {
            return ProductivityInsight(
                title = "Taski Recommendation",
                body = "You have several high-effort tasks pending. " +
                    "Consider starting one early rather than leaving them close to their deadlines.",
                kind = ProductivityInsight.Kind.HIGH_EFFORT
            )
        }
        if (stats.completedFocusSessions >= 1 && stats.averageSessionMinutes in 1..19) {
            return ProductivityInsight(
                title = "Taski Insight",
                body = "Your average focus session is ${stats.averageSessionMinutes} minutes. " +
                    "Consider using focused sessions for large-effort tasks.",
                kind = ProductivityInsight.Kind.SHORT_FOCUS
            )
        }
        if (
            stats.recentCompletedCount >= 3 &&
            stats.recentHighPriorityCompletedCount * 2 >= stats.recentCompletedCount
        ) {
            return ProductivityInsight(
                title = "Taski Insight",
                body = "You completed ${stats.recentCompletedCount} tasks recently. " +
                    "Most were high-priority tasks.",
                kind = ProductivityInsight.Kind.STRONG_COMPLETION
            )
        }
        if (stats.completedFocusSessions >= 5 || stats.streakDays >= 3) {
            val body = if (stats.completedFocusSessions >= 5) {
                "You completed ${stats.completedFocusSessions} focus sessions. " +
                    "That consistency helps you finish important work."
            } else {
                "You have a ${stats.streakDays}-day completion streak. Keep the momentum going."
            }
            return ProductivityInsight(
                title = "Taski Insight",
                body = body,
                kind = ProductivityInsight.Kind.CONSISTENCY
            )
        }
        if (stats.completedTasks > 0) {
            return ProductivityInsight(
                title = "Taski Insight",
                body = "You completed ${stats.completedTasks} tasks. " +
                    "Keep using Today's Focus Plan to stay on track.",
                kind = ProductivityInsight.Kind.GENERAL
            )
        }
        return ProductivityInsight(
            title = "Taski Insight",
            body = if (stats.totalFocusMillis > 0) {
                "You logged focus time. Complete a task to see more productivity insights."
            } else {
                "You logged ${stats.completedFocusSessions} focus sessions. " +
                    "Complete a task to see more productivity insights."
            },
            kind = ProductivityInsight.Kind.GENERAL
        )
    }

    fun statsFrom(
        completedTasks: Int,
        pendingTasks: List<Task>,
        completedFocusSessions: Int,
        totalFocusMillis: Long,
        recentCompletedTasks: List<Task>,
        streakDays: Int,
        nowMillis: Long,
        timeZone: TimeZone
    ): InsightStats {
        val startOfToday = LocalDates.startOfDay(nowMillis, timeZone)
        val averageMinutes = if (completedFocusSessions > 0) {
            (totalFocusMillis / completedFocusSessions / 60_000L).toInt()
        } else {
            0
        }
        return InsightStats(
            completedTasks = completedTasks,
            pendingTasks = pendingTasks.size,
            highPriorityPendingCount = pendingTasks.count { isHighPriority(it) },
            overduePendingCount = pendingTasks.count { it.deadline < startOfToday },
            highEffortPendingCount = pendingTasks.count { it.estimatedEffort > HIGH_EFFORT_MINUTES },
            completedFocusSessions = completedFocusSessions,
            averageSessionMinutes = averageMinutes,
            recentCompletedCount = recentCompletedTasks.size,
            recentHighPriorityCompletedCount = recentCompletedTasks.count { isHighPriority(it) },
            streakDays = streakDays,
            totalFocusMillis = totalFocusMillis
        )
    }

    private fun isHighPriority(task: Task): Boolean =
        PriorityLevel.fromScore(task.priorityScore) == PriorityLevel.HIGH ||
            task.priorityScore >= PriorityCalculator.LEVEL_HIGH_MIN
}
