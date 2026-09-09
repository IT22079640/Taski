package com.example.taski.integration

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.taski.data.database.TaskiDatabase
import com.example.taski.data.repository.TaskRepository
import com.example.taski.reminder.ReminderIds
import com.example.taski.reminder.ReminderKind
import com.example.taski.reminder.ReminderPlanner
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
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
    fun incompleteTask_getsUpcomingAndOverduePlan() = runBlocking {
        val now = System.currentTimeMillis()
        val deadline = now + ReminderPlanner.DAY_MS + ReminderPlanner.HOUR_MS
        val id = repository.insert(sampleTask(title = "Upcoming work", deadline = deadline))
        val stored = requireNotNull(repository.getById(id))

        val plan = ReminderPlanner.plan(stored.deadline, now, stored.completed)
        assertNotNull(plan.upcomingAt)
        assertEquals(deadline - ReminderPlanner.DAY_MS, plan.upcomingAt)
        assertEquals(deadline, plan.overdueAt)
        assertEquals(id, scheduler.scheduledTasks.single().id)
        assertTrue(!stored.completed)
    }

    @Test
    fun completedTask_doesNotReceiveReminderPlan() = runBlocking {
        val now = System.currentTimeMillis()
        val id = repository.insert(
            sampleTask(title = "Will complete", deadline = now + ReminderPlanner.DAY_MS)
        )
        repository.setCompleted(id, true)
        val stored = requireNotNull(repository.getById(id))

        val plan = ReminderPlanner.plan(stored.deadline, now, stored.completed)
        assertNull(plan.upcomingAt)
        assertNull(plan.overdueAt)
        assertTrue(scheduler.cancelledIds.contains(id))
    }

    @Test
    fun overdueIncompleteTask_getsOverdueHandling() = runBlocking {
        val now = System.currentTimeMillis()
        val deadline = now - ReminderPlanner.HOUR_MS
        val id = repository.insert(sampleTask(title = "Already late", deadline = deadline))
        val stored = requireNotNull(repository.getById(id))

        val plan = ReminderPlanner.plan(stored.deadline, now, stored.completed)
        assertNull(plan.upcomingAt)
        assertEquals(now + ReminderPlanner.MINUTE_MS, plan.overdueAt)
        assertTrue(plan.hasAlarms)
        assertEquals(id, scheduler.scheduledTasks.last().id)
    }

    @Test
    fun editingDeadline_producesNewReminderPlan() = runBlocking {
        val now = System.currentTimeMillis()
        val originalDeadline = now + 3 * ReminderPlanner.HOUR_MS
        val id = repository.insert(sampleTask(title = "Move deadline", deadline = originalDeadline))
        val original = requireNotNull(repository.getById(id))
        val originalPlan = ReminderPlanner.plan(original.deadline, now, original.completed)

        val newDeadline = now + ReminderPlanner.DAY_MS + ReminderPlanner.HOUR_MS
        repository.update(original.copy(deadline = newDeadline))
        val edited = requireNotNull(repository.getById(id))
        val editedPlan = ReminderPlanner.plan(edited.deadline, now, edited.completed)

        assertEquals(originalDeadline - ReminderPlanner.HOUR_MS, originalPlan.upcomingAt)
        assertEquals(newDeadline - ReminderPlanner.DAY_MS, editedPlan.upcomingAt)
        assertNotEquals(originalPlan.upcomingAt, editedPlan.upcomingAt)
        assertEquals(newDeadline, edited.deadline)
        assertTrue(scheduler.scheduledTasks.count { it.id == id } >= 2)
    }

    @Test
    fun reminderIds_stayDeterministicForTheSameTask() = runBlocking {
        val id = repository.insert(sampleTask(title = "Stable ids"))
        val firstUpcoming = ReminderIds.notificationId(id, ReminderKind.UPCOMING)
        val firstOverdue = ReminderIds.notificationId(id, ReminderKind.OVERDUE)

        val stored = requireNotNull(repository.getById(id))
        repository.update(stored.copy(title = "Still the same task"))

        assertEquals(firstUpcoming, ReminderIds.notificationId(id, ReminderKind.UPCOMING))
        assertEquals(firstOverdue, ReminderIds.notificationId(id, ReminderKind.OVERDUE))
        assertNotEquals(firstUpcoming, firstOverdue)
        assertNotEquals(
            ReminderIds.notificationId(id, ReminderKind.UPCOMING),
            ReminderIds.notificationId(id + 1, ReminderKind.UPCOMING)
        )
    }
}
