package com.example.taski.integration

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.taski.data.database.TaskiDatabase
import com.example.taski.data.entity.Importance
import com.example.taski.data.repository.TaskRepository
import com.example.taski.plan.FocusPlanBuilder
import com.example.taski.plan.FocusRecommendationBuilder
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FocusPlanDataFlowIntegrationTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var db: TaskiDatabase
    private lateinit var repository: TaskRepository

    @Before
    fun setUp() {
        db = createInMemoryDb()
        repository = TaskRepository(db.taskDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun planFromPersistedTasks_excludesCompletedAndPrefersHigherPriority() = runBlocking {
        val now = System.currentTimeMillis()
        val highId = repository.insert(
            sampleTask(
                title = "High priority",
                deadline = now + DAY_MS,
                importance = Importance.HIGH,
                estimatedEffort = 60
            )
        )
        val mediumId = repository.insert(
            sampleTask(
                title = "Medium priority",
                deadline = now + 2 * DAY_MS,
                importance = Importance.MEDIUM,
                estimatedEffort = 60
            )
        )
        val lowId = repository.insert(
            sampleTask(
                title = "Low priority",
                deadline = now + 10 * DAY_MS,
                importance = Importance.LOW,
                estimatedEffort = 60
            )
        )
        val doneId = repository.insert(
            sampleTask(
                title = "Already done",
                deadline = now + DAY_MS,
                importance = Importance.HIGH,
                estimatedEffort = 30
            )
        )
        repository.setCompleted(doneId, true)

        val stored = repository.observeAllByPriority().awaitFirst()
        val plan = FocusPlanBuilder.build(
            stored,
            availableMinutes = 120,
            nowMillis = now
        )

        assertTrue(plan.selectedTasks.none { it.id == doneId || it.completed })
        assertEquals(listOf(highId, mediumId), plan.selectedTasks.map { it.id })
        assertTrue(plan.selectedTasks.none { it.id == lowId })
        assertEquals(120, plan.totalPlannedMinutes)
        assertTrue(!plan.exceedsAvailableTime)

        val recommendation = FocusRecommendationBuilder.from(plan)
        assertEquals(highId, recommendation.recommendedTask?.id)
        assertEquals(plan.selectedTasks.map { it.id }, recommendation.orderedTasks.map { it.id })
    }

    @Test
    fun planFromPersistedTasks_fallsBackWhenNothingFits() = runBlocking {
        val now = System.currentTimeMillis()
        val longHighId = repository.insert(
            sampleTask(
                title = "Long high",
                deadline = now + DAY_MS,
                importance = Importance.HIGH,
                estimatedEffort = 240
            )
        )
        repository.insert(
            sampleTask(
                title = "Longer medium",
                deadline = now + 3 * DAY_MS,
                importance = Importance.MEDIUM,
                estimatedEffort = 300
            )
        )

        val stored = repository.getIncomplete()
        val plan = FocusPlanBuilder.build(
            stored,
            availableMinutes = 30,
            nowMillis = now
        )

        assertEquals(listOf(longHighId), plan.selectedTasks.map { it.id })
        assertTrue(plan.exceedsAvailableTime)
        assertEquals(240, plan.totalPlannedMinutes)
    }
}
