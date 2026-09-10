package com.example.taski.progress

import com.example.taski.data.entity.Importance
import com.example.taski.data.entity.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

class ProductivityInsightGeneratorTest {

    @Test
    fun focusTimeWithoutCompletions_isNotEmpty() {
        val insight = ProductivityInsightGenerator.generate(
            InsightStats(
                completedTasks = 0,
                pendingTasks = 1,
                highPriorityPendingCount = 0,
                overduePendingCount = 0,
                highEffortPendingCount = 0,
                completedFocusSessions = 0,
                averageSessionMinutes = 0,
                recentCompletedCount = 0,
                recentHighPriorityCompletedCount = 0,
                streakDays = 0,
                totalFocusMillis = 180_000L
            )
        )
        assertEquals(ProductivityInsight.Kind.GENERAL, insight.kind)
        assertTrue(insight.body.contains("focus time"))
    }

    @Test
    fun insufficientData_returnsEmptyInsight() {
        val insight = ProductivityInsightGenerator.generate(
            InsightStats(
                completedTasks = 0,
                pendingTasks = 2,
                highPriorityPendingCount = 1,
                overduePendingCount = 0,
                highEffortPendingCount = 0,
                completedFocusSessions = 0,
                averageSessionMinutes = 0,
                recentCompletedCount = 0,
                recentHighPriorityCompletedCount = 0,
                streakDays = 0
            )
        )
        assertEquals(ProductivityInsight.Kind.EMPTY, insight.kind)
        assertEquals("Keep going", insight.title)
        assertTrue(insight.body.contains("personalized productivity insights"))
    }

    @Test
    fun noPendingWithCompletions_celebratesProgress() {
        val insight = ProductivityInsightGenerator.generate(baseStats().copy(pendingTasks = 0, completedTasks = 4))
        assertEquals(ProductivityInsight.Kind.CAUGHT_UP, insight.kind)
        assertTrue(insight.body.contains("highest-priority"))
    }

    @Test
    fun manyHighPriorityPending_recommendsUrgentWork() {
        val insight = ProductivityInsightGenerator.generate(
            baseStats().copy(
                pendingTasks = 5,
                highPriorityPendingCount = 3,
                overduePendingCount = 0
            )
        )
        assertEquals(ProductivityInsight.Kind.URGENT, insight.kind)
        assertEquals("Taski Recommendation", insight.title)
        assertTrue(insight.body.contains("urgent"))
    }

    @Test
    fun manyHighEffortPending_recommendsStartingEarly() {
        val insight = ProductivityInsightGenerator.generate(
            baseStats().copy(
                pendingTasks = 3,
                highPriorityPendingCount = 0,
                overduePendingCount = 0,
                highEffortPendingCount = 2
            )
        )
        assertEquals(ProductivityInsight.Kind.HIGH_EFFORT, insight.kind)
        assertTrue(insight.body.contains("high-effort"))
    }

    @Test
    fun shortFocusSessions_recommendsLongerFocus() {
        val insight = ProductivityInsightGenerator.generate(
            baseStats().copy(
                pendingTasks = 1,
                highPriorityPendingCount = 0,
                overduePendingCount = 0,
                highEffortPendingCount = 0,
                completedFocusSessions = 2,
                averageSessionMinutes = 12,
                recentCompletedCount = 1,
                recentHighPriorityCompletedCount = 0
            )
        )
        assertEquals(ProductivityInsight.Kind.SHORT_FOCUS, insight.kind)
        assertTrue(insight.body.contains("12 minutes"))
    }

    @Test
    fun strongRecentCompletion_highlightsHighPriorityWins() {
        val insight = ProductivityInsightGenerator.generate(
            baseStats().copy(
                pendingTasks = 1,
                highPriorityPendingCount = 0,
                overduePendingCount = 0,
                highEffortPendingCount = 0,
                completedFocusSessions = 1,
                averageSessionMinutes = 27,
                recentCompletedCount = 6,
                recentHighPriorityCompletedCount = 5
            )
        )
        assertEquals(ProductivityInsight.Kind.STRONG_COMPLETION, insight.kind)
        assertTrue(insight.body.contains("6 tasks recently"))
        assertTrue(insight.body.contains("high-priority"))
    }

    @Test
    fun statsFrom_countsOverdueHighPriorityAndEffort() {
        val utc = TimeZone.getTimeZone("UTC")
        val now = 1_746_489_600_000L
        val pending = listOf(
            task(score = 80, effort = 60, deadline = now - 86_400_000L),
            task(score = 40, effort = 240, deadline = now + 5 * 86_400_000L),
            task(score = 75, effort = 300, deadline = now + 86_400_000L)
        )
        val stats = ProductivityInsightGenerator.statsFrom(
            completedTasks = 2,
            pendingTasks = pending,
            completedFocusSessions = 1,
            totalFocusMillis = 25 * 60_000L,
            recentCompletedTasks = listOf(task(score = 80, effort = 30, deadline = now)),
            streakDays = 1,
            nowMillis = now,
            timeZone = utc
        )
        assertEquals(3, stats.pendingTasks)
        assertEquals(2, stats.highPriorityPendingCount)
        assertEquals(1, stats.overduePendingCount)
        assertEquals(2, stats.highEffortPendingCount)
        assertEquals(25, stats.averageSessionMinutes)
        assertEquals(1, stats.recentHighPriorityCompletedCount)
    }

    private fun baseStats(): InsightStats = InsightStats(
        completedTasks = 4,
        pendingTasks = 2,
        highPriorityPendingCount = 0,
        overduePendingCount = 0,
        highEffortPendingCount = 0,
        completedFocusSessions = 1,
        averageSessionMinutes = 25,
        recentCompletedCount = 2,
        recentHighPriorityCompletedCount = 1,
        streakDays = 1
    )

    private fun task(score: Int, effort: Int, deadline: Long): Task = Task(
        title = "Task",
        deadline = deadline,
        importance = Importance.MEDIUM,
        estimatedEffort = effort,
        category = "Study",
        priorityScore = score
    )
}
