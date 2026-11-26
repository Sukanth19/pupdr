package dev.suks.pupdr.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Exercise::class, Routine::class, RoutineExercise::class, WorkoutSet::class, DailyLog::class, FoodEntry::class],
    version = 3,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3)],
)
abstract class PupdrDatabase : RoomDatabase() {
    abstract fun exercises(): ExerciseDao
    abstract fun routines(): RoutineDao
    abstract fun workouts(): WorkoutDao
    abstract fun daily(): DailyDao

    companion object {
        fun build(context: Context): PupdrDatabase =
            Room.databaseBuilder(context, PupdrDatabase::class.java, "pupdr.db").build()
    }
}
