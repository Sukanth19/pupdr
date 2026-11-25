package dev.suks.pupdr.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<Exercise>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: Exercise)

    @Query("SELECT * FROM exercises ORDER BY name COLLATE NOCASE")
    fun all(): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises")
    suspend fun allOnce(): List<Exercise>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun byId(id: String): Exercise?
}

@Dao
interface RoutineDao {
    @Query(
        """SELECT r.id, r.name, r.notes, COUNT(re.id) AS exerciseCount
           FROM routines r LEFT JOIN routine_exercises re ON re.routineId = r.id
           GROUP BY r.id ORDER BY r.createdAt DESC"""
    )
    fun summaries(): Flow<List<RoutineSummary>>

    @Query("SELECT * FROM routines WHERE id = :id")
    fun routine(id: Long): Flow<Routine?>

    @Query("SELECT * FROM routines WHERE id = :id")
    suspend fun routineOnce(id: Long): Routine?

    @Query(
        """SELECT re.id, re.routineId, re.exerciseId, re.position, re.targetSets, re.targetReps,
                  e.name AS exerciseName, e.primaryMuscles
           FROM routine_exercises re JOIN exercises e ON e.id = re.exerciseId
           WHERE re.routineId = :routineId ORDER BY re.position"""
    )
    fun items(routineId: Long): Flow<List<RoutineExerciseRow>>

    @Query(
        """SELECT re.id, re.routineId, re.exerciseId, re.position, re.targetSets, re.targetReps,
                  e.name AS exerciseName, e.primaryMuscles
           FROM routine_exercises re JOIN exercises e ON e.id = re.exerciseId
           WHERE re.routineId = :routineId ORDER BY re.position"""
    )
    suspend fun itemsOnce(routineId: Long): List<RoutineExerciseRow>

    @Insert
    suspend fun insert(r: Routine): Long

    @Update
    suspend fun update(r: Routine)

    @Query("DELETE FROM routines WHERE id = :id")
    suspend fun deleteRoutine(id: Long)

    @Query("DELETE FROM routine_exercises WHERE routineId = :id")
    suspend fun deleteItemsOf(id: Long)

    @Insert
    suspend fun insertItems(items: List<RoutineExercise>)

    @Query("UPDATE routine_exercises SET targetSets = :sets, targetReps = :reps WHERE id = :id")
    suspend fun updateTarget(id: Long, sets: Int, reps: String)

    @Query("DELETE FROM routine_exercises WHERE id = :id")
    suspend fun deleteItem(id: Long)

    @Query("UPDATE routine_exercises SET position = :position WHERE id = :id")
    suspend fun setPosition(id: Long, position: Int)

    @Query("SELECT COALESCE(MAX(position), -1) FROM routine_exercises WHERE routineId = :id")
    suspend fun maxPosition(id: Long): Int

    @Transaction
    suspend fun delete(id: Long) {
        deleteItemsOf(id)
        deleteRoutine(id)
    }
}

@Dao
interface WorkoutDao {
    @Query(
        """SELECT s.id, s.epochDay, s.exerciseId, s.setIndex, s.reps, s.weight, s.done, s.createdAt,
                  e.name AS exerciseName, e.primaryMuscles, e.category
           FROM workout_sets s JOIN exercises e ON e.id = s.exerciseId
           WHERE s.epochDay = :day ORDER BY s.createdAt, s.setIndex"""
    )
    fun setsForDay(day: Long): Flow<List<SetWithExercise>>

    @Query(
        """SELECT s.id, s.epochDay, s.exerciseId, s.setIndex, s.reps, s.weight, s.done, s.createdAt,
                  e.name AS exerciseName, e.primaryMuscles, e.category
           FROM workout_sets s JOIN exercises e ON e.id = s.exerciseId
           WHERE s.epochDay BETWEEN :from AND :to ORDER BY s.epochDay, s.createdAt, s.setIndex"""
    )
    fun setsBetween(from: Long, to: Long): Flow<List<SetWithExercise>>

    @Query(
        """SELECT s.id, s.epochDay, s.exerciseId, s.setIndex, s.reps, s.weight, s.done, s.createdAt,
                  e.name AS exerciseName, e.primaryMuscles, e.category
           FROM workout_sets s JOIN exercises e ON e.id = s.exerciseId
           WHERE s.epochDay BETWEEN :from AND :to ORDER BY s.epochDay, s.createdAt, s.setIndex"""
    )
    suspend fun setsBetweenOnce(from: Long, to: Long): List<SetWithExercise>

    @Query("SELECT DISTINCT epochDay FROM workout_sets WHERE done = 1 ORDER BY epochDay DESC")
    fun trainedDays(): Flow<List<Long>>

    @Query("SELECT MIN(epochDay) FROM workout_sets")
    suspend fun firstDay(): Long?

    @Query("SELECT COALESCE(MAX(setIndex), 0) FROM workout_sets WHERE epochDay = :day AND exerciseId = :exerciseId")
    suspend fun maxSetIndex(day: Long, exerciseId: String): Int

    /** Last weight/reps used for an exercise before [day], to prefill new sets. */
    @Query(
        """SELECT * FROM workout_sets WHERE exerciseId = :exerciseId AND epochDay < :day AND done = 1
           ORDER BY epochDay DESC, setIndex DESC LIMIT 1"""
    )
    suspend fun lastSetBefore(exerciseId: String, day: Long): WorkoutSet?

    @Insert
    suspend fun insert(set: WorkoutSet): Long

    @Insert
    suspend fun insertAll(sets: List<WorkoutSet>)

    @Query("UPDATE workout_sets SET reps = :reps, weight = :weight, done = :done WHERE id = :id")
    suspend fun update(id: Long, reps: Int, weight: Double, done: Boolean)

    @Query("DELETE FROM workout_sets WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM workout_sets WHERE epochDay = :day AND exerciseId = :exerciseId")
    suspend fun deleteExerciseOnDay(day: Long, exerciseId: String)
}
