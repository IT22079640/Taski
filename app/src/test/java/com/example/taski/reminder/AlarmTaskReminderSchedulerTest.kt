package com.example.taski.reminder

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.taski.data.entity.Importance
import com.example.taski.data.entity.Task
import org.junit.Assert.assertEquals
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
    private lateinit var scheduler: AlarmTaskReminderScheduler

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        scheduler = AlarmTaskReminderScheduler(context)
        shadowOf(alarmManager).scheduledAlarms.clear()
    }

    @Test
    fun futureTask_schedulesAlarmAtDeadline() {
        val now = System.currentTimeMillis()
        val deadline = now + ReminderPlanner.HOUR_MS
        scheduler.schedule(task(id = 11L, deadline = deadline))

        val alarms = shadowOf(alarmManager).scheduledAlarms
        assertEquals(1, alarms.size)
        assertEquals(deadline, alarms.single().triggerAtTime)
    }

    @Test
    fun pastDeadline_doesNotScheduleAlarm() {
        val now = System.currentTimeMillis()
        scheduler.schedule(task(id = 12L, deadline = now - ReminderPlanner.HOUR_MS))
        assertTrue(shadowOf(alarmManager).scheduledAlarms.isEmpty())
    }

    @Test
    fun completedTask_doesNotScheduleAlarm() {
        val now = System.currentTimeMillis()
        scheduler.schedule(
            task(id = 13L, deadline = now + ReminderPlanner.HOUR_MS, completed = true)
        )
        assertTrue(shadowOf(alarmManager).scheduledAlarms.isEmpty())
    }

    @Test
    fun editDeadline_replacesPreviousAlarm() {
        val now = System.currentTimeMillis()
        val first = now + ReminderPlanner.HOUR_MS
        val second = now + 2 * ReminderPlanner.HOUR_MS
        scheduler.schedule(task(id = 14L, deadline = first))
        scheduler.schedule(task(id = 14L, deadline = second))

        val alarms = shadowOf(alarmManager).scheduledAlarms
        assertEquals(1, alarms.size)
        assertEquals(second, alarms.single().triggerAtTime)
    }

    @Test
    fun multipleTasks_keepIndependentAlarms() {
        val now = System.currentTimeMillis()
        scheduler.schedule(task(id = 21L, deadline = now + ReminderPlanner.HOUR_MS))
        scheduler.schedule(task(id = 22L, deadline = now + 2 * ReminderPlanner.HOUR_MS))

        val alarms = shadowOf(alarmManager).scheduledAlarms
        assertEquals(2, alarms.size)
        assertNotEquals(
            ReminderIds.notificationId(21L, ReminderKind.DUE),
            ReminderIds.notificationId(22L, ReminderKind.DUE)
        )
    }

    @Test
    fun cancel_removesScheduledAlarm() {
        val now = System.currentTimeMillis()
        scheduler.schedule(task(id = 31L, deadline = now + ReminderPlanner.HOUR_MS))
        scheduler.cancel(31L)
        assertTrue(shadowOf(alarmManager).scheduledAlarms.isEmpty())
    }

    @Test
    fun rescheduleAll_skipsPastAndCompleted() {
        val now = System.currentTimeMillis()
        shadowOf(alarmManager).scheduledAlarms.clear()
        scheduler.rescheduleAll(
            listOf(
                task(id = 41L, deadline = now + ReminderPlanner.HOUR_MS),
                task(id = 42L, deadline = now - ReminderPlanner.HOUR_MS),
                task(id = 43L, deadline = now + ReminderPlanner.HOUR_MS, completed = true)
            )
        )
        val alarms = shadowOf(alarmManager).scheduledAlarms
        assertEquals(1, alarms.size)
        assertEquals(now + ReminderPlanner.HOUR_MS, alarms.single().triggerAtTime)
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
