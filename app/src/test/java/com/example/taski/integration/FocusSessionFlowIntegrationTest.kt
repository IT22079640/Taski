package com.example.taski.integration

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.taski.data.database.TaskiDatabase
import com.example.taski.data.entity.FocusSession
import com.example.taski.data.repository.FocusSessionRepository
import com.example.taski.data.repository.TaskRepository
import com.example.taski.progress.LocalDates
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
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FocusSessionFlowIntegrationTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var db: TaskiDatabase
    private lateinit var taskRepository: TaskRepository
    private lateinit var focusSessionRepository: FocusSessionRepository

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
    fun savedFocusSession_contributesToFocusTimeQueries() = runBlocking {
        val taskId = taskRepository.insert(sampleTask(title = "Timed work"))
        val startTime = System.currentTimeMillis()
        val sessionId = focusSessionRepository.insert(
            FocusSession(
                taskId = taskId,
                duration = 1_500_000L,
                startTime = startTime,
                completed = true
            )
        )

        val stored = requireNotNull(focusSessionRepository.getById(sessionId))
        assertEquals(taskId, stored.taskId)
        assertEquals(1_500_000L, stored.duration)
        assertEquals(startTime, stored.startTime)
        assertTrue(stored.completed)

        assertEquals(1_500_000L, focusSessionRepository.observeTotalSavedDuration().awaitFirst())
        assertEquals(1_500_000L, focusSessionRepository.observeTotalCompletedDuration().awaitFirst())
        assertEquals(1, focusSessionRepository.observeCompletedSessionCount().awaitFirst())
        assertEquals(1_500_000L, focusSessionRepository.observeTotalDurationForTask(taskId).awaitFirst())

        val today = LocalDates.dayRangeContaining(startTime, TimeZone.getDefault())
        assertEquals(
            1_500_000L,
            focusSessionRepository.observeSavedDurationBetween(today.startInclusive, today.endExclusive)
                .awaitFirst()
        )

        val stoppedId = focusSessionRepository.insert(
            FocusSession(
                taskId = taskId,
                duration = 180_000L,
                startTime = startTime + 1_000L,
                completed = false
            )
        )
        requireNotNull(focusSessionRepository.getById(stoppedId))
        assertEquals(1_680_000L, focusSessionRepository.observeTotalSavedDuration().awaitFirst())
        assertEquals(1, focusSessionRepository.observeCompletedSessionCount().awaitFirst())
    }

    @Test
    fun deletingTask_removesRelatedFocusSessionFromProgressTotals() = runBlocking {
        val taskId = taskRepository.insert(sampleTask(title = "To delete"))
        focusSessionRepository.insert(
            FocusSession(
                taskId = taskId,
                duration = 600_000L,
                startTime = System.currentTimeMillis(),
                completed = true
            )
        )
        assertEquals(600_000L, focusSessionRepository.observeTotalSavedDuration().awaitFirst())

        val task = requireNotNull(taskRepository.getById(taskId))
        taskRepository.delete(task)

        assertNull(taskRepository.getById(taskId))
        assertTrue(focusSessionRepository.observeByTaskId(taskId).awaitFirst().isEmpty())
        assertEquals(0L, focusSessionRepository.observeTotalSavedDuration().awaitFirst())
        assertEquals(0, focusSessionRepository.observeCompletedSessionCount().awaitFirst())
    }
}
