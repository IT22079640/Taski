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

    @Test
    fun noPendingTasks_returnsCaughtUp() {
        val plan = FocusPlanBuilder.build(emptyList(), availableMinutes = 120)
        val recommendation = FocusRecommendationBuilder.from(plan, now, utc)

        assertEquals(FocusRecommendation.State.CAUGHT_UP, recommendation.state)
        assertNull(recommendation.recommendedTask)
        assertTrue(recommendation.orderedTasks.isEmpty())
        assertTrue(recommendation.isCaughtUp)
    }

    @Test
    fun pendingTasks_recommendHighestPriorityFromPlanOrder() {
        val high = task(id = 1, score = 90, effortMinutes = 60, title = "Database Assignment")
        val medium = task(id = 2, score = 70, effortMinutes = 60, title = "Mobile App Report")
        val low = task(id = 3, score = 40, effortMinutes = 60, title = "Research Presentation")
        val plan = FocusPlanBuilder.build(listOf(low, high, medium), availableMinutes = 180)
        val recommendation = FocusRecommendationBuilder.from(plan, now, utc)

        assertEquals(FocusRecommendation.State.RECOMMENDED, recommendation.state)
        assertEquals(1L, recommendation.recommendedTask?.id)
        assertEquals("Database Assignment", recommendation.recommendedTask?.title)
        assertEquals(listOf(1L, 2L, 3L), recommendation.orderedTasks.map { it.id })
        assertTrue(recommendation.whySummary.contains("High importance"))
        assertTrue(recommendation.whySummary.contains("1h estimated effort"))
    }

    @Test
    fun overflowPlan_usesHighestPriorityTask() {
        val highest = task(id = 8, score = 88, effortMinutes = 240, title = "Long lab")
        val next = task(id = 9, score = 70, effortMinutes = 180, title = "Essay")
        val plan = FocusPlanBuilder.build(listOf(next, highest), availableMinutes = 60)
        val recommendation = FocusRecommendationBuilder.from(plan, now, utc)

        assertEquals(FocusRecommendation.State.OVERFLOW, recommendation.state)
        assertEquals(8L, recommendation.recommendedTask?.id)
        assertEquals(listOf(8L), recommendation.orderedTasks.map { it.id })
    }

    @Test
    fun whySummary_usesTomorrowDeadline() {
        val task = task(
            id = 4,
            score = 80,
            effortMinutes = 120,
            deadline = now + 86_400_000L,
            importance = Importance.HIGH
        )
        val summary = FocusRecommendationBuilder.whySummary(task, now, utc)
        assertEquals("Due tomorrow • High importance • 2h estimated effort", summary)
    }

    private fun task(
        id: Long,
        score: Int,
        effortMinutes: Int,
        title: String = "Task $id",
        deadline: Long = now + 86_400_000L,
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
