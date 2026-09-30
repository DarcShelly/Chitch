package com.chitch.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.chitch.app.data.dao.DailyHabitCheckDao
import com.chitch.app.data.dao.DailyReviewDao
import com.chitch.app.data.dao.HabitDao
import com.chitch.app.data.dao.RoutineDao
import com.chitch.app.data.dao.TaskDao
import com.chitch.app.data.dao.TaskLedgerDao
import com.chitch.app.data.dao.UserDao
import com.chitch.app.data.entity.DailyHabitCheckEntity
import com.chitch.app.data.entity.DailyReviewEntity
import com.chitch.app.data.entity.HabitEntity
import com.chitch.app.data.entity.RoutineEntity
import com.chitch.app.data.entity.TaskEntity
import com.chitch.app.data.entity.TaskLedgerEntity
import com.chitch.app.data.entity.UserEntity

@Database(
    entities = [
        TaskEntity::class,
        RoutineEntity::class,
        UserEntity::class,
        HabitEntity::class,
        TaskLedgerEntity::class,
        DailyReviewEntity::class,
        DailyHabitCheckEntity::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun routineDao(): RoutineDao
    abstract fun userDao(): UserDao
    abstract fun habitDao(): HabitDao
    abstract fun taskLedgerDao(): TaskLedgerDao
    abstract fun dailyReviewDao(): DailyReviewDao
    abstract fun dailyHabitCheckDao(): DailyHabitCheckDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "chitch.db"
                )
                    // Pre-release schema, no shipped user data yet — destructive migration
                    // is simpler than hand-writing Migrations for every iteration.
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build().also { instance = it }
            }

        /** Closes and drops the cached singleton so the next [getInstance] reopens the on-disk file fresh. */
        fun closeInstance() {
            synchronized(this) {
                instance?.close()
                instance = null
            }
        }
    }
}
