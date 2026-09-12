package com.example.taski.plan

import com.example.taski.data.entity.Importance
import com.example.taski.data.entity.Task
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

class PlanUrgencyTest {

    private val now = 1_746_489_600_000L
    private val utc: TimeZone = TimeZone.getTimeZone("UTC")
    private val dayMs = 86_400_000L

    @Test
    fun overdueTask_isUrgent() {
        val task = task(score = 40, deadline = now - dayMs)
        assertTrue(PlanUrgency.isUrgent(task, now, utc))
        assertTrue(PlanUrgency.label(task, now, utc) == "Overdue")
    }

    @Test
    fun dueToday_isUrgent() {
        val task = task(score = 40, deadline = now)
        assertTrue(PlanUrgency.isUrgent(task, now, utc))
        assertTrue(PlanUrgency.label(task, now, utc) == "Due today")
    }

    @Test
    fun highPriorityLater_isUrgent() {
        val task = task(score = 86, deadline = now + 10 * dayMs)
        assertTrue(PlanUrgency.isUrgent(task, now, utc))
    }

    @Test
    fun mediumPriorityNextWeek_isNotUrgent() {
        val task = task(score = 55, deadline = now + 8 * dayMs)
        assertFalse(PlanUrgency.isUrgent(task, now, utc))
        assertTrue(PlanUrgency.label(task, now, utc) == "Due later")
    }

    private fun task(score: Int, deadline: Long): Task = Task(
        id = 1,
        title = "Task",
        deadline = deadline,
        importance = Importance.MEDIUM,
        estimatedEffort = 30,
        category = "Study",
        priorityScore = score
    )
}
