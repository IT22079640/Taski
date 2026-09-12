package com.example.taski.reminder

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.taski.data.entity.Importance
import com.example.taski.data.entity.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AlarmTaskReminderSchedulerTest {

    private lateinit var context: Context
    private lateinit var alarmManager: AlarmManager
    private lateinit var overdueStore: OverdueNoticeStore
    private lateinit var scheduler: AlarmTaskReminderScheduler
    private var now = 1_746_489_600_000L

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        context.getSharedPreferences(OverdueNoticeStore.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        overdueStore = OverdueNoticeStore.from(context)
        scheduler = AlarmTaskReminderScheduler(
            context = context,
            clock = { now },
            overdueStore = overdueStore
        )
        shadowOf(alarmManager).scheduledAlarms.clear()
    }

    @Test
    fun moreThanOneDayAway_schedulesUpcomingAndDue() {
        val deadline = now + ReminderPlanner.DAY_MS + ReminderPlanner.HOUR_MS
        scheduler.schedule(task(id = 11L, deadline = deadline))

        assertEquals(setOf(ReminderKind.UPCOMING, ReminderKind.DUE), alarmKinds().toSet())
        assertEquals(deadline - ReminderPlanner.DAY_MS, triggerFor(ReminderKind.UPCOMING))
        assertEquals(deadline, triggerFor(ReminderKind.DUE))
    }

    @Test
    fun lessThanOneHourRemaining_schedulesDueOnly() {
        val deadline = now + 15 * ReminderPlanner.MINUTE_MS
        scheduler.schedule(task(id = 12L, deadline = deadline))

        assertEquals(listOf(ReminderKind.DUE), alarmKinds())
        assertEquals(deadline, triggerFor(ReminderKind.DUE))
    }

    @Test
    fun pastDeadline_schedulesOverdueOnce() {
        scheduler.schedule(task(id = 13L, deadline = now - ReminderPlanner.HOUR_MS))
        assertEquals(listOf(ReminderKind.OVERDUE), alarmKinds())
        assertEquals(now, triggerFor(ReminderKind.OVERDUE))
    }

    @Test
    fun overdue_isNotRescheduledAfterNoticeWasPosted() {
        val overdue = task(id = 14L, deadline = now - ReminderPlanner.HOUR_MS)
        scheduler.schedule(overdue)
        assertEquals(1, shadowOf(alarmManager).scheduledAlarms.size)
        overdueStore.markNotified(14L)

        scheduler.schedule(overdue)
        assertTrue(shadowOf(alarmManager).scheduledAlarms.isEmpty())
    }

    @Test
    fun rescheduleAll_doesNotDuplicateAlarms() {
        val deadline = now + ReminderPlanner.DAY_MS + ReminderPlanner.HOUR_MS
        val item = task(id = 15L, deadline = deadline)
        scheduler.rescheduleAll(listOf(item))
        scheduler.rescheduleAll(listOf(item))
        assertEquals(2, shadowOf(alarmManager).scheduledAlarms.size)
        assertEquals(setOf(ReminderKind.UPCOMING, ReminderKind.DUE), alarmKinds().toSet())
    }

    @Test
    fun completedTask_doesNotScheduleAlarm() {
        scheduler.schedule(
            task(id = 16L, deadline = now + ReminderPlanner.HOUR_MS, completed = true)
        )
        assertTrue(shadowOf(alarmManager).scheduledAlarms.isEmpty())
    }

    @Test
    fun editDeadline_replacesPreviousAlarms() {
        val first = now + ReminderPlanner.DAY_MS + ReminderPlanner.HOUR_MS
        val second = now + 2 * ReminderPlanner.HOUR_MS
        scheduler.schedule(task(id = 17L, deadline = first))
        scheduler.schedule(task(id = 17L, deadline = second))

        assertEquals(listOf(ReminderKind.UPCOMING, ReminderKind.DUE).toSet(), alarmKinds().toSet())
        assertEquals(second - ReminderPlanner.HOUR_MS, triggerFor(ReminderKind.UPCOMING))
        assertEquals(second, triggerFor(ReminderKind.DUE))
        assertFalse(alarmKinds().contains(ReminderKind.OVERDUE))
    }

    @Test
    fun multipleTasks_keepIndependentAlarms() {
        scheduler.schedule(task(id = 21L, deadline = now + ReminderPlanner.HOUR_MS))
        scheduler.schedule(task(id = 22L, deadline = now + 2 * ReminderPlanner.HOUR_MS))

        assertEquals(3, shadowOf(alarmManager).scheduledAlarms.size)
        assertNotEquals(
            ReminderIds.notificationId(21L, ReminderKind.DUE),
            ReminderIds.notificationId(22L, ReminderKind.DUE)
        )
    }

    @Test
    fun cancel_removesAllReminderKinds() {
        scheduler.schedule(
            task(id = 31L, deadline = now + ReminderPlanner.DAY_MS + ReminderPlanner.HOUR_MS)
        )
        scheduler.cancel(31L)
        assertTrue(shadowOf(alarmManager).scheduledAlarms.isEmpty())
        assertFalse(overdueStore.wasNotified(31L))
    }

    @Test
    fun rescheduleAll_skipsCompletedButKeepsOverdueAndFuture() {
        shadowOf(alarmManager).scheduledAlarms.clear()
        scheduler.rescheduleAll(
            listOf(
                task(id = 41L, deadline = now + ReminderPlanner.HOUR_MS),
                task(id = 42L, deadline = now - ReminderPlanner.HOUR_MS),
                task(id = 43L, deadline = now + ReminderPlanner.HOUR_MS, completed = true)
            )
        )
        val kindsByTask = alarmsByTask()
        assertEquals(setOf(ReminderKind.DUE), kindsByTask[41L])
        assertEquals(setOf(ReminderKind.OVERDUE), kindsByTask[42L])
        assertFalse(kindsByTask.containsKey(43L))
    }

    private fun alarmKinds(): List<ReminderKind> =
        shadowOf(alarmManager).scheduledAlarms.map { kindOf(requireNotNull(it.operation)) }

    private fun triggerFor(kind: ReminderKind): Long =
        shadowOf(alarmManager).scheduledAlarms
            .first { kindOf(requireNotNull(it.operation)) == kind }
            .triggerAtTime

    private fun alarmsByTask(): Map<Long, Set<ReminderKind>> =
        shadowOf(alarmManager).scheduledAlarms.groupBy { alarm ->
            shadowOf(requireNotNull(alarm.operation)).savedIntent
                .getLongExtra(AlarmTaskReminderScheduler.EXTRA_TASK_ID, 0L)
        }.mapValues { (_, alarms) ->
            alarms.map { kindOf(requireNotNull(it.operation)) }.toSet()
        }

    private fun kindOf(operation: android.app.PendingIntent): ReminderKind {
        val raw = shadowOf(operation).savedIntent
            .getStringExtra(AlarmTaskReminderScheduler.EXTRA_KIND)
        return ReminderKind.valueOf(requireNotNull(raw))
    }

    private fun task(
        id: Long,
        deadline: Long,
        completed: Boolean = false
    ): Task = Task(
        id = id,
        title = "Task $id",
        deadline = deadline,
        importance = Importance.HIGH,
        estimatedEffort = 30,
        category = "Study",
        completed = completed
    )
}
