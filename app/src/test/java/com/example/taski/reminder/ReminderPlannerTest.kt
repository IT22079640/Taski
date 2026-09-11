package com.example.taski.reminder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderPlannerTest {

    private val now = 1_746_489_600_000L

    @Test
    fun futureDeadline_schedulesExactlyAtDeadline() {
        val deadline = now + ReminderPlanner.DAY_MS + ReminderPlanner.HOUR_MS
        val plan = ReminderPlanner.plan(deadline, now, completed = false)
        assertEquals(deadline, plan.triggerAt)
        assertTrue(plan.hasAlarms)
    }

    @Test
    fun pastDeadline_doesNotScheduleAnyAlarm() {
        val deadline = now - ReminderPlanner.HOUR_MS
        val plan = ReminderPlanner.plan(deadline, now, completed = false)
        assertNull(plan.triggerAt)
        assertFalse(plan.hasAlarms)
    }

    @Test
    fun deadlineEqualToNow_doesNotSchedule() {
        val plan = ReminderPlanner.plan(now, now, completed = false)
        assertNull(plan.triggerAt)
    }

    @Test
    fun completedTask_isNotScheduledEvenIfFuture() {
        val deadline = now + ReminderPlanner.DAY_MS
        val plan = ReminderPlanner.plan(deadline, now, completed = true)
        assertNull(plan.triggerAt)
        assertFalse(plan.hasAlarms)
    }

    @Test
    fun shortlyBeforeDeadline_stillSchedulesAtDeadline() {
        val deadline = now + 5 * ReminderPlanner.MINUTE_MS
        val plan = ReminderPlanner.plan(deadline, now, completed = false)
        assertEquals(deadline, plan.triggerAt)
    }
}

class ReminderIdsTest {
    @Test
    fun idsAreDeterministicAndStable() {
        val first = ReminderIds.notificationId(12L, ReminderKind.DUE)
        val second = ReminderIds.notificationId(12L, ReminderKind.DUE)
        assertEquals(first, second)
        assertEquals(
            ReminderIds.notificationId(12L, ReminderKind.OVERDUE),
            ReminderIds.notificationId(12L, ReminderKind.DUE)
        )
    }

    @Test
    fun differentTasksDoNotShareIds() {
        assertNotEquals(
            ReminderIds.notificationId(1L, ReminderKind.DUE),
            ReminderIds.notificationId(2L, ReminderKind.DUE)
        )
        assertNotEquals(
            ReminderIds.notificationId(1L, ReminderKind.UPCOMING),
            ReminderIds.notificationId(2L, ReminderKind.UPCOMING)
        )
    }

    @Test
    fun upcomingAndDueForSameTaskDiffer() {
        assertNotEquals(
            ReminderIds.notificationId(7L, ReminderKind.UPCOMING),
            ReminderIds.notificationId(7L, ReminderKind.DUE)
        )
    }
}
