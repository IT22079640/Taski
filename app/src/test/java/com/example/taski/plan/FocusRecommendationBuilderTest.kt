package com.example.taski.plan

import com.example.taski.data.entity.Importance
import com.example.taski.data.entity.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

class FocusRecommendationBuilderTest {

    private val now = 1_746_489_600_000L
    private val utc: TimeZone = TimeZone.getTimeZone("UTC")
    private val dayMs = 86_400_000L

    @Test
    fun noPendingTasks_returnsCaughtUp() {
        val plan = FocusPlanBuilder.build(emptyList(), availableMinutes = 120, nowMillis = now, timeZone = utc)
        val recommendation = FocusRecommendationBuilder.from(plan, now, utc)

        assertEquals(FocusRecommendation.State.CAUGHT_UP, recommendation.state)
        assertNull(recommendation.recommendedTask)
        assertTrue(recommendation.orderedTasks.isEmpty())
        assertTrue(recommendation.isCaughtUp)
    }

    @Test
    fun pendingTasks_recommendFirstTaskFromTheCalculatedPlan() {
        val high = task(id = 1, score = 90, effortMinutes = 60, title = "Database Assignment", deadline = now)
        val medium = task(id = 2, score = 70, effortMinutes = 60, title = "Mobile App Report", deadline = now)
        val low = task(id = 3, score = 40, effortMinutes = 60, title = "Research Presentation", deadline = now)
        val plan = FocusPlanBuilder.build(
            listOf(low, high, medium),
            availableMinutes = 180,
            nowMillis = now,
            timeZone = utc
        )
        val recommendation = FocusRecommendationBuilder.from(plan, now, utc)

        assertEquals(FocusRecommendation.State.RECOMMENDED, recommendation.state)
        assertEquals(1L, recommendation.recommendedTask?.id)
        assertEquals("Database Assignment", recommendation.recommendedTask?.title)
        assertEquals(plan.selectedTasks.map { it.id }, recommendation.orderedTasks.map { it.id })
        assertTrue(recommendation.whySummary.contains("high priority"))
        assertTrue(recommendation.whySummary.contains("due today"))
        assertTrue(recommendation.whySummary.contains("fits your"))
    }

    @Test
    fun overflowPlan_explainsFallbackToHighestPriority() {
        val highest = task(id = 8, score = 88, effortMinutes = 240, title = "Long lab")
        val next = task(id = 9, score = 70, effortMinutes = 180, title = "Essay")
        val plan = FocusPlanBuilder.build(
            listOf(next, highest),
            availableMinutes = 60,
            nowMillis = now,
            timeZone = utc
        )
        val recommendation = FocusRecommendationBuilder.from(plan, now, utc)

        assertEquals(FocusRecommendation.State.OVERFLOW, recommendation.state)
        assertEquals(8L, recommendation.recommendedTask?.id)
        assertEquals(listOf(8L), recommendation.orderedTasks.map { it.id })
        assertEquals(
            "No tasks fit within 1 hour. Taski recommends your highest-priority pending task.",
            recommendation.whySummary
        )
    }

    @Test
    fun whyRecommended_matchesDeterministicTemplate() {
        val task = task(
            id = 4,
            score = 85,
            effortMinutes = 30,
            deadline = now,
            importance = Importance.HIGH
        )
        val text = FocusRecommendationBuilder.whyRecommended(task, 60, now, utc)
        assertEquals(
            "Recommended because it is high priority, due today, and fits your 1-hour focus window.",
            text
        )
    }

    private fun task(
        id: Long,
        score: Int,
        effortMinutes: Int,
        title: String = "Task $id",
        deadline: Long = now + dayMs,
        importance: Importance = Importance.HIGH
    ): Task = Task(
        id = id,
        title = title,
        deadline = deadline,
        importance = importance,
        estimatedEffort = effortMinutes,
        category = "Study",
        priorityScore = score
    )
}
