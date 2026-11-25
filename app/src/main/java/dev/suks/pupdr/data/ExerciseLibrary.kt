package dev.suks.pupdr.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Parses the bundled exercise library (assets/exercises.json), derived from
 * free-exercise-db (https://github.com/yuhonas/free-exercise-db, public domain / Unlicense).
 */
object ExerciseLibrary {
    @Serializable
    private data class Raw(
        val id: String,
        val name: String,
        val category: String = "other",
        val equipment: String = "none",
        val level: String = "",
        val primary: List<String> = emptyList(),
        val secondary: List<String> = emptyList(),
        val instructions: List<String> = emptyList(),
    )

    private val json = Json { ignoreUnknownKeys = true }

    fun parse(text: String): List<Exercise> =
        json.decodeFromString<List<Raw>>(text).map {
            Exercise(
                id = it.id,
                name = it.name,
                category = it.category,
                equipment = it.equipment,
                level = it.level,
                primaryMuscles = it.primary.joinToString(","),
                secondaryMuscles = it.secondary.joinToString(","),
                instructions = it.instructions.joinToString("\n"),
            )
        }
}
