package com.example.taski.integration

import com.example.taski.data.entity.Task
import com.example.taski.reminder.ReminderPlanner
import com.example.taski.reminder.TaskReminderScheduler

class RecordingReminderScheduler : TaskReminderScheduler {
    val scheduledTasks = mutableListOf<Task>()
    val cancelledIds = mutableListOf<Long>()

    override fun schedule(task: Task) {
        cancel(task.id)
        val plan = ReminderPlanner.plan(
            deadlineMillis = task.deadline,
            nowMillis = System.currentTimeMillis(),
            completed = task.completed
        )
        if (plan.hasAlarms) {
            scheduledTasks += task
        }
    }

    override fun cancel(taskId: Long) {
        cancelledIds += taskId
    }

    override fun rescheduleAll(tasks: List<Task>) {
        tasks.forEach { schedule(it) }
    }
}
