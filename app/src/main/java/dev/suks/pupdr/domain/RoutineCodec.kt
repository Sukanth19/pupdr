package dev.suks.pupdr.domain

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

/** Writes routines as JSON for sharing. */
object RoutineCodec {
    private val json = Json { prettyPrint = true; encodeDefaults = true }

    fun encode(routines: List<RoutineDto>): String = json.encodeToString(RoutineFile.serializer(), RoutineFile(routines = routines))

    fun customId(name: String) = "custom_" + name.lowercase().replace(Regex("[^a-z0-9]+"), " ").trim().replace(' ', '_')
}
