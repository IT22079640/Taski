package com.example.taski.integration

import com.example.taski.data.entity.Task
import com.example.taski.reminder.TaskReminderScheduler

class RecordingReminderScheduler : TaskReminderScheduler {
    val scheduledTasks = mutableListOf<Task>()
    val cancelledIds = mutableListOf<Long>()

    override fun schedule(task: Task) {
        scheduledTasks += task
    }

    override fun cancel(taskId: Long) {
        cancelledIds += taskId
    }

    override fun rescheduleAll(tasks: List<Task>) {
        tasks.forEach { schedule(it) }
    }
}
