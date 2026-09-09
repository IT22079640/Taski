package com.example.taski.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.taski.data.dao.FocusSessionDao
import com.example.taski.data.dao.TaskDao
import com.example.taski.data.entity.FocusSession
import com.example.taski.data.entity.Task

@Database(
    entities = [Task::class, FocusSession::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class TaskiDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun focusSessionDao(): FocusSessionDao

    companion object {
        private const val DATABASE_NAME = "taski.db"

        @Volatile
        private var INSTANCE: TaskiDatabase? = null

        fun getInstance(context: Context): TaskiDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    TaskiDatabase::class.java,
                    DATABASE_NAME
                ).build().also { INSTANCE = it }
            }
        }
    }
}
