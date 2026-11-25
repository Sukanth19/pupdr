package dev.suks.pupdr.domain

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** Walks arbitrary JSON looking for routines and exercises, whatever the key names are. */
internal object JsonImport {
    private val listKeys = listOf("routines", "workouts", "templates", "days", "plans", "program", "programs", "sessions", "splits")
    private val itemKeys = listOf("exercises", "items", "movements", "lifts", "exercise_list", "exerciselist", "workout", "blocks", "entries")
    private val nameKeys = listOf("name", "title", "routine", "label", "day", "workoutname", "routinename", "routine_name")
    private val exNameKeys = listOf("name", "exercise", "exercisename", "exercise_name", "title", "movement", "lift")
    private val idKeys = listOf("id", "exerciseid", "exercise_id")
    private val setsKeys = listOf("sets", "set_count", "setcount", "numsets", "num_sets", "rounds", "target_sets", "targetsets")
    private val repsKeys = listOf("reps", "repetitions", "rep", "reprange", "rep_range", "reps_range", "target", "targetreps", "target_reps", "duration", "time")

    private fun JsonObject.find(keys: List<String>): JsonElement? {
        for (k in keys) entries.firstOrNull { it.key.lowercase() == k }?.let { return it.value }
        return null
    }

    private fun JsonElement.text(): String? = when (this) {
        is JsonPrimitive -> contentOrNull?.trim()?.takeIf { it.isNotEmpty() }
        is JsonObject -> find(exNameKeys)?.text()
        else -> null
    }

    private fun JsonObject.text(keys: List<String>): String? = find(keys)?.text()

    private fun looksLikeExercise(e: JsonElement): Boolean = when (e) {
        is JsonPrimitive -> e.isString
        is JsonObject -> e.find(itemKeys) !is JsonArray && e.find(listKeys) == null && e.text(exNameKeys) != null
        else -> false
    }

    fun routines(el: JsonElement, fallback: String): List<RoutineDto> = when (el) {
        is JsonArray -> when {
            el.isEmpty() -> emptyList()
            el.all { looksLikeExercise(it) } -> listOf(RoutineDto(fallback, exercises = el.mapNotNull(::item)))
            else -> el.flatMapIndexed { i, e -> routines(e, if (el.size > 1) "$fallback ${i + 1}" else fallback) }
        }
        is JsonObject -> {
            val list = el.find(listKeys)
            val items = el.find(itemKeys)
            when {
                list is JsonArray || list is JsonObject -> routines(list, fallback)
                items is JsonArray -> listOf(
                    RoutineDto(
                        name = el.text(nameKeys) ?: fallback,
                        notes = el.text(listOf("notes", "description")) ?: "",
                        exercises = items.mapNotNull(::item),
                    )
                )
                looksLikeExercise(el) -> listOf(RoutineDto(fallback, exercises = listOfNotNull(item(el))))
                // {"Push day": [...], "Pull day": [...]}
                else -> el.entries.flatMap { (k, v) -> if (v is JsonArray || v is JsonObject) routines(v, k) else emptyList() }
            }
        }
        else -> emptyList()
    }

    private fun item(e: JsonElement): RoutineItemDto? {
        if (e is JsonPrimitive) return e.contentOrNull?.let { LineParser.parse(it) }?.toItem()
        if (e !is JsonObject) return null
        val name = e.text(exNameKeys) ?: return null
        val setsEl = e.find(setsKeys)
        val repsEl = e.find(repsKeys)
        var sets: Int? = null
        var reps: String? = null
        when (setsEl) {
            is JsonPrimitive -> sets = setsEl.contentOrNull?.let { Regex("""\d+""").find(it)?.value?.toIntOrNull() }
            is JsonArray -> {
                sets = setsEl.size
                (setsEl.firstOrNull() as? JsonObject)?.find(repsKeys)?.text()?.let { reps = LineParser.normalizeReps(it) }
            }
            else -> {}
        }
        repsEl?.text()?.let { reps = LineParser.normalizeReps(it) }
        if (sets == null && reps == null) {
            // the name itself might carry the scheme, e.g. "Bench 3x8"
            LineParser.parse(name)?.toItem()?.let { return it.copy(id = e.text(idKeys)) }
        }
        return RoutineItemDto(
            id = e.text(idKeys),
            name = name,
            sets = (sets ?: 3).coerceIn(1, 50),
            reps = reps?.takeIf { it.isNotBlank() } ?: "10",
        )
    }
}
