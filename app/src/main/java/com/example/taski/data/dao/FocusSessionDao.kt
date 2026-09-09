package com.example.taski.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.taski.data.entity.FocusSession
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusSessionDao {
    @Query("SELECT * FROM focus_sessions ORDER BY startTime DESC")
    fun observeAll(): Flow<List<FocusSession>>

    @Query("SELECT * FROM focus_sessions WHERE taskId = :taskId ORDER BY startTime DESC")
    fun observeByTaskId(taskId: Long): Flow<List<FocusSession>>

    @Query("SELECT * FROM focus_sessions WHERE id = :id")
    suspend fun getById(id: Long): FocusSession?

    @Insert
    suspend fun insert(session: FocusSession): Long

    @Update
    suspend fun update(session: FocusSession)

    @Delete
    suspend fun delete(session: FocusSession)

    @Query("SELECT COALESCE(SUM(duration), 0) FROM focus_sessions WHERE completed = 1")
    fun observeTotalCompletedDuration(): Flow<Long>

    @Query(
        "SELECT COALESCE(SUM(duration), 0) FROM focus_sessions WHERE taskId = :taskId AND completed = 1"
    )
    fun observeTotalDurationForTask(taskId: Long): Flow<Long>
}
