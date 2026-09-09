package com.example.taski.integration

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.taski.data.database.TaskiDatabase
import com.example.taski.data.repository.TaskRepository
import com.example.taski.progress.LocalDates
import com.example.taski.progress.StreakCalculator
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
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CompletionProgressIntegrationTest {

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
    fun completingTask_isReflectedInProgressQueries() = runBlocking {
        val pendingId = repository.insert(sampleTask(title = "Still open"))
        val completedId = repository.insert(sampleTask(title = "Mark done"))
        val before = System.currentTimeMillis()

        repository.setCompleted(completedId, true)

        val completed = requireNotNull(repository.getById(completedId))
        val pending = requireNotNull(repository.getById(pendingId))
        val after = System.currentTimeMillis()
        assertTrue(completed.completed)
        assertTrue(!pending.completed)
        val completedAt = requireNotNull(completed.completedAt)
        assertTrue(completedAt in before..after)

        assertEquals(1, repository.observeCompletedCount().awaitFirst())
        assertEquals(1, repository.observePendingCount().awaitFirst())

        val timeZone = TimeZone.getDefault()
        val today = LocalDates.dayRangeContaining(completedAt, timeZone)
        assertEquals(
            1,
            repository.observeCompletedCountBetween(today.startInclusive, today.endExclusive).awaitFirst()
        )

        val recent = repository.observeRecentCompleted(5).awaitFirst()
        assertEquals(listOf(completedId), recent.map { it.id })
        assertEquals("Mark done", recent.single().title)

        val completionTimes = repository.observeCompletedAtTimes().awaitFirst()
        assertEquals(listOf(completedAt), completionTimes)
        assertEquals(
            1,
            StreakCalculator.currentStreak(completionTimes, completedAt, timeZone)
        )
    }
}
