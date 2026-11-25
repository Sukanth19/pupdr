package dev.suks.pupdr.domain

import dev.suks.pupdr.data.Exercise
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

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
 * Reads and writes routines. Import accepts:
 *  - pupdr JSON ({"routines":[...]}), a single routine object, or an array of routines
 *  - plain text, e.g.
 *        # Push Day
 *        Bench Press 4x8
 *        - Overhead press, 3 x 10-12
 *        Dips
 */
object RoutineCodec {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true; encodeDefaults = true }
    private val setsReps = Regex("""(\d+)\s*[xX×*]\s*(\d+(?:\s*[-–]\s*\d+)?)\s*$""")
    private val bullet = Regex("""^\s*(?:[-*•]|\d+[.)])\s+""")

    fun encode(routines: List<RoutineDto>): String = json.encodeToString(RoutineFile.serializer(), RoutineFile(routines = routines))

    fun decode(text: String): List<RoutineDto> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return emptyList()
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            val el = runCatching { json.parseToJsonElement(trimmed) }.getOrNull()
            if (el != null) return decodeJson(el)
        }
        return decodeText(trimmed)
    }

    private fun decodeJson(el: kotlinx.serialization.json.JsonElement): List<RoutineDto> = when (el) {
        is JsonArray -> el.map { json.decodeFromJsonElement<RoutineDto>(it) }
        is JsonObject -> when {
            "routines" in el -> json.decodeFromJsonElement<RoutineFile>(el).routines
            else -> listOf(json.decodeFromJsonElement<RoutineDto>(el))
        }
        else -> throw IllegalArgumentException("Unsupported routine format")
    }.filter { it.exercises.isNotEmpty() || it.name.isNotBlank() }

    private fun decodeText(text: String): List<RoutineDto> {
        val out = mutableListOf<RoutineDto>()
        var name: String? = null
        var items = mutableListOf<RoutineItemDto>()
        fun flush() {
            if (items.isNotEmpty()) out += RoutineDto(name ?: "Imported routine", exercises = items)
            items = mutableListOf()
        }
        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isEmpty()) continue
            if (line.startsWith("#") || (line.endsWith(":") && !setsReps.containsMatchIn(line))) {
                flush()
                name = line.trimStart('#').trimEnd(':').trim().ifEmpty { null }
                continue
            }
            val body = line.replace(bullet, "")
            val m = setsReps.find(body)
            if (m != null) {
                val exName = body.substring(0, m.range.first).trim().trimEnd(',', '-', ':', '–').trim()
                if (exName.isNotEmpty()) {
                    items += RoutineItemDto(name = exName, sets = m.groupValues[1].toInt().coerceIn(1, 50), reps = m.groupValues[2].replace(" ", ""))
                }
            } else {
                items += RoutineItemDto(name = body)
            }
        }
        flush()
        return out
    }

    private fun normalize(s: String) = s.lowercase().replace(Regex("[^a-z0-9]+"), " ").trim()

    /** Finds the library exercise that best matches an imported item, or null if nothing plausible. */
    fun match(item: RoutineItemDto, library: List<Exercise>): Exercise? {
        item.id?.let { id -> library.firstOrNull { it.id == id }?.let { return it } }
        val q = normalize(item.name)
        if (q.isEmpty()) return null
        library.firstOrNull { normalize(it.name) == q }?.let { return it }
        val tokens = q.split(' ').filter { it.isNotBlank() }
        return library
            .filter { ex -> val n = normalize(ex.name).split(' ').toSet(); tokens.all { t -> n.any { it == t || it == t + "s" || it + "s" == t } } }
            .minByOrNull { it.name.length }
    }

    fun customId(name: String) = "custom_" + normalize(name).replace(' ', '_')
}
