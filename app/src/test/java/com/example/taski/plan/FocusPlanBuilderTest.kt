package com.example.taski.plan

import com.example.taski.data.entity.Importance
import com.example.taski.data.entity.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

class FocusPlanBuilderTest {

    private val now = 1_746_489_600_000L
    private val utc: TimeZone = TimeZone.getTimeZone("UTC")
    private val dayMs = 86_400_000L

    @Test
    fun noPendingTasks_returnsCaughtUpPlan() {
        val completed = task(id = 1, score = 90, effortMinutes = 60, completed = true)
        val plan = planOf(listOf(completed), availableMinutes = 120)

        assertTrue(plan.isCaughtUp)
        assertTrue(plan.selectedTasks.isEmpty())
        assertEquals(0, plan.taskCount)
        assertEquals(0, plan.totalPlannedMinutes)
        assertEquals(120, plan.availableMinutes)
    }

    @Test
    fun emptyList_returnsCaughtUpPlan() {
        val plan = planOf(emptyList(), availableMinutes = 60)
        assertTrue(plan.isCaughtUp)
        assertEquals(60, plan.availableMinutes)
    }

    @Test
    fun highPriorityDueToday_beatsMediumLaterEvenIfLaterFillsTheHour() {
        val urgent = task(
            id = 1,
            score = 85,
            effortMinutes = 30,
            deadline = now,
            title = "Task A"
        )
        val later = task(
            id = 2,
            score = 65,
            effortMinutes = 60,
            deadline = now + 10 * dayMs,
            title = "Task B"
        )

        val plan = planOf(listOf(later, urgent), availableMinutes = 60)
        val recommendation = FocusRecommendationBuilder.from(plan, now, utc)

        assertEquals(listOf(1L), plan.selectedTasks.map { it.id })
        assertEquals(30, plan.totalPlannedMinutes)
        assertFalse(plan.exceedsAvailableTime)
        assertEquals(1L, recommendation.recommendedTask?.id)
        assertTrue(recommendation.whySummary.contains("high priority"))
        assertTrue(recommendation.whySummary.contains("due today"))
        assertTrue(recommendation.whySummary.contains("1-hour"))
    }

    @Test
    fun shorterTaskThatFits_isPreferredOverLongerTaskThatDoesNot() {
        val shortTask = task(id = 1, score = 40, effortMinutes = 20, deadline = now + 5 * dayMs)
        val longTask = task(id = 2, score = 90, effortMinutes = 60, deadline = now + 5 * dayMs)

        val plan = planOf(listOf(longTask, shortTask), availableMinutes = 30)

        assertEquals(listOf(1L), plan.selectedTasks.map { it.id })
        assertEquals(20, plan.totalPlannedMinutes)
        assertFalse(plan.exceedsAvailableTime)
    }

    @Test
    fun oversizedHighScore_isSkippedWhenAFittingTaskExists() {
        val oversized = task(id = 1, score = 95, effortMinutes = 90, deadline = now)
        val fitting = task(id = 2, score = 50, effortMinutes = 30, deadline = now + 8 * dayMs)

        val plan = planOf(listOf(oversized, fitting), availableMinutes = 60)

        assertEquals(listOf(2L), plan.selectedTasks.map { it.id })
        assertFalse(plan.exceedsAvailableTime)
    }

    @Test
    fun noneFit_fallsBackToHighestPriorityPendingTask() {
        val highest = task(id = 1, score = 88, effortMinutes = 240)
        val next = task(id = 2, score = 70, effortMinutes = 180)

        val plan = planOf(listOf(next, highest), availableMinutes = 30)
        val recommendation = FocusRecommendationBuilder.from(plan, now, utc)

        assertEquals(listOf(1L), plan.selectedTasks.map { it.id })
        assertTrue(plan.exceedsAvailableTime)
        assertEquals(FocusRecommendation.State.OVERFLOW, recommendation.state)
        assertTrue(recommendation.whySummary.contains("No tasks fit within 30 minutes"))
        assertTrue(recommendation.whySummary.contains("highest-priority pending task"))
    }

    @Test
    fun completedTask_isNeverRecommended() {
        val done = task(id = 1, score = 99, effortMinutes = 20, completed = true, deadline = now)
        val pending = task(id = 2, score = 40, effortMinutes = 20, deadline = now + 10 * dayMs)

        val plan = planOf(listOf(done, pending), availableMinutes = 60)
        val recommendation = FocusRecommendationBuilder.from(plan, now, utc)

        assertEquals(listOf(2L), plan.selectedTasks.map { it.id })
        assertEquals(2L, recommendation.recommendedTask?.id)
        assertFalse(plan.selectedTasks.any { it.completed })
    }

    @Test
    fun changingAvailableTimeFromOneHourToTwoHours_recalculatesPlan() {
        val shortTask = task(id = 1, score = 80, effortMinutes = 30, deadline = now + 6 * dayMs)
        val longTask = task(id = 2, score = 85, effortMinutes = 90, deadline = now + 6 * dayMs)
        val tasks = listOf(shortTask, longTask)

        val oneHour = planOf(tasks, availableMinutes = 60)
        val twoHours = planOf(tasks, availableMinutes = 120)

        assertEquals(listOf(1L), oneHour.selectedTasks.map { it.id })
        assertEquals(listOf(2L, 1L), twoHours.selectedTasks.map { it.id })
        assertEquals(30, oneHour.totalPlannedMinutes)
        assertEquals(120, twoHours.totalPlannedMinutes)
    }

    @Test
    fun selectsHighestPriorityFittingTasksThatPackIntoTheWindow() {
        val high = task(id = 1, score = 90, effortMinutes = 60, deadline = now)
        val medium = task(id = 2, score = 70, effortMinutes = 60, deadline = now)
        val low = task(id = 3, score = 40, effortMinutes = 60, deadline = now)

        val plan = planOf(listOf(low, high, medium), availableMinutes = 120)

        assertEquals(listOf(1L, 2L), plan.selectedTasks.map { it.id })
        assertEquals(120, plan.totalPlannedMinutes)
        assertFalse(plan.exceedsAvailableTime)
    }

    @Test
    fun skipsOversizedHigherPriorityToFitLaterTasks() {
        val hugeHigh = task(id = 1, score = 95, effortMinutes = 240, deadline = now)
        val fitMedium = task(id = 2, score = 70, effortMinutes = 60, deadline = now)
        val fitLow = task(id = 3, score = 50, effortMinutes = 45, deadline = now)

        val plan = planOf(listOf(hugeHigh, fitMedium, fitLow), availableMinutes = 120)

        assertEquals(listOf(2L, 3L), plan.selectedTasks.map { it.id })
        assertEquals(105, plan.totalPlannedMinutes)
    }

    @Test
    fun packsMultipleSmallTasksInsteadOfOneLargerTask() {
        val a = task(id = 1, score = 80, effortMinutes = 25, deadline = now)
        val b = task(id = 2, score = 75, effortMinutes = 30, deadline = now)
        val c = task(id = 3, score = 70, effortMinutes = 60, deadline = now)

        val plan = planOf(listOf(c, b, a), availableMinutes = 60)

        assertEquals(listOf(1L, 2L), plan.selectedTasks.map { it.id })
        assertEquals(55, plan.totalPlannedMinutes)
        assertFalse(plan.exceedsAvailableTime)
    }

    @Test
    fun doesNotModifyStoredPriorityScores() {
        val original = task(id = 1, score = 73, effortMinutes = 90)
        val snapshot = original.copy()

        planOf(listOf(original), availableMinutes = 120)

        assertEquals(snapshot, original)
        assertEquals(73, original.priorityScore)
    }

    @Test
    fun sameInputs_produceTheSamePlan() {
        val tasks = listOf(
            task(id = 3, score = 60, effortMinutes = 45, deadline = now + 3 * dayMs),
            task(id = 1, score = 80, effortMinutes = 60, deadline = now),
            task(id = 2, score = 80, effortMinutes = 30, deadline = now + dayMs)
        )

        val first = planOf(tasks, availableMinutes = 90)
        val second = planOf(tasks, availableMinutes = 90)

        assertEquals(first, second)
        assertEquals(listOf(1L, 2L), first.selectedTasks.map { it.id })
    }

    @Test
    fun customAvailableTime_isHonoredAndCapped() {
        val shortTask = task(id = 1, score = 50, effortMinutes = 90)
        val plan = planOf(listOf(shortTask), availableMinutes = 150)

        assertEquals(150, plan.availableMinutes)
        assertEquals(listOf(1L), plan.selectedTasks.map { it.id })

        val capped = planOf(listOf(shortTask), availableMinutes = 10_000)
        assertEquals(FocusPlanBuilder.MAX_AVAILABLE_MINUTES, capped.availableMinutes)
    }

    @Test
    fun overdueTask_ranksAheadOfDueTodayWhenBothFit() {
        val overdue = task(id = 1, score = 70, effortMinutes = 20, deadline = now - dayMs)
        val dueToday = task(id = 2, score = 90, effortMinutes = 20, deadline = now)

        val plan = planOf(listOf(dueToday, overdue), availableMinutes = 60)

        assertEquals(listOf(1L, 2L), plan.selectedTasks.map { it.id })
    }

    @Test
    fun longTitle_isStillRecommendedWhenItFits() {
        val title = "Prepare the complete database assignment write-up including diagrams and citations"
        val longTitle = task(id = 1, score = 82, effortMinutes = 25, deadline = now, title = title)
        val other = task(id = 2, score = 50, effortMinutes = 25, deadline = now + 8 * dayMs)

        val plan = planOf(listOf(other, longTitle), availableMinutes = 60)
        val recommendation = FocusRecommendationBuilder.from(plan, now, utc)

        assertEquals(title, recommendation.recommendedTask?.title)
    }

    @Test
    fun defaultAvailableTime_isOneHour() {
        assertEquals(60, FocusPlanBuilder.DEFAULT_AVAILABLE_MINUTES)
    }

    private fun planOf(tasks: List<Task>, availableMinutes: Int) =
        FocusPlanBuilder.build(tasks, availableMinutes, now, utc)

    private fun task(
        id: Long,
        score: Int,
        effortMinutes: Int,
        deadline: Long = now + dayMs,
        importance: Importance = Importance.MEDIUM,
        completed: Boolean = false,
        title: String = "Task $id"
    ): Task = Task(
        id = id,
        title = title,
        deadline = deadline,
        importance = importance,
        estimatedEffort = effortMinutes,
        category = "Study",
        priorityScore = score,
        completed = completed
    )
}
