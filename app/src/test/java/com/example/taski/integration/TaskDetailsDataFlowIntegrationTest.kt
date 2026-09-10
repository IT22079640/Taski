package com.example.taski.integration

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.taski.data.database.TaskiDatabase
import com.example.taski.data.entity.FocusSession
import com.example.taski.data.entity.Importance
import com.example.taski.data.repository.FocusSessionRepository
import com.example.taski.data.repository.TaskRepository
import com.example.taski.priority.PriorityCalculator
import com.example.taski.priority.PriorityExplainer
import com.example.taski.priority.PriorityLevel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TaskDetailsDataFlowIntegrationTest {

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
    fun detailsReadsLiveTaskAndPriorityFromRoom() = runBlocking {
        val id = taskRepository.insert(
            sampleTask(
                title = "HCI draft",
                importance = Importance.HIGH,
                estimatedEffort = 120
            )
        )

        val task = requireNotNull(taskRepository.observeById(id).awaitFirst())
        val explained = taskRepository.explainPriority(task)
        val expected = calculator.calculate(task)
        val detailsCopy = PriorityExplainer.explain(task, explained)

        assertEquals("HCI draft", task.title)
        assertEquals(expected.score, task.priorityScore)
        assertEquals(expected.score, explained.score)
        assertEquals(expected.priorityLevel, explained.priorityLevel)
        assertEquals(3, explained.reasons.size)
        assertEquals(PriorityLevel.fromScore(task.priorityScore), explained.priorityLevel)
        assertTrue(detailsCopy.whyThisTask.isNotBlank())
        assertTrue(detailsCopy.recommendationTitle.isNotBlank())
    }

    @Test
    fun editingRecalculatesPriorityScore() = runBlocking {
        val id = taskRepository.insert(
            sampleTask(
                title = "Draft",
                importance = Importance.LOW,
                estimatedEffort = 240,
                deadline = System.currentTimeMillis() + 14 * DAY_MS
            )
        )
        val original = requireNotNull(taskRepository.getById(id))
        val originalScore = original.priorityScore

        taskRepository.update(
            original.copy(
                title = "Final draft",
                importance = Importance.HIGH,
                estimatedEffort = 30,
                deadline = System.currentTimeMillis()
            )
        )

        val updated = requireNotNull(taskRepository.observeById(id).awaitFirst())
        val explained = taskRepository.explainPriority(updated)
        assertEquals("Final draft", updated.title)
        assertEquals(Importance.HIGH, updated.importance)
        assertNotEquals(originalScore, updated.priorityScore)
        assertEquals(explained.score, updated.priorityScore)
    }

    @Test
    fun completionStateUpdatesForDetails() = runBlocking {
        val id = taskRepository.insert(sampleTask(title = "Complete me"))
        assertFalse(requireNotNull(taskRepository.getById(id)).completed)

        taskRepository.setCompleted(id, true)
        val completed = requireNotNull(taskRepository.observeById(id).awaitFirst())
        assertTrue(completed.completed)
        assertTrue(completed.completedAt != null)

        taskRepository.setCompleted(id, false)
        val reopened = requireNotNull(taskRepository.observeById(id).awaitFirst())
        assertFalse(reopened.completed)
        assertNull(reopened.completedAt)
    }

    @Test
    fun focusSessionsAppearOnDetailsAndDeleteRemovesTask() = runBlocking {
        val id = taskRepository.insert(sampleTask(title = "Focus task"))
        assertTrue(focusSessionRepository.observeByTaskId(id).awaitFirst().isEmpty())

        focusSessionRepository.insert(
            FocusSession(
                taskId = id,
                duration = 25 * 60_000L,
                startTime = System.currentTimeMillis(),
                completed = true
            )
        )
        val sessions = focusSessionRepository.observeByTaskId(id).awaitFirst()
        assertEquals(1, sessions.size)
        assertEquals(25 * 60_000L, sessions.sumOf { it.duration })

        val task = requireNotNull(taskRepository.getById(id))
        taskRepository.delete(task)
        assertNull(taskRepository.observeById(id).awaitFirst())
        assertTrue(focusSessionRepository.observeByTaskId(id).awaitFirst().isEmpty())
    }
}
