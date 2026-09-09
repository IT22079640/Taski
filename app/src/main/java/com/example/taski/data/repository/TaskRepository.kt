package com.example.taski.data.repository

import com.example.taski.data.dao.TaskDao
import com.example.taski.data.entity.Task
import com.example.taski.data.entity.TaskWithFocusSessions
import com.example.taski.priority.PriorityCalculator
import com.example.taski.priority.PriorityResult
import com.example.taski.reminder.TaskReminderScheduler
import kotlinx.coroutines.flow.Flow

class TaskRepository(
    private val taskDao: TaskDao,
    private val priorityCalculator: PriorityCalculator = PriorityCalculator(),
    private val reminderScheduler: TaskReminderScheduler = TaskReminderScheduler.NoOp
) {

    fun observeAllByPriority(): Flow<List<Task>> = taskDao.observeAllByPriority()

    fun observeIncomplete(): Flow<List<Task>> = taskDao.observeIncomplete()

    fun observeCompleted(): Flow<List<Task>> = taskDao.observeCompleted()

    fun observeById(id: Long): Flow<Task?> = taskDao.observeById(id)

    fun observeWithSessions(id: Long): Flow<TaskWithFocusSessions?> =
        taskDao.observeWithSessions(id)

    suspend fun getById(id: Long): Task? = taskDao.getById(id)

    suspend fun getIncomplete(): List<Task> = taskDao.getIncomplete()

    suspend fun insert(task: Task): Long {
        val toSave = withPriority(task)
        val id = taskDao.insert(toSave)
        reminderScheduler.schedule(toSave.copy(id = id))
        return id
    }

    suspend fun update(task: Task) {
        val toSave = withPriority(task)
        taskDao.update(toSave)
        reminderScheduler.schedule(toSave)
    }

    suspend fun delete(task: Task) {
        reminderScheduler.cancel(task.id)
        taskDao.delete(task)
    }

    suspend fun setCompleted(id: Long, completed: Boolean) {
        val completedAt = if (completed) System.currentTimeMillis() else null
        taskDao.setCompleted(id, completed, completedAt)
        if (completed) {
            reminderScheduler.cancel(id)
        } else {
            taskDao.getById(id)?.let { reminderScheduler.schedule(it) }
        }
    }

    fun observeCompletedCount(): Flow<Int> = taskDao.observeCompletedCount()

    fun observePendingCount(): Flow<Int> = taskDao.observePendingCount()

    fun observeCompletedCountBetween(startInclusive: Long, endExclusive: Long): Flow<Int> =
        taskDao.observeCompletedCountBetween(startInclusive, endExclusive)

    fun observeRecentCompleted(limit: Int): Flow<List<Task>> =
        taskDao.observeRecentCompleted(limit)

    fun observeCompletedAtTimes(): Flow<List<Long>> = taskDao.observeCompletedAtTimes()

    fun explainPriority(task: Task): PriorityResult = priorityCalculator.calculate(task)

    private fun withPriority(task: Task): Task {
        val result = priorityCalculator.calculate(task)
        return task.copy(priorityScore = result.score)
    }
}
