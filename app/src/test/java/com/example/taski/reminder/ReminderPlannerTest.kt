package com.example.taski.reminder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderPlannerTest {

    private val now = 1_746_489_600_000L

    @Test
    fun moreThanOneDayAway_schedulesUpcoming24HoursBeforeAndDueAtDeadline() {
        val deadline = now + ReminderPlanner.DAY_MS + ReminderPlanner.HOUR_MS
        val plan = ReminderPlanner.plan(deadline, now, completed = false)
        assertEquals(deadline - ReminderPlanner.DAY_MS, plan.upcomingAt)
        assertEquals(deadline, plan.dueAt)
        assertFalse(plan.overdueNow)
        assertTrue(plan.hasAlarms)
        assertTrue(plan.upcomingAt!! > now)
    }

    @Test
    fun exactly24HoursAway_usesOneHourUpcomingNotANowAlarm() {
        val deadline = now + ReminderPlanner.DAY_MS
        val plan = ReminderPlanner.plan(deadline, now, completed = false)
        assertEquals(deadline - ReminderPlanner.HOUR_MS, plan.upcomingAt)
        assertEquals(deadline, plan.dueAt)
        assertTrue(plan.upcomingAt!! > now)
    }

    @Test
    fun lessThan24HoursButMoreThanOneHour_schedulesUpcomingOneHourBefore() {
        val deadline = now + 5 * ReminderPlanner.HOUR_MS
        val plan = ReminderPlanner.plan(deadline, now, completed = false)
        assertEquals(deadline - ReminderPlanner.HOUR_MS, plan.upcomingAt)
        assertEquals(deadline, plan.dueAt)
        assertFalse(plan.overdueNow)
    }

    @Test
    fun exactlyOneHourAway_doesNotScheduleUpcoming() {
        val deadline = now + ReminderPlanner.HOUR_MS
        val plan = ReminderPlanner.plan(deadline, now, completed = false)
        assertNull(plan.upcomingAt)
        assertEquals(deadline, plan.dueAt)
    }

    @Test
    fun lessThanOneHourRemaining_doesNotCreatePastUpcomingAlarm() {
        val deadline = now + 15 * ReminderPlanner.MINUTE_MS
        val plan = ReminderPlanner.plan(deadline, now, completed = false)
        assertNull(plan.upcomingAt)
        assertEquals(deadline, plan.dueAt)
        assertFalse(plan.overdueNow)
    }

    @Test
    fun pastDeadline_isOverdueWithoutFutureAlarms() {
        val deadline = now - ReminderPlanner.HOUR_MS
        val plan = ReminderPlanner.plan(deadline, now, completed = false)
        assertNull(plan.upcomingAt)
        assertNull(plan.dueAt)
        assertTrue(plan.overdueNow)
        assertTrue(plan.hasAlarms)
    }

    @Test
    fun deadlineEqualToNow_isOverdue() {
        val plan = ReminderPlanner.plan(now, now, completed = false)
        assertTrue(plan.overdueNow)
        assertNull(plan.dueAt)
        assertNull(plan.upcomingAt)
    }

    @Test
    fun completedTask_receivesNoReminders() {
        val deadline = now + ReminderPlanner.DAY_MS
        val plan = ReminderPlanner.plan(deadline, now, completed = true)
        assertNull(plan.upcomingAt)
        assertNull(plan.dueAt)
        assertFalse(plan.overdueNow)
        assertFalse(plan.hasAlarms)
    }

    @Test
    fun upcomingTriggerNeverInThePast() {
        val deadline = now + ReminderPlanner.MINUTE_MS
        assertNull(ReminderPlanner.upcomingTriggerAt(deadline, now))
        assertNotNull(ReminderPlanner.dueTriggerAt(deadline, now, completed = false))
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
            ReminderIds.notificationId(12L, ReminderKind.OVERDUE)
        )
    }

    @Test
    fun upcomingDueAndOverdueForSameTaskDiffer() {
        val upcoming = ReminderIds.notificationId(7L, ReminderKind.UPCOMING)
        val due = ReminderIds.notificationId(7L, ReminderKind.DUE)
        val overdue = ReminderIds.notificationId(7L, ReminderKind.OVERDUE)
        assertNotEquals(upcoming, due)
        assertNotEquals(due, overdue)
        assertNotEquals(upcoming, overdue)
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
        assertNotEquals(
            ReminderIds.notificationId(1L, ReminderKind.OVERDUE),
            ReminderIds.notificationId(2L, ReminderKind.OVERDUE)
        )
    }
}
