package com.example.taski.reminder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderPlannerTest {

    private val now = 1_746_489_600_000L

    @Test
    fun twentyFourHoursBeforeDeadline_whenThereIsTime() {
        val deadline = now + ReminderPlanner.DAY_MS + ReminderPlanner.HOUR_MS
        val plan = ReminderPlanner.plan(deadline, now, completed = false)
        assertEquals(deadline - ReminderPlanner.DAY_MS, plan.upcomingAt)
        assertEquals(deadline, plan.overdueAt)
    }

    @Test
    fun exactlyTwentyFourHoursAway_remindsImmediately() {
        val deadline = now + ReminderPlanner.DAY_MS
        val plan = ReminderPlanner.plan(deadline, now, completed = false)
        assertEquals(now, plan.upcomingAt)
        assertEquals(deadline, plan.overdueAt)
    }

    @Test
    fun lessThanTwentyFourHours_usesShorterLeadTime() {
        val deadline = now + 3 * ReminderPlanner.HOUR_MS
        val plan = ReminderPlanner.plan(deadline, now, completed = false)
        assertEquals(deadline - ReminderPlanner.HOUR_MS, plan.upcomingAt)
        assertTrue(plan.upcomingAt!! > now)
        assertTrue(plan.upcomingAt!! < deadline)
        assertEquals(deadline, plan.overdueAt)
    }

    @Test
    fun underTwoHours_usesFifteenMinutesBeforeDeadline() {
        val deadline = now + 90 * ReminderPlanner.MINUTE_MS
        val plan = ReminderPlanner.plan(deadline, now, completed = false)
        assertEquals(deadline - ReminderPlanner.FIFTEEN_MINUTES_MS, plan.upcomingAt)
    }

    @Test
    fun shortlyBeforeDeadline_schedulesSoonReminder() {
        val deadline = now + 20 * ReminderPlanner.MINUTE_MS
        val plan = ReminderPlanner.plan(deadline, now, completed = false)
        assertEquals(now + ReminderPlanner.MINUTE_MS, plan.upcomingAt)
    }

    @Test
    fun pastDeadline_skipsUpcomingAndSchedulesOverdueSoon() {
        val deadline = now - ReminderPlanner.HOUR_MS
        val plan = ReminderPlanner.plan(deadline, now, completed = false)
        assertNull(plan.upcomingAt)
        assertEquals(now + ReminderPlanner.MINUTE_MS, plan.overdueAt)
    }

    @Test
    fun completedTask_isNotScheduled() {
        val deadline = now + ReminderPlanner.DAY_MS
        val plan = ReminderPlanner.plan(deadline, now, completed = true)
        assertNull(plan.upcomingAt)
        assertNull(plan.overdueAt)
        assertTrue(!plan.hasAlarms)
    }

    @Test
    fun verySoonDeadline_skipsUpcoming() {
        val deadline = now + 5 * ReminderPlanner.MINUTE_MS
        val plan = ReminderPlanner.plan(deadline, now, completed = false)
        assertNull(plan.upcomingAt)
        assertEquals(deadline, plan.overdueAt)
    }
}

class ReminderIdsTest {
    @Test
    fun idsAreDeterministicAndStable() {
        val first = ReminderIds.notificationId(12L, ReminderKind.UPCOMING)
        val second = ReminderIds.notificationId(12L, ReminderKind.UPCOMING)
        assertEquals(first, second)
        assertEquals(ReminderIds.notificationId(12L, ReminderKind.OVERDUE), ReminderIds.notificationId(12L, ReminderKind.OVERDUE))
    }

    @Test
    fun differentTasksDoNotShareIds() {
        assertNotEquals(
            ReminderIds.notificationId(1L, ReminderKind.UPCOMING),
            ReminderIds.notificationId(2L, ReminderKind.UPCOMING)
        )
        assertNotEquals(
            ReminderIds.notificationId(1L, ReminderKind.OVERDUE),
            ReminderIds.notificationId(2L, ReminderKind.OVERDUE)
        )
    }

    @Test
    fun upcomingAndOverdueForSameTaskDiffer() {
        assertNotEquals(
            ReminderIds.notificationId(7L, ReminderKind.UPCOMING),
            ReminderIds.notificationId(7L, ReminderKind.OVERDUE)
        )
    }
}
