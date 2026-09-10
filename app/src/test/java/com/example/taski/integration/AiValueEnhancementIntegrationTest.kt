package com.example.taski.integration

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.taski.data.database.TaskiDatabase
import com.example.taski.data.entity.FocusSession
import com.example.taski.data.entity.Importance
import com.example.taski.data.repository.FocusSessionRepository
import com.example.taski.data.repository.TaskRepository
import com.example.taski.plan.FocusPlanBuilder
import com.example.taski.plan.FocusRecommendation
import com.example.taski.plan.FocusRecommendationBuilder
import com.example.taski.priority.PriorityCalculator
import com.example.taski.priority.PriorityChangeExplainer
import com.example.taski.priority.PriorityExplainer
import com.example.taski.priority.PriorityLevel
import com.example.taski.progress.ProductivityInsight
import com.example.taski.progress.ProductivityInsightGenerator
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AiValueEnhancementIntegrationTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var db: TaskiDatabase
    private lateinit var taskRepository: TaskRepository
    private lateinit var focusSessionRepository: FocusSessionRepository
    private val calculator = PriorityCalculator()

    @Before
    fun setUp() {
        db = createInMemoryDb()
        taskRepository = TaskRepository(db.taskDao())
        focusSessionRepository = FocusSessionRepository(db.focusSessionDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun createExplainEditPlanAndInsight_useStoredRoomData() = runBlocking {
        val now = System.currentTimeMillis()
        val highId = taskRepository.insert(
            sampleTask(
                title = "Database Assignment",
                deadline = now + DAY_MS,
                importance = Importance.HIGH,
                estimatedEffort = 60
            )
        )
        val created = requireNotNull(taskRepository.getById(highId))
        val explained = taskRepository.explainPriority(created)
        val expected = calculator.calculate(created)
        assertEquals(expected.score, created.priorityScore)
        assertEquals(expected.priorityLevel, explained.priorityLevel)

        val detailsCopy = PriorityExplainer.explain(created, explained)
        assertEquals(explained.priorityLevel, PriorityLevel.fromScore(created.priorityScore))
        when (explained.priorityLevel) {
            PriorityLevel.HIGH -> assertTrue(detailsCopy.whyThisTask.contains("highly prioritized"))
            PriorityLevel.MEDIUM -> assertTrue(detailsCopy.whyThisTask.contains("higher-priority"))
            PriorityLevel.LOW -> assertTrue(detailsCopy.whyThisTask.contains("deferred"))
        }

        val originalScore = created.priorityScore
        taskRepository.update(
            created.copy(
                importance = Importance.LOW,
                estimatedEffort = 30,
                deadline = now + 20 * DAY_MS
            )
        )
        val updated = requireNotNull(taskRepository.observeById(highId).awaitFirst())
        val updatedExplained = taskRepository.explainPriority(updated)
        assertEquals(updatedExplained.score, updated.priorityScore)
        assertNotEquals(originalScore, updated.priorityScore)

        val change = PriorityChangeExplainer.explain(
            previousScore = originalScore,
            newScore = updated.priorityScore,
            deadlineChanged = true,
            importanceChanged = true,
            effortChanged = true
        )
        assertTrue(change.shouldShow)
        assertEquals("$originalScore → ${updated.priorityScore}", change.scoreLine)

        taskRepository.update(
            updated.copy(
                importance = Importance.HIGH,
                estimatedEffort = 60,
                deadline = now + DAY_MS
            )
        )
        val restoredHigh = requireNotNull(taskRepository.getById(highId))
        taskRepository.insert(
            sampleTask(
                title = "Mobile App Report",
                deadline = now + 2 * DAY_MS,
                importance = Importance.MEDIUM,
                estimatedEffort = 60
            )
        )

        val pending = taskRepository.getIncomplete()
        val plan = FocusPlanBuilder.build(pending, availableMinutes = 120)
        val recommendation = FocusRecommendationBuilder.from(plan)
        assertEquals(FocusRecommendation.State.RECOMMENDED, recommendation.state)
        assertEquals(restoredHigh.id, recommendation.recommendedTask?.id)
        assertEquals("Database Assignment", recommendation.recommendedTask?.title)
        assertEquals(plan.selectedTasks.map { it.id }, recommendation.orderedTasks.map { it.id })

        taskRepository.setCompleted(highId, true)
        focusSessionRepository.insert(
            FocusSession(
                taskId = highId,
                duration = 25 * 60_000L,
                startTime = now,
                completed = true
            )
        )

        val completedCount = taskRepository.observeCompletedCount().awaitFirst()
        val remaining = taskRepository.getIncomplete()
        val sessions = focusSessionRepository.observeCompletedSessionCount().awaitFirst()
        val totalFocus = focusSessionRepository.observeTotalSavedDuration().awaitFirst()
        val recent = taskRepository.observeRecentCompleted(5).awaitFirst()
        val insight = ProductivityInsightGenerator.generate(
            ProductivityInsightGenerator.statsFrom(
                completedTasks = completedCount,
                pendingTasks = remaining,
                completedFocusSessions = sessions,
                totalFocusMillis = totalFocus,
                recentCompletedTasks = recent,
                streakDays = 1,
                nowMillis = now,
                timeZone = TimeZone.getDefault()
            )
        )
        assertTrue(completedCount >= 1)
        assertEquals(1, sessions)
        assertEquals(25 * 60_000L, totalFocus)
        assertTrue(recent.any { it.id == highId })
        assertTrue(insight.kind != ProductivityInsight.Kind.EMPTY)
        assertTrue(insight.body.isNotBlank())
    }
}
