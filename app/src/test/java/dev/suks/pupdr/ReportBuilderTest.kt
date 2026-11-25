package dev.suks.pupdr

import dev.suks.pupdr.data.SetWithExercise
import dev.suks.pupdr.domain.DateRange
import dev.suks.pupdr.domain.RangePreset
import dev.suks.pupdr.domain.ReportBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ReportBuilderTest {
    private val d = LocalDate.of(2026, 11, 26) // a thursday
    private fun set(day: LocalDate, ex: String, idx: Int, reps: Int, w: Double, done: Boolean = true) =
        SetWithExercise(0, day.toEpochDay(), ex, idx, reps, w, done, 0, ex.replaceFirstChar { it.uppercase() }, if (ex == "bench") "chest" else "quadriceps", "strength")

    @Test fun presets() {
        assertEquals(DateRange(LocalDate.of(2026, 11, 23), d), DateRange.of(RangePreset.THIS_WEEK, d))
        assertEquals(DateRange(LocalDate.of(2026, 11, 16), LocalDate.of(2026, 11, 22)), DateRange.of(RangePreset.LAST_WEEK, d))
        assertEquals(DateRange(LocalDate.of(2026, 11, 1), d), DateRange.of(RangePreset.THIS_MONTH, d))
        assertEquals(DateRange(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)), DateRange.of(RangePreset.LAST_MONTH, d))
        assertEquals(7, DateRange.of(RangePreset.LAST_7, d).days)
    }

    @Test fun aggregates() {
        val sets = listOf(
            set(d, "bench", 1, 10, 60.0),
            set(d, "bench", 2, 8, 70.0),
            set(d, "squat", 1, 5, 100.0),
            set(d, "squat", 2, 0, 100.0, done = false), // not performed
            set(d.minusDays(1), "squat", 1, 5, 90.0),
            set(d.minusDays(40), "squat", 1, 5, 80.0), // outside range
        )
        val r = ReportBuilder.build(DateRange.of(RangePreset.THIS_WEEK, d), sets)
        assertEquals(2, r.daysTrained)
        assertEquals(4, r.totalSets)
        assertEquals(28, r.totalReps)
        assertEquals(600 + 560 + 500 + 450.0, r.totalVolume, 0.001)
        val bench = r.byExercise.first { it.name == "Bench" }
        assertEquals(70.0, bench.bestWeight, 0.0)
        assertEquals(70 * (1 + 8 / 30.0), bench.bestE1rm, 0.001)
        assertEquals(listOf("chest" to 2, "quadriceps" to 2).toSet(), r.byMuscle.toSet())

        val csv = ReportBuilder.toCsv(r).lines()
        assertTrue(csv[0].startsWith("date,exercise,set,weight_kg,reps"))
        assertEquals(5, csv.count { it.isNotBlank() })
        val text = ReportBuilder.toText(r)
        assertTrue(text.contains("Total sets   : 4"))
    }

    @Test fun csvEscapes() {
        val r = ReportBuilder.build(DateRange(d, d), listOf(set(d, "bench", 1, 5, 50.0).copy(exerciseName = "Press, \"heavy\"")))
        assertTrue(ReportBuilder.toCsv(r).contains("\"Press, \"\"heavy\"\"\""))
    }
}
