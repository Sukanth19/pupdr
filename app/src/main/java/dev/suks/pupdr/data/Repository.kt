package dev.suks.pupdr.data

import android.content.Context
import androidx.room.withTransaction
import dev.suks.pupdr.domain.DraftItem
import dev.suks.pupdr.domain.DraftRoutine
import dev.suks.pupdr.domain.ExerciseMatcher
import dev.suks.pupdr.domain.RoutineCodec
import dev.suks.pupdr.domain.RoutineDto
import dev.suks.pupdr.domain.RoutineItemDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

data class ImportResult(val routines: Int, val exercises: Int, val created: List<String>)

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

    // ---- routines
    val routines: Flow<List<RoutineSummary>> = routineDao.summaries()
    fun routine(id: Long) = routineDao.routine(id)
    fun routineItems(id: Long) = routineDao.items(id)

    suspend fun createRoutine(name: String): Long = routineDao.insert(Routine(name = name.trim().ifEmpty { "New routine" }))

    suspend fun renameRoutine(id: Long, name: String, notes: String) {
        routineDao.routineOnce(id)?.let { routineDao.update(it.copy(name = name.trim().ifEmpty { it.name }, notes = notes)) }
    }

    suspend fun deleteRoutine(id: Long) = routineDao.delete(id)

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

    /** Parses any routine text/JSON into drafts matched against the library, for review before saving. */
    suspend fun draftImport(text: String): List<DraftRoutine> = withContext(Dispatchers.Default) {
        val matcher = ExerciseMatcher(exerciseDao.allOnce())
        var key = 0L
        RoutineCodec.decode(text, matcher::isKnown).map { dto ->
            DraftRoutine(key++, dto.name, dto.exercises.map { item ->
                val m = matcher.match(item.name, item.id)
                DraftItem(key++, item.name, m?.exercise?.id, m?.exercise?.name ?: item.name, m?.score ?: 0.0, item.sets, item.reps)
            })
        }.filter { it.items.isNotEmpty() }
    }

    /** Saves reviewed drafts; unmatched items become custom exercises. */
    suspend fun commitImport(drafts: List<DraftRoutine>): ImportResult = db.withTransaction {
        val created = linkedSetOf<String>()
        var count = 0
        for (d in drafts) {
            if (d.items.isEmpty()) continue
            val rid = routineDao.insert(Routine(name = d.name.ifBlank { "Imported routine" }))
            routineDao.insertItems(d.items.mapIndexed { i, item ->
                val id = item.exerciseId ?: run {
                    val existing = exerciseDao.byId(RoutineCodec.customId(item.exerciseName))
                    existing?.id ?: addCustomExercise(item.exerciseName, "", "other").also { created += it.name }.id
                }
                count++
                RoutineExercise(routineId = rid, exerciseId = id, position = i, targetSets = item.sets.coerceIn(1, 50), targetReps = item.reps)
            })
        }
        ImportResult(drafts.count { it.items.isNotEmpty() }, count, created.toList())
    }

    suspend fun importRoutines(text: String): ImportResult {
        val drafts = draftImport(text)
        require(drafts.isNotEmpty()) { "No exercises found" }
        return commitImport(drafts)
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
