package com.example.taski.reminder

import com.example.taski.data.entity.Task

interface TaskReminderScheduler {
    fun schedule(task: Task)
    fun cancel(taskId: Long)
    fun rescheduleAll(tasks: List<Task>)

    object NoOp : TaskReminderScheduler {
        override fun schedule(task: Task) = Unit
        override fun cancel(taskId: Long) = Unit
        override fun rescheduleAll(tasks: List<Task>) = Unit
    }
}
