package dev.suks.pupdr.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Exercise::class, Routine::class, RoutineExercise::class, WorkoutSet::class],
    version = 1,
    exportSchema = true,
)
abstract class PupdrDatabase : RoomDatabase() {
    abstract fun exercises(): ExerciseDao
    abstract fun routines(): RoutineDao
    abstract fun workouts(): WorkoutDao

    companion object {
        fun build(context: Context): PupdrDatabase =
            Room.databaseBuilder(context, PupdrDatabase::class.java, "pupdr.db").build()
    }
}
