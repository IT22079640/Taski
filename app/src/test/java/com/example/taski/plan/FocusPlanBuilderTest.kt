package com.example.taski.plan

import com.example.taski.data.entity.Importance
import com.example.taski.data.entity.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusPlanBuilderTest {

    @Test
    fun noPendingTasks_returnsCaughtUpPlan() {
        val completed = task(id = 1, score = 90, effortMinutes = 60, completed = true)
        val plan = FocusPlanBuilder.build(listOf(completed), availableMinutes = 120)

        assertTrue(plan.isCaughtUp)
        assertTrue(plan.selectedTasks.isEmpty())
        assertEquals(0, plan.taskCount)
        assertEquals(0, plan.totalPlannedMinutes)
        assertEquals(120, plan.availableMinutes)
    }

    @Test
    fun emptyList_returnsCaughtUpPlan() {
        val plan = FocusPlanBuilder.build(emptyList(), availableMinutes = 60)
        assertTrue(plan.isCaughtUp)
        assertEquals(60, plan.availableMinutes)
    }

    @Test
    fun selectsHighestPriorityTasksThatFit() {
        val high = task(id = 1, score = 90, effortMinutes = 60)
        val medium = task(id = 2, score = 70, effortMinutes = 60)
        val low = task(id = 3, score = 40, effortMinutes = 60)

        val plan = FocusPlanBuilder.build(listOf(low, high, medium), availableMinutes = 120)

        assertEquals(listOf(1L, 2L), plan.selectedTasks.map { it.id })
        assertEquals(120, plan.totalPlannedMinutes)
        assertFalse(plan.exceedsAvailableTime)
    }

    @Test
    fun skipsOversizedHigherPriorityToFitLaterTasks() {
        val hugeHigh = task(id = 1, score = 95, effortMinutes = 240)
        val fitMedium = task(id = 2, score = 70, effortMinutes = 60)
        val fitLow = task(id = 3, score = 50, effortMinutes = 45)

        val plan = FocusPlanBuilder.build(
            listOf(hugeHigh, fitMedium, fitLow),
            availableMinutes = 120
        )

        assertEquals(listOf(2L, 3L), plan.selectedTasks.map { it.id })
        assertEquals(105, plan.totalPlannedMinutes)
    }

    @Test
    fun noneFit_stillSelectsHighestPriorityTask() {
        val highest = task(id = 1, score = 88, effortMinutes = 240)
        val next = task(id = 2, score = 70, effortMinutes = 180)

        val plan = FocusPlanBuilder.build(listOf(next, highest), availableMinutes = 60)

        assertEquals(listOf(1L), plan.selectedTasks.map { it.id })
        assertTrue(plan.exceedsAvailableTime)
        assertEquals(240, plan.totalPlannedMinutes)
        assertEquals(60, plan.availableMinutes)
    }

    @Test
    fun zeroAvailableTime_stillSelectsHighestPriorityPendingTask() {
        val task = task(id = 4, score = 80, effortMinutes = 30)
        val plan = FocusPlanBuilder.build(listOf(task), availableMinutes = 0)

        assertEquals(listOf(4L), plan.selectedTasks.map { it.id })
        assertTrue(plan.exceedsAvailableTime)
    }

    @Test
    fun ignoresCompletedTasksEvenIfHigherScore() {
        val done = task(id = 1, score = 99, effortMinutes = 30, completed = true)
        val pending = task(id = 2, score = 40, effortMinutes = 30)

        val plan = FocusPlanBuilder.build(listOf(done, pending), availableMinutes = 60)

        assertEquals(listOf(2L), plan.selectedTasks.map { it.id })
    }

    @Test
    fun doesNotModifyStoredPriorityScores() {
        val original = task(id = 1, score = 73, effortMinutes = 90)
        val snapshot = original.copy()

        FocusPlanBuilder.build(listOf(original), availableMinutes = 120)

        assertEquals(snapshot, original)
        assertEquals(73, original.priorityScore)
    }

    @Test
    fun sameInputs_produceTheSamePlan() {
        val tasks = listOf(
            task(id = 3, score = 60, effortMinutes = 45, deadline = 3_000L),
            task(id = 1, score = 80, effortMinutes = 60, deadline = 1_000L),
            task(id = 2, score = 80, effortMinutes = 30, deadline = 2_000L)
        )

        val first = FocusPlanBuilder.build(tasks, availableMinutes = 90)
        val second = FocusPlanBuilder.build(tasks, availableMinutes = 90)

        assertEquals(first, second)
        assertEquals(listOf(1L, 2L), first.selectedTasks.map { it.id })
    }

    @Test
    fun equalScores_preferEarlierDeadlineThenHigherImportance() {
        val laterHigh = task(
            id = 1,
            score = 70,
            effortMinutes = 30,
            deadline = 5_000L,
            importance = Importance.HIGH
        )
        val soonerMedium = task(
            id = 2,
            score = 70,
            effortMinutes = 30,
            deadline = 1_000L,
            importance = Importance.MEDIUM
        )

        val plan = FocusPlanBuilder.build(listOf(laterHigh, soonerMedium), availableMinutes = 30)

        assertEquals(listOf(2L), plan.selectedTasks.map { it.id })
    }

    @Test
    fun customAvailableTime_isHonoredAndCapped() {
        val shortTask = task(id = 1, score = 50, effortMinutes = 90)
        val plan = FocusPlanBuilder.build(listOf(shortTask), availableMinutes = 150)

        assertEquals(150, plan.availableMinutes)
        assertEquals(listOf(1L), plan.selectedTasks.map { it.id })

        val capped = FocusPlanBuilder.build(listOf(shortTask), availableMinutes = 10_000)
        assertEquals(FocusPlanBuilder.MAX_AVAILABLE_MINUTES, capped.availableMinutes)
    }

    private fun task(
        id: Long,
        score: Int,
        effortMinutes: Int,
        deadline: Long = 1_000L + id,
        importance: Importance = Importance.MEDIUM,
        completed: Boolean = false
    ): Task = Task(
        id = id,
        title = "Task $id",
        deadline = deadline,
        importance = importance,
        estimatedEffort = effortMinutes,
        category = "Study",
        priorityScore = score,
        completed = completed
    )
}
