package com.example.taski.data.repository

import com.example.taski.data.dao.TaskDao
import com.example.taski.data.entity.Task
import com.example.taski.data.entity.TaskWithFocusSessions
import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao) {

    fun observeAllByPriority(): Flow<List<Task>> = taskDao.observeAllByPriority()

    fun observeIncomplete(): Flow<List<Task>> = taskDao.observeIncomplete()

    fun observeCompleted(): Flow<List<Task>> = taskDao.observeCompleted()

    fun observeById(id: Long): Flow<Task?> = taskDao.observeById(id)

    fun observeWithSessions(id: Long): Flow<TaskWithFocusSessions?> =
        taskDao.observeWithSessions(id)

    suspend fun getById(id: Long): Task? = taskDao.getById(id)

    suspend fun insert(task: Task): Long = taskDao.insert(task)

    suspend fun update(task: Task) = taskDao.update(task)

    suspend fun delete(task: Task) = taskDao.delete(task)

    suspend fun setCompleted(id: Long, completed: Boolean) = taskDao.setCompleted(id, completed)
}
