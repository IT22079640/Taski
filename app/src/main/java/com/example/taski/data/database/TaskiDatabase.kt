package com.example.taski.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.taski.data.dao.FocusSessionDao
import com.example.taski.data.dao.TaskDao
import com.example.taski.data.entity.FocusSession
import com.example.taski.data.entity.Task
import com.example.taski.utils.DateUtils
import java.util.TimeZone

@Database(
    entities = [Task::class, FocusSession::class],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class TaskiDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun focusSessionDao(): FocusSessionDao

    companion object {
        private const val DATABASE_NAME = "taski.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN completedAt INTEGER")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val timeZone = TimeZone.getDefault()
                db.query("SELECT id, deadline FROM tasks").use { cursor ->
                    val idIndex = cursor.getColumnIndex("id")
                    val deadlineIndex = cursor.getColumnIndex("deadline")
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idIndex)
                        val deadline = cursor.getLong(deadlineIndex)
                        val migrated = DateUtils.withDefaultEveningIfDateOnly(deadline, timeZone)
                        if (migrated != deadline) {
                            db.execSQL(
                                "UPDATE tasks SET deadline = ? WHERE id = ?",
                                arrayOf(migrated, id)
                            )
                        }
                    }
                }
            }
        }

        @Volatile
        private var INSTANCE: TaskiDatabase? = null

        fun getInstance(context: Context): TaskiDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    TaskiDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
