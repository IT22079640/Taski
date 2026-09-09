package com.example.taski.integration

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.taski.data.database.TaskiDatabase
import com.example.taski.data.entity.Importance
import com.example.taski.data.entity.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

internal fun createInMemoryDb(): TaskiDatabase {
    val context = ApplicationProvider.getApplicationContext<Context>()
    return Room.inMemoryDatabaseBuilder(context, TaskiDatabase::class.java)
        .allowMainThreadQueries()
        .build()
}

internal fun sampleTask(
    title: String = "Lab report",
    deadline: Long = DEFAULT_DEADLINE,
    importance: Importance = Importance.HIGH,
    estimatedEffort: Int = 120,
    category: String = "Study",
    priorityScore: Int = 0,
    completed: Boolean = false,
    completedAt: Long? = null,
    createdAt: Long = DEFAULT_CREATED_AT
): Task = Task(
    title = title,
    deadline = deadline,
    importance = importance,
    estimatedEffort = estimatedEffort,
    category = category,
    priorityScore = priorityScore,
    completed = completed,
    createdAt = createdAt,
    completedAt = completedAt
)

internal fun <T> Flow<T>.awaitFirst(): T = runBlocking {
    withTimeout(5_000) { first() }
}

internal const val DEFAULT_DEADLINE = 1_746_489_600_000L + 86_400_000L
internal const val DEFAULT_CREATED_AT = 1_746_489_600_000L
internal const val DAY_MS = 86_400_000L
