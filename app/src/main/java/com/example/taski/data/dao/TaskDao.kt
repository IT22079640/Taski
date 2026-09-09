package com.example.taski.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.taski.data.entity.Task
import com.example.taski.data.entity.TaskWithFocusSessions
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY completed ASC, priorityScore DESC, deadline ASC")
    fun observeAllByPriority(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE completed = 0 ORDER BY priorityScore DESC, deadline ASC")
    fun observeIncomplete(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE completed = 1 ORDER BY deadline DESC")
    fun observeCompleted(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE completed = 0")
    suspend fun getIncomplete(): List<Task>

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun observeById(id: Long): Flow<Task?>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: Long): Task?

    @Transaction
    @Query("SELECT * FROM tasks WHERE id = :id")
    fun observeWithSessions(id: Long): Flow<TaskWithFocusSessions?>

    @Insert
    suspend fun insert(task: Task): Long

    @Update
    suspend fun update(task: Task)

    @Delete
    suspend fun delete(task: Task)

    @Query("UPDATE tasks SET completed = :completed, completedAt = :completedAt WHERE id = :id")
    suspend fun setCompleted(id: Long, completed: Boolean, completedAt: Long?)

    @Query("SELECT COUNT(*) FROM tasks WHERE completed = 1")
    fun observeCompletedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM tasks WHERE completed = 0")
    fun observePendingCount(): Flow<Int>

    @Query(
        "SELECT COUNT(*) FROM tasks WHERE completed = 1 AND completedAt IS NOT NULL " +
            "AND completedAt >= :startInclusive AND completedAt < :endExclusive"
    )
    fun observeCompletedCountBetween(startInclusive: Long, endExclusive: Long): Flow<Int>

    @Query(
        "SELECT * FROM tasks WHERE completed = 1 AND completedAt IS NOT NULL " +
            "ORDER BY completedAt DESC LIMIT :limit"
    )
    fun observeRecentCompleted(limit: Int): Flow<List<Task>>

    @Query("SELECT completedAt FROM tasks WHERE completed = 1 AND completedAt IS NOT NULL")
    fun observeCompletedAtTimes(): Flow<List<Long>>
}
