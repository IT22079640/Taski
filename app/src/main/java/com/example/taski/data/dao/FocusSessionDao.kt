package com.example.taski.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.taski.data.entity.FocusSession
import com.example.taski.data.entity.FocusSessionWithTask
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

    @Query("SELECT COALESCE(SUM(duration), 0) FROM focus_sessions")
    fun observeTotalSavedDuration(): Flow<Long>

    @Query(
        "SELECT COALESCE(SUM(duration), 0) FROM focus_sessions " +
            "WHERE startTime >= :startInclusive AND startTime < :endExclusive"
    )
    fun observeSavedDurationBetween(startInclusive: Long, endExclusive: Long): Flow<Long>

    @Query("SELECT COUNT(*) FROM focus_sessions WHERE completed = 1")
    fun observeCompletedSessionCount(): Flow<Int>

    @Query("SELECT * FROM focus_sessions ORDER BY startTime DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<FocusSession>>

    @Query(
        "SELECT focus_sessions.id, focus_sessions.taskId, focus_sessions.duration, " +
            "focus_sessions.startTime, focus_sessions.completed, tasks.title AS taskTitle " +
            "FROM focus_sessions INNER JOIN tasks ON tasks.id = focus_sessions.taskId " +
            "ORDER BY focus_sessions.startTime DESC LIMIT :limit"
    )
    fun observeRecentWithTask(limit: Int): Flow<List<FocusSessionWithTask>>
}
