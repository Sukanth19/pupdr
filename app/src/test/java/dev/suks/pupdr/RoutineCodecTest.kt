package dev.suks.pupdr

import dev.suks.pupdr.data.Exercise
import dev.suks.pupdr.domain.RoutineCodec
import dev.suks.pupdr.domain.RoutineDto
import dev.suks.pupdr.domain.RoutineItemDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RoutineCodecTest {
    private fun ex(id: String, name: String) = Exercise(id, name, "strength", "barbell", "", "chest", "", "")
    private val lib = listOf(
        ex("bench", "Barbell Bench Press - Medium Grip"),
        ex("dips", "Dips - Chest Version"),
        ex("ohp", "Standing Military Press"),
        ex("squat", "Barbell Squat"),
    )

    @Test fun jsonRoundTrip() {
        val r = listOf(RoutineDto("Push", "notes", listOf(RoutineItemDto("bench", "Barbell Bench Press - Medium Grip", 4, "6-8"))))
        assertEquals(r, RoutineCodec.decode(RoutineCodec.encode(r)))
    }

    @Test fun singleObjectAndArray() {
        assertEquals("A", RoutineCodec.decode("""{"name":"A","exercises":[{"name":"Squat"}]}""").single().name)
        assertEquals(2, RoutineCodec.decode("""[{"name":"A","exercises":[{"name":"x"}]},{"name":"B","exercises":[{"name":"y"}]}]""").size)
    }

    @Test fun plainText() {
        val text = """
            # Push Day
            Bench Press 4x8
            - Overhead press, 3 x 10-12
            Dips

            Legs:
            1. Squat 5×5
        """.trimIndent()
        val rs = RoutineCodec.decode(text)
        assertEquals(listOf("Push Day", "Legs"), rs.map { it.name })
        assertEquals(RoutineItemDto(name = "Bench Press", sets = 4, reps = "8"), rs[0].exercises[0])
        assertEquals(RoutineItemDto(name = "Overhead press", sets = 3, reps = "10-12"), rs[0].exercises[1])
        assertEquals(RoutineItemDto(name = "Dips"), rs[0].exercises[2])
        assertEquals(RoutineItemDto(name = "Squat", sets = 5, reps = "5"), rs[1].exercises[0])
    }

    @Test fun matching() {
        assertEquals("bench", RoutineCodec.match(RoutineItemDto(name = "bench press"), lib)?.id)
        assertEquals("squat", RoutineCodec.match(RoutineItemDto(name = "Barbell squat"), lib)?.id)
        assertEquals("dips", RoutineCodec.match(RoutineItemDto(name = "dip"), lib)?.id)
        assertEquals("ohp", RoutineCodec.match(RoutineItemDto(id = "ohp", name = "whatever"), lib)?.id)
        assertNull(RoutineCodec.match(RoutineItemDto(name = "Zercher carry"), lib))
    }
}
