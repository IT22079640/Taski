package com.example.taski.integration

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.taski.data.database.TaskiDatabase
import com.example.taski.data.repository.TaskRepository
import com.example.taski.reminder.ReminderIds
import com.example.taski.reminder.ReminderKind
import com.example.taski.reminder.ReminderPlanner
import com.example.taski.reminder.TaskReminderScheduler
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
class ReminderTaskStateIntegrationTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var db: TaskiDatabase
    private lateinit var scheduler: RecordingReminderScheduler
    private lateinit var repository: TaskRepository

    @Before
    fun setUp() {
        db = createInMemoryDb()
        scheduler = RecordingReminderScheduler()
        repository = TaskRepository(db.taskDao(), reminderScheduler = scheduler)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun creatingFutureTask_schedulesReminderAtDeadline() = runBlocking {
        val now = System.currentTimeMillis()
        val deadline = now + ReminderPlanner.HOUR_MS
        val id = repository.insert(sampleTask(title = "Submit Database Assignment", deadline = deadline))
        val stored = requireNotNull(repository.getById(id))

        val plan = ReminderPlanner.plan(stored.deadline, now, stored.completed)
        assertEquals(deadline, plan.triggerAt)
        assertEquals(id, scheduler.scheduledTasks.single().id)
        assertEquals(deadline, scheduler.scheduledTasks.single().deadline)
        assertTrue(!stored.completed)
    }

    @Test
    fun editingDeadline_cancelsThenSchedulesNewReminder() = runBlocking {
        val now = System.currentTimeMillis()
        val originalDeadline = now + 3 * ReminderPlanner.HOUR_MS
        val id = repository.insert(sampleTask(title = "Move deadline", deadline = originalDeadline))
        val original = requireNotNull(repository.getById(id))
        scheduler.cancelledIds.clear()
        scheduler.scheduledTasks.clear()

        val newDeadline = now + ReminderPlanner.DAY_MS
        repository.update(original.copy(deadline = newDeadline))
        val edited = requireNotNull(repository.getById(id))

        assertTrue(scheduler.cancelledIds.contains(id))
        assertEquals(newDeadline, scheduler.scheduledTasks.single().deadline)
        assertEquals(newDeadline, edited.deadline)
        assertEquals(newDeadline, ReminderPlanner.plan(edited.deadline, now, false).triggerAt)
        assertNotEquals(originalDeadline, edited.deadline)
    }

    @Test
    fun deletingTask_cancelsReminderAndLeavesOtherTasks() = runBlocking {
        val now = System.currentTimeMillis()
        val keepId = repository.insert(
            sampleTask(title = "Keep me", deadline = now + ReminderPlanner.DAY_MS)
        )
        val deleteId = repository.insert(
            sampleTask(title = "Delete me", deadline = now + 2 * ReminderPlanner.DAY_MS)
        )
        val toDelete = requireNotNull(repository.getById(deleteId))
        scheduler.cancelledIds.clear()

        repository.delete(toDelete)

        assertNull(repository.getById(deleteId))
        assertTrue(scheduler.cancelledIds.contains(deleteId))
        assertFalse(scheduler.cancelledIds.contains(keepId))
        assertEquals(keepId, requireNotNull(repository.getById(keepId)).id)
        assertNotEquals(
            ReminderIds.notificationId(keepId, ReminderKind.DUE),
            ReminderIds.notificationId(deleteId, ReminderKind.DUE)
        )
    }

    @Test
    fun completingTask_cancelsFutureReminder() = runBlocking {
        val now = System.currentTimeMillis()
        val id = repository.insert(
            sampleTask(title = "Will complete", deadline = now + ReminderPlanner.DAY_MS)
        )
        scheduler.cancelledIds.clear()
        repository.setCompleted(id, true)
        val stored = requireNotNull(repository.getById(id))

        assertTrue(stored.completed)
        assertTrue(scheduler.cancelledIds.contains(id))
        assertNull(ReminderPlanner.plan(stored.deadline, now, stored.completed).triggerAt)
    }

    @Test
    fun uncompletingFutureTask_reschedulesReminder() = runBlocking {
        val now = System.currentTimeMillis()
        val deadline = now + ReminderPlanner.DAY_MS
        val id = repository.insert(sampleTask(title = "Toggle", deadline = deadline))
        repository.setCompleted(id, true)
        scheduler.scheduledTasks.clear()
        repository.setCompleted(id, false)

        assertEquals(id, scheduler.scheduledTasks.last().id)
        assertEquals(deadline, scheduler.scheduledTasks.last().deadline)
    }

    @Test
    fun pastDeadline_doesNotScheduleFutureAlarm() = runBlocking {
        val now = System.currentTimeMillis()
        val deadline = now - ReminderPlanner.HOUR_MS
        val id = repository.insert(sampleTask(title = "Already late", deadline = deadline))
        val stored = requireNotNull(repository.getById(id))

        assertNull(ReminderPlanner.plan(stored.deadline, now, stored.completed).triggerAt)
        assertTrue(scheduler.scheduledTasks.none { it.id == id })
        assertEquals(id, stored.id)
    }

    @Test
    fun multipleTasks_haveIndependentReminderIds() = runBlocking {
        val now = System.currentTimeMillis()
        val first = repository.insert(sampleTask(title = "First", deadline = now + ReminderPlanner.HOUR_MS))
        val second = repository.insert(sampleTask(title = "Second", deadline = now + 2 * ReminderPlanner.HOUR_MS))

        assertEquals(2, scheduler.scheduledTasks.map { it.id }.distinct().size)
        assertNotEquals(
            ReminderIds.notificationId(first, ReminderKind.DUE),
            ReminderIds.notificationId(second, ReminderKind.DUE)
        )
    }

    @Test
    fun rescheduleAll_schedulesFuturePendingTasksOnly() = runBlocking {
        val now = System.currentTimeMillis()
        val futureId = repository.insert(
            sampleTask(title = "Future", deadline = now + ReminderPlanner.DAY_MS)
        )
        val pastId = repository.insert(
            sampleTask(title = "Past", deadline = now - ReminderPlanner.HOUR_MS)
        )
        val doneId = repository.insert(
            sampleTask(title = "Done", deadline = now + ReminderPlanner.DAY_MS)
        )
        repository.setCompleted(doneId, true)
        scheduler.scheduledTasks.clear()

        scheduler.rescheduleAll(repository.getIncomplete())

        assertEquals(setOf(futureId), scheduler.scheduledTasks.map { it.id }.toSet())
        assertTrue(scheduler.scheduledTasks.none { it.id == pastId || it.id == doneId })
    }

    @Test
    fun notificationPermissionDenied_taskCreationStillSucceeds() = runBlocking {
        val noopRepository = TaskRepository(
            taskDao = db.taskDao(),
            reminderScheduler = TaskReminderScheduler.NoOp
        )
        val now = System.currentTimeMillis()
        val id = noopRepository.insert(
            sampleTask(title = "Saved without notifications", deadline = now + ReminderPlanner.HOUR_MS)
        )
        val stored = requireNotNull(noopRepository.getById(id))
        assertEquals("Saved without notifications", stored.title)
        assertEquals(now + ReminderPlanner.HOUR_MS, stored.deadline)
    }
}
