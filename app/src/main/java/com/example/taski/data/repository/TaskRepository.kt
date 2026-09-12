package com.example.taski.data.repository

import com.example.taski.data.dao.TaskDao
import com.example.taski.data.entity.Task
import com.example.taski.priority.PriorityCalculator
import com.example.taski.priority.PriorityResult
import com.example.taski.reminder.TaskReminderScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

class TaskRepository(
    private val taskDao: TaskDao,
    private val priorityCalculator: PriorityCalculator = PriorityCalculator(),
    private val reminderScheduler: TaskReminderScheduler = TaskReminderScheduler.NoOp,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    private val displayEpoch = MutableStateFlow(0)

    fun refreshDisplayPriority() {
        displayEpoch.value = displayEpoch.value + 1
    }

    fun observeAllByPriority(): Flow<List<Task>> =
        combine(taskDao.observeAllByPriority(), displayEpoch) { tasks, _ ->
            sortForDisplay(tasks.map { it.withDisplayPriority(clock()) })
        }

    fun observeIncomplete(): Flow<List<Task>> =
        combine(taskDao.observeIncomplete(), displayEpoch) { tasks, _ ->
            sortPendingByCurrentPriority(tasks.map { it.withDisplayPriority(clock()) })
        }

    fun observeCompleted(): Flow<List<Task>> = taskDao.observeCompleted()

    fun observeById(id: Long): Flow<Task?> =
        combine(taskDao.observeById(id), displayEpoch) { task, _ ->
            task?.withDisplayPriority(clock())
        }

    suspend fun getById(id: Long): Task? = taskDao.getById(id)?.withDisplayPriority(clock())

    suspend fun getIncomplete(): List<Task> =
        sortPendingByCurrentPriority(taskDao.getIncomplete().map { it.withDisplayPriority(clock()) })

    suspend fun insert(task: Task): Long {
        val toSave = withPrioritySnapshot(task)
        val id = taskDao.insert(toSave)
        reminderScheduler.schedule(toSave.copy(id = id))
        return id
    }

    suspend fun update(task: Task) {
        val toSave = withPrioritySnapshot(task)
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

    fun explainPriority(task: Task): PriorityResult =
        priorityCalculator.calculate(task, clock())

    private fun withPrioritySnapshot(task: Task): Task {
        val result = priorityCalculator.calculate(task, clock())
        return task.copy(priorityScore = result.score)
    }

    private fun Task.withDisplayPriority(nowMillis: Long): Task {
        if (completed) return this
        val current = priorityCalculator.calculate(this, nowMillis).score
        return copy(priorityScore = current)
    }

    private fun sortPendingByCurrentPriority(tasks: List<Task>): List<Task> =
        tasks.sortedWith(pendingComparator)

    private fun sortForDisplay(tasks: List<Task>): List<Task> =
        tasks.sortedWith(
            compareBy<Task> { it.completed }
                .thenByDescending { it.priorityScore }
                .thenBy { it.deadline }
                .thenBy { it.id }
        )

    private companion object {
        val pendingComparator: Comparator<Task> =
            compareByDescending<Task> { it.priorityScore }
                .thenBy { it.deadline }
                .thenBy { it.id }
    }
}
