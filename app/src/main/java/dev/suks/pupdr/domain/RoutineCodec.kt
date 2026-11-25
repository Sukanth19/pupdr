package dev.suks.pupdr.domain

import dev.suks.pupdr.data.Exercise
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class RoutineItemDto(
    val id: String? = null,
    val name: String,
    val sets: Int = 3,
    val reps: String = "10",
)

@Serializable
data class RoutineDto(
    val name: String,
    val notes: String = "",
    val exercises: List<RoutineItemDto> = emptyList(),
)

@Serializable
data class RoutineFile(
    val app: String = "pupdr",
    val version: Int = 1,
    val routines: List<RoutineDto>,
)

/**
 * Reads and writes routines. Import is deliberately forgiving: pupdr JSON, other apps' JSON,
 * markdown / CSV / tab tables, and free-typed text in most of the ways people write workouts.
 */
object RoutineCodec {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true; encodeDefaults = true; isLenient = true }

    fun encode(routines: List<RoutineDto>): String = json.encodeToString(RoutineFile.serializer(), RoutineFile(routines = routines))

    /** @param isKnownExercise true for names that are definitely exercises (helps tell headers from exercises). */
    fun decode(text: String, isKnownExercise: (String) -> Boolean = { false }): List<RoutineDto> {
        val t = text.removePrefix("﻿").trim()
        if (t.isEmpty()) return emptyList()
        if (t.startsWith("{") || t.startsWith("[")) {
            runCatching { json.parseToJsonElement(t) }.getOrNull()?.let { el ->
                val routines = JsonImport.routines(el, "Imported routine").filter { it.exercises.isNotEmpty() }
                if (routines.isNotEmpty()) return routines
            }
        }
        return TextImport(isKnownExercise).parse(t)
    }

    /** Best library match, or null. Kept for callers that don't need a score. */
    fun match(item: RoutineItemDto, library: List<Exercise>): Exercise? = ExerciseMatcher(library).match(item.name, item.id)?.exercise

    fun customId(name: String) = "custom_" + name.lowercase().replace(Regex("[^a-z0-9]+"), " ").trim().replace(' ', '_')
}
