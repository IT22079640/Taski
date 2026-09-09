package com.example.taski.data.repository

import com.example.taski.data.dao.FocusSessionDao
import com.example.taski.data.entity.FocusSession
import kotlinx.coroutines.flow.Flow

class FocusSessionRepository(private val focusSessionDao: FocusSessionDao) {

    fun observeAll(): Flow<List<FocusSession>> = focusSessionDao.observeAll()

    fun observeByTaskId(taskId: Long): Flow<List<FocusSession>> =
        focusSessionDao.observeByTaskId(taskId)

    fun observeTotalCompletedDuration(): Flow<Long> =
        focusSessionDao.observeTotalCompletedDuration()

    fun observeTotalDurationForTask(taskId: Long): Flow<Long> =
        focusSessionDao.observeTotalDurationForTask(taskId)

    suspend fun getById(id: Long): FocusSession? = focusSessionDao.getById(id)

    suspend fun insert(session: FocusSession): Long = focusSessionDao.insert(session)

    suspend fun update(session: FocusSession) = focusSessionDao.update(session)

    suspend fun delete(session: FocusSession) = focusSessionDao.delete(session)
}
