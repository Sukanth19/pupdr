package dev.suks.pupdr.data

import android.content.Context
import androidx.room.withTransaction
import dev.suks.pupdr.domain.RoutineCodec
import dev.suks.pupdr.domain.RoutineDto
import dev.suks.pupdr.domain.RoutineItemDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class Repository(private val db: PupdrDatabase) {
    private val exerciseDao = db.exercises()
    private val routineDao = db.routines()
    private val workoutDao = db.workouts()

    suspend fun ensureSeeded(context: Context) = withContext(Dispatchers.IO) {
        if (exerciseDao.count() == 0) {
            val text = context.assets.open("exercises.json").bufferedReader().use { it.readText() }
            exerciseDao.insertAll(ExerciseLibrary.parse(text))
        }
    }

    // ---- exercises
    val exercises: Flow<List<Exercise>> = exerciseDao.all()

    suspend fun addCustomExercise(name: String, muscle: String, equipment: String, category: String = "strength"): Exercise {
        val ex = Exercise(
            id = RoutineCodec.customId(name),
            name = name.trim(),
            category = category,
            equipment = equipment.ifBlank { "other" },
            level = "",
            primaryMuscles = muscle.trim().lowercase(),
            secondaryMuscles = "",
            instructions = "",
            custom = true,
        )
        exerciseDao.upsert(ex)
        return ex
    }

    /** Deletes a user-created exercise everywhere it's used. Library exercises can't be deleted. */
    suspend fun deleteCustomExercise(id: String) = db.withTransaction {
        val ex = exerciseDao.byId(id) ?: return@withTransaction
        if (!ex.custom) return@withTransaction
        workoutDao.deleteAllFor(id)
        routineDao.deleteItemsFor(id)
        exerciseDao.delete(id)
    }

    /**
     * One-off cleanup for the old text importer, which turned lines of notes into "custom exercises".
     * Removes custom exercises that read like sentences and were never logged.
     */
    suspend fun removeJunkCustomExercises(): Int = db.withTransaction {
        val junk = exerciseDao.allOnce().filter { it.custom && looksLikeProse(it.name) && workoutDao.countFor(it.id) == 0 }
        junk.forEach {
            routineDao.deleteItemsFor(it.id)
            exerciseDao.delete(it.id)
        }
        junk.size
    }

    private fun looksLikeProse(name: String): Boolean =
        name.length > 40 || name.split(' ').size > 6 || name.contains("**") ||
            Regex("""[.:!?·=]""").containsMatchIn(name) || name.count { it.isDigit() } > 3

    // ---- routines
    val routines: Flow<List<RoutineSummary>> = routineDao.summaries()
    fun routine(id: Long) = routineDao.routine(id)
    fun routineItems(id: Long) = routineDao.items(id)

    suspend fun createRoutine(name: String): Long = routineDao.insert(Routine(name = name.trim()))

    suspend fun renameRoutine(id: Long, name: String, notes: String) {
        routineDao.routineOnce(id)?.let { routineDao.update(it.copy(name = name, notes = notes)) }
    }

    suspend fun deleteRoutine(id: Long) = routineDao.delete(id)

    /** Creates a routine already containing [exerciseIds]. */
    suspend fun createRoutineWith(name: String, exerciseIds: List<String>): Long = db.withTransaction {
        val id = createRoutine(name)
        addToRoutine(id, exerciseIds)
        id
    }

    /** Called when leaving a routine editor: unnamed ones get a name, untouched new ones are discarded. */
    suspend fun finishEditing(id: Long, wasNew: Boolean) {
        val r = routineDao.routineOnce(id) ?: return
        val count = routineDao.itemsOnce(id).size
        when {
            wasNew && count == 0 && r.name.isBlank() -> routineDao.delete(id)
            r.name.isBlank() -> routineDao.update(r.copy(name = "Routine ${id}"))
        }
    }

    suspend fun addToRoutine(routineId: Long, exerciseIds: List<String>) = db.withTransaction {
        var pos = routineDao.maxPosition(routineId)
        routineDao.insertItems(exerciseIds.map { RoutineExercise(routineId = routineId, exerciseId = it, position = ++pos) })
    }

    suspend fun updateTarget(itemId: Long, sets: Int, reps: String) = routineDao.updateTarget(itemId, sets.coerceIn(1, 50), reps.ifBlank { "10" })
    suspend fun removeRoutineItem(itemId: Long) = routineDao.deleteItem(itemId)

    suspend fun moveRoutineItem(routineId: Long, itemId: Long, delta: Int) = db.withTransaction {
        val items = routineDao.itemsOnce(routineId).toMutableList()
        val i = items.indexOfFirst { it.id == itemId }
        val j = i + delta
        if (i < 0 || j !in items.indices) return@withTransaction
        val moved = items.removeAt(i)
        items.add(j, moved)
        items.forEachIndexed { idx, row -> routineDao.setPosition(row.id, idx) }
    }

    suspend fun exportRoutines(ids: List<Long>): String {
        val dtos = ids.mapNotNull { id ->
            val r = routineDao.routineOnce(id) ?: return@mapNotNull null
            RoutineDto(
                name = r.name,
                notes = r.notes,
                exercises = routineDao.itemsOnce(id).map { RoutineItemDto(it.exerciseId, it.exerciseName, it.targetSets, it.targetReps) },
            )
        }
        return RoutineCodec.encode(dtos)
    }

    // ---- workouts
    fun setsForDay(day: Long) = workoutDao.setsForDay(day)
    fun setsBetween(from: Long, to: Long) = workoutDao.setsBetween(from, to)
    val trainedDays = workoutDao.trainedDays()
    suspend fun firstDay() = workoutDao.firstDay()
    suspend fun setsBetweenOnce(from: Long, to: Long) = workoutDao.setsBetweenOnce(from, to)

    private fun firstNumber(reps: String) = Regex("""\d+""").find(reps)?.value?.toIntOrNull() ?: 0

    suspend fun addExercisesToDay(day: Long, exerciseIds: List<String>, sets: Int = 1, reps: String = "") = db.withTransaction {
        for (id in exerciseIds) appendSets(day, id, sets, reps)
    }

    suspend fun startRoutine(day: Long, routineId: Long) = db.withTransaction {
        for (item in routineDao.itemsOnce(routineId)) appendSets(day, item.exerciseId, item.targetSets, item.targetReps)
    }

    private suspend fun appendSets(day: Long, exerciseId: String, count: Int, reps: String) {
        val last = workoutDao.lastSetBefore(exerciseId, day)
        val start = workoutDao.maxSetIndex(day, exerciseId)
        val r = firstNumber(reps).takeIf { it > 0 } ?: 0
        workoutDao.insertAll((1..count).map {
            WorkoutSet(epochDay = day, exerciseId = exerciseId, setIndex = start + it, reps = r, weight = last?.weight ?: 0.0)
        })
    }

    /** Adds one more set to an exercise, copying the previous set's numbers. */
    suspend fun addSet(day: Long, exerciseId: String, copyFrom: SetWithExercise?) {
        val idx = workoutDao.maxSetIndex(day, exerciseId) + 1
        workoutDao.insert(WorkoutSet(epochDay = day, exerciseId = exerciseId, setIndex = idx, reps = copyFrom?.reps ?: 0, weight = copyFrom?.weight ?: 0.0))
    }

    suspend fun updateSet(id: Long, reps: Int, weight: Double, done: Boolean) = workoutDao.update(id, reps, weight, done)
    suspend fun deleteSet(id: Long) = workoutDao.delete(id)
    suspend fun removeExerciseFromDay(day: Long, exerciseId: String) = workoutDao.deleteExerciseOnDay(day, exerciseId)
}
