package com.example.taski.integration

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.taski.data.dao.FocusSessionDao
import com.example.taski.data.dao.TaskDao
import com.example.taski.data.database.TaskiDatabase
import com.example.taski.data.entity.FocusSession
import com.example.taski.data.entity.Importance
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
class TaskDaoIntegrationTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var db: TaskiDatabase
    private lateinit var taskDao: TaskDao
    private lateinit var focusSessionDao: FocusSessionDao

    @Before
    fun setUp() {
        db = createInMemoryDb()
        taskDao = db.taskDao()
        focusSessionDao = db.focusSessionDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertAndRetrieveTask_persistsCoreFields() = runBlockingTest {
        val id = taskDao.insert(
            sampleTask(
                title = "SE4041 report",
                deadline = DEFAULT_DEADLINE,
                importance = Importance.HIGH,
                estimatedEffort = 150,
                priorityScore = 82
            )
        )

        val stored = taskDao.getById(id)
        requireNotNull(stored)
        assertEquals("SE4041 report", stored.title)
        assertEquals(DEFAULT_DEADLINE, stored.deadline)
        assertEquals(Importance.HIGH, stored.importance)
        assertEquals(150, stored.estimatedEffort)
        assertEquals(82, stored.priorityScore)
    }

    @Test
    fun updateTask_persistsChangedFields() = runBlockingTest {
        val id = taskDao.insert(sampleTask(title = "Draft", importance = Importance.LOW, priorityScore = 20))
        val original = requireNotNull(taskDao.getById(id))

        taskDao.update(
            original.copy(
                title = "Final draft",
                importance = Importance.HIGH,
                priorityScore = 91,
                completed = true,
                completedAt = DEFAULT_CREATED_AT + 1_000L
            )
        )

        val updated = requireNotNull(taskDao.getById(id))
        assertEquals("Final draft", updated.title)
        assertEquals(Importance.HIGH, updated.importance)
        assertEquals(91, updated.priorityScore)
        assertTrue(updated.completed)
        assertEquals(DEFAULT_CREATED_AT + 1_000L, updated.completedAt)
    }

    @Test
    fun deleteTask_removesRow() = runBlockingTest {
        val id = taskDao.insert(sampleTask(title = "Temporary"))
        val stored = requireNotNull(taskDao.getById(id))

        taskDao.delete(stored)

        assertNull(taskDao.getById(id))
        assertTrue(taskDao.observeAllByPriority().awaitFirst().isEmpty())
    }

    @Test
    fun incompleteTasks_areOrderedByPriorityScoreDescending() = runBlockingTest {
        taskDao.insert(sampleTask(title = "Low", priorityScore = 22, deadline = DEFAULT_DEADLINE + DAY_MS))
        taskDao.insert(sampleTask(title = "High", priorityScore = 88, deadline = DEFAULT_DEADLINE + 2 * DAY_MS))
        taskDao.insert(sampleTask(title = "Medium", priorityScore = 55, deadline = DEFAULT_DEADLINE))

        val ordered = taskDao.observeIncomplete().awaitFirst()
        assertEquals(listOf("High", "Medium", "Low"), ordered.map { it.title })
        assertTrue(ordered[0].priorityScore > ordered[1].priorityScore)
        assertTrue(ordered[1].priorityScore > ordered[2].priorityScore)
    }

    @Test
    fun allByPriority_listsIncompleteBeforeCompleted() = runBlockingTest {
        taskDao.insert(
            sampleTask(title = "Done high", priorityScore = 99, completed = true, completedAt = DEFAULT_CREATED_AT)
        )
        taskDao.insert(sampleTask(title = "Open low", priorityScore = 15))
        taskDao.insert(sampleTask(title = "Open high", priorityScore = 80))

        val ordered = taskDao.observeAllByPriority().awaitFirst()
        assertEquals(listOf("Open high", "Open low", "Done high"), ordered.map { it.title })
        assertTrue(!ordered[0].completed && !ordered[1].completed && ordered[2].completed)
    }

    @Test
    fun insertFocusSession_persistsLinkAndFields() = runBlockingTest {
        val taskId = taskDao.insert(sampleTask(title = "Focus target"))
        val startTime = DEFAULT_CREATED_AT + 5_000L
        val sessionId = focusSessionDao.insert(
            FocusSession(
                taskId = taskId,
                duration = 1_500_000L,
                startTime = startTime,
                completed = true
            )
        )

        val stored = requireNotNull(focusSessionDao.getById(sessionId))
        assertEquals(taskId, stored.taskId)
        assertEquals(1_500_000L, stored.duration)
        assertEquals(startTime, stored.startTime)
        assertTrue(stored.completed)
    }

    @Test
    fun deletingTask_cascadesToFocusSessions() = runBlockingTest {
        val taskId = taskDao.insert(sampleTask(title = "Parent"))
        val sessionId = focusSessionDao.insert(
            FocusSession(
                taskId = taskId,
                duration = 600_000L,
                startTime = DEFAULT_CREATED_AT,
                completed = false
            )
        )
        val task = requireNotNull(taskDao.getById(taskId))

        taskDao.delete(task)

        assertNull(taskDao.getById(taskId))
        assertNull(focusSessionDao.getById(sessionId))
        assertTrue(focusSessionDao.observeByTaskId(taskId).awaitFirst().isEmpty())
    }

    private fun runBlockingTest(block: suspend () -> Unit) {
        kotlinx.coroutines.runBlocking { block() }
    }
}
