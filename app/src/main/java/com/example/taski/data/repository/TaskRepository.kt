package com.example.taski.data.repository

import com.example.taski.data.dao.TaskDao
import com.example.taski.data.entity.Task
import com.example.taski.data.entity.TaskWithFocusSessions
import com.example.taski.priority.PriorityCalculator
import kotlinx.coroutines.flow.Flow

class TaskRepository(
    private val taskDao: TaskDao,
    private val priorityCalculator: PriorityCalculator = PriorityCalculator()
) {

    fun observeAllByPriority(): Flow<List<Task>> = taskDao.observeAllByPriority()

    fun observeIncomplete(): Flow<List<Task>> = taskDao.observeIncomplete()

    fun observeCompleted(): Flow<List<Task>> = taskDao.observeCompleted()

    fun observeById(id: Long): Flow<Task?> = taskDao.observeById(id)

    fun observeWithSessions(id: Long): Flow<TaskWithFocusSessions?> =
        taskDao.observeWithSessions(id)

    suspend fun getById(id: Long): Task? = taskDao.getById(id)

    suspend fun insert(task: Task): Long = taskDao.insert(withPriority(task))

    suspend fun update(task: Task) = taskDao.update(withPriority(task))

    suspend fun delete(task: Task) = taskDao.delete(task)

    suspend fun setCompleted(id: Long, completed: Boolean) = taskDao.setCompleted(id, completed)

    private fun withPriority(task: Task): Task {
        val result = priorityCalculator.calculate(task)
        return task.copy(priorityScore = result.score)
    }
}
