package dev.suks.pupdr.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** One exercise from the bundled library (or one the user created). Lists are stored comma/newline joined. */
@Entity(tableName = "exercises", indices = [Index("name")])
data class Exercise(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val equipment: String,
    val level: String,
    val primaryMuscles: String,
    val secondaryMuscles: String,
    val instructions: String,
    val custom: Boolean = false,
) {
    val primaryList: List<String> get() = primaryMuscles.split(',').filter { it.isNotBlank() }
}

@Entity(tableName = "routines")
data class Routine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "routine_exercises",
    indices = [Index("routineId")],
)
data class RoutineExercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineId: Long,
    val exerciseId: String,
    val position: Int,
    val targetSets: Int = 3,
    val targetReps: String = "10",
)

/** A single logged set. [epochDay] is LocalDate.toEpochDay(), so ranges are simple integer comparisons. */
@Entity(
    tableName = "workout_sets",
    indices = [Index("epochDay"), Index("exerciseId")],
)
data class WorkoutSet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val exerciseId: String,
    val setIndex: Int,
    val reps: Int = 0,
    val weight: Double = 0.0,
    val done: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

/** Joined row used by the workout screen and reports. */
data class SetWithExercise(
    val id: Long,
    val epochDay: Long,
    val exerciseId: String,
    val setIndex: Int,
    val reps: Int,
    val weight: Double,
    val done: Boolean,
    val createdAt: Long,
    val exerciseName: String,
    val primaryMuscles: String,
    val category: String,
)

data class RoutineExerciseRow(
    val id: Long,
    val routineId: Long,
    val exerciseId: String,
    val position: Int,
    val targetSets: Int,
    val targetReps: String,
    val exerciseName: String,
    val primaryMuscles: String,
    val category: String,
)

data class RoutineSummary(
    val id: Long,
    val name: String,
    val notes: String,
    val exerciseCount: Int,
    val totalSets: Int = 0,
    val preview: String? = null,
)

/** A per-day number for a daily habit: glasses of water, creatine taken (0/1). */
@Entity(tableName = "daily_logs", primaryKeys = ["epochDay", "key"])
data class DailyLog(val epochDay: Long, val key: String, val value: Double)

/** Something eaten, for the protein / calorie tally. */
@Entity(tableName = "food_entries", indices = [Index("epochDay")])
data class FoodEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val name: String,
    val protein: Double,
    val calories: Int,
    val createdAt: Long = System.currentTimeMillis(),
)
