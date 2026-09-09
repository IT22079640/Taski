package com.example.taski.integration

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.taski.data.database.TaskiDatabase
import com.example.taski.data.entity.Importance
import com.example.taski.data.repository.TaskRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
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
class TaskRepositoryIntegrationTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var db: TaskiDatabase
    private lateinit var repository: TaskRepository
    private lateinit var scheduler: RecordingReminderScheduler

    @Before
    fun setUp() {
        db = createInMemoryDb()
        scheduler = RecordingReminderScheduler()
        repository = TaskRepository(
            taskDao = db.taskDao(),
            reminderScheduler = scheduler
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insert_writesThroughDaoToRoom() = runBlocking {
        val now = System.currentTimeMillis()
        val id = repository.insert(
            sampleTask(
                title = "Created via repository",
                deadline = now + 2 * DAY_MS,
                importance = Importance.MEDIUM,
                estimatedEffort = 90
            )
        )

        val stored = requireNotNull(repository.getById(id))
        assertEquals("Created via repository", stored.title)
        assertEquals(Importance.MEDIUM, stored.importance)
        assertEquals(90, stored.estimatedEffort)
        assertEquals(now + 2 * DAY_MS, stored.deadline)
        assertEquals(1, scheduler.scheduledTasks.size)
        assertEquals(id, scheduler.scheduledTasks.single().id)
    }

    @Test
    fun update_persistsEditsInRoom() = runBlocking {
        val id = repository.insert(sampleTask(title = "Original", importance = Importance.LOW))
        val original = requireNotNull(repository.getById(id))

        repository.update(
            original.copy(
                title = "Edited title",
                importance = Importance.HIGH,
                estimatedEffort = 60
            )
        )

        val updated = requireNotNull(repository.getById(id))
        assertEquals("Edited title", updated.title)
        assertEquals(Importance.HIGH, updated.importance)
        assertEquals(60, updated.estimatedEffort)
    }

    @Test
    fun setCompleted_persistsCompletedFlagAndTimestamp() = runBlocking {
        val before = System.currentTimeMillis()
        val id = repository.insert(sampleTask(title = "Finish lab"))

        repository.setCompleted(id, true)

        val completed = requireNotNull(repository.getById(id))
        val after = System.currentTimeMillis()
        assertTrue(completed.completed)
        val completedAt = requireNotNull(completed.completedAt)
        assertTrue(completedAt in before..after)
        assertTrue(scheduler.cancelledIds.contains(id))
    }

    @Test
    fun setCompletedFalse_clearsCompletedAt() = runBlocking {
        val id = repository.insert(sampleTask(title = "Toggle"))
        repository.setCompleted(id, true)
        repository.setCompleted(id, false)

        val reopened = requireNotNull(repository.getById(id))
        assertTrue(!reopened.completed)
        assertNull(reopened.completedAt)
    }

    @Test
    fun delete_removesTaskAndRelatedFocusSessions() = runBlocking {
        val id = repository.insert(sampleTask(title = "Parent task"))
        val sessionId = db.focusSessionDao().insert(
            com.example.taski.data.entity.FocusSession(
                taskId = id,
                duration = 900_000L,
                startTime = DEFAULT_CREATED_AT,
                completed = true
            )
        )
        val task = requireNotNull(repository.getById(id))

        repository.delete(task)

        assertNull(repository.getById(id))
        assertNull(db.focusSessionDao().getById(sessionId))
        assertTrue(scheduler.cancelledIds.contains(id))
    }

    @Test
    fun insertAndUpdate_persistCalculatedPriorityScore() = runBlocking {
        val now = System.currentTimeMillis()
        val urgentId = repository.insert(
            sampleTask(
                title = "Urgent high",
                deadline = now + DAY_MS,
                importance = Importance.HIGH,
                estimatedEffort = 120,
                priorityScore = 0
            )
        )
        val laterId = repository.insert(
            sampleTask(
                title = "Later low",
                deadline = now + 40 * DAY_MS,
                importance = Importance.LOW,
                estimatedEffort = 480,
                priorityScore = 0
            )
        )

        val urgent = requireNotNull(repository.getById(urgentId))
        val later = requireNotNull(repository.getById(laterId))
        assertTrue(urgent.priorityScore in 1..100)
        assertTrue(later.priorityScore in 0..100)
        assertTrue(urgent.priorityScore > later.priorityScore)

        repository.update(
            later.copy(
                importance = Importance.HIGH,
                deadline = now + DAY_MS,
                estimatedEffort = 120
            )
        )
        val raised = requireNotNull(repository.getById(laterId))
        assertTrue(raised.priorityScore > later.priorityScore)
        assertEquals(urgent.priorityScore, raised.priorityScore)
    }
}
