package com.example.taski.data.database

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.taski.utils.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TaskiDatabaseMigrationTest {

    @Test
    fun migrate2To3_setsDateOnlyDeadlinesToSixPmAndPreservesTimedDeadlines() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val timeZone = TimeZone.getDefault()
        val midnight = DateUtils.localDateTimeMillis(
            year = 2026,
            month = Calendar.SEPTEMBER,
            day = 20,
            hourOfDay = 0,
            minute = 0,
            timeZone = timeZone
        )
        val timed = DateUtils.localDateTimeMillis(
            year = 2026,
            month = Calendar.SEPTEMBER,
            day = 21,
            hourOfDay = 16,
            minute = 30,
            timeZone = timeZone
        )
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(null)
                .callback(Version2Callback())
                .build()
        )
        val db = helper.writableDatabase
        insertTask(db, 1L, midnight)
        insertTask(db, 2L, timed)

        TaskiDatabase.MIGRATION_2_3.migrate(db)

        assertEquals(
            DateUtils.withLocalTime(midnight, 18, 0, timeZone),
            deadlineFor(db, 1L)
        )
        assertEquals(timed, deadlineFor(db, 2L))
        db.close()
    }

    private fun insertTask(db: SupportSQLiteDatabase, id: Long, deadline: Long) {
        db.execSQL(
            "INSERT INTO tasks (id, title, description, deadline, importance, estimatedEffort, category, priorityScore, completed, createdAt, completedAt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            arrayOf(id, "Task $id", "", deadline, "HIGH", 60, "Study", 50, 0, deadline, null)
        )
    }

    private fun deadlineFor(db: SupportSQLiteDatabase, id: Long): Long {
        db.query("SELECT deadline FROM tasks WHERE id = $id").use { cursor ->
            cursor.moveToFirst()
            return cursor.getLong(0)
        }
    }

    private class Version2Callback : SupportSQLiteOpenHelper.Callback(2) {
        override fun onCreate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS tasks (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `deadline` INTEGER NOT NULL, `importance` TEXT NOT NULL, `estimatedEffort` INTEGER NOT NULL, `category` TEXT NOT NULL, `priorityScore` INTEGER NOT NULL, `completed` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `completedAt` INTEGER)"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS focus_sessions (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `taskId` INTEGER NOT NULL, `duration` INTEGER NOT NULL, `startTime` INTEGER NOT NULL, `completed` INTEGER NOT NULL, FOREIGN KEY(`taskId`) REFERENCES `tasks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_focus_sessions_taskId` ON `focus_sessions` (`taskId`)")
        }

        override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
    }
}
