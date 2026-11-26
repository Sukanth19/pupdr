package dev.suks.pupdr

import dev.suks.pupdr.data.SetWithExercise
import dev.suks.pupdr.domain.DateRange
import dev.suks.pupdr.domain.Granularity
import dev.suks.pupdr.domain.Periods
import dev.suks.pupdr.domain.ReportBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ReportBuilderTest {
    private val d = LocalDate.of(2026, 11, 26) // a thursday
    private fun set(day: LocalDate, ex: String, idx: Int, reps: Int, w: Double, done: Boolean = true) =
        SetWithExercise(0, day.toEpochDay(), ex, idx, reps, w, done, 0, ex.replaceFirstChar { it.uppercase() }, if (ex == "bench") "chest" else "quadriceps", "strength")

    @Test fun periods() {
        assertEquals(DateRange(d, d), Periods.range(Granularity.DAY, 0, d))
        assertEquals(DateRange(LocalDate.of(2026, 11, 23), LocalDate.of(2026, 11, 29)), Periods.range(Granularity.WEEK, 0, d))
        assertEquals(DateRange(LocalDate.of(2026, 11, 16), LocalDate.of(2026, 11, 22)), Periods.range(Granularity.WEEK, -1, d))
        assertEquals(DateRange(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)), Periods.range(Granularity.MONTH, -1, d))
        assertEquals(DateRange(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31)), Periods.range(Granularity.YEAR, -1, d))
        assertEquals("Last week", Periods.title(Granularity.WEEK, -1, Periods.range(Granularity.WEEK, -1, d)))
        assertEquals("September 2026", Periods.title(Granularity.MONTH, -2, Periods.range(Granularity.MONTH, -2, d)))
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
        val r = ReportBuilder.build(Periods.range(Granularity.WEEK, 0, d), sets)
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

    @Test fun dailyStats() {
        val r = DateRange(d.minusDays(6), d)
        val logs = listOf(
            Triple(d.toEpochDay(), "creatine", 1.0), Triple(d.minusDays(1).toEpochDay(), "creatine", 1.0),
            Triple(d.toEpochDay(), "water", 8.0), Triple(d.minusDays(2).toEpochDay(), "water", 4.0),
        )
        fun fe(day: LocalDate, p: Double, c: Double, f: Double, k: Int) = dev.suks.pupdr.data.FoodEntry(epochDay = day.toEpochDay(), name = "x", protein = p, carbs = c, fat = f, calories = k)
        val food = listOf(fe(d, 100.0, 150.0, 40.0, 1500), fe(d, 40.0, 50.0, 20.0, 500), fe(d.minusDays(1), 120.0, 300.0, 60.0, 2000))
        val s = dev.suks.pupdr.domain.DailyStats.build(r, logs, food, waterGoal = 8)
        assertEquals(2, s.creatineDays)
        assertEquals(1, s.waterGoalDays)
        assertEquals(6.0, s.avgWater, 0.0)
        assertEquals(2, s.foodDays)
        assertEquals(130.0, s.avgProtein, 0.001)
        assertEquals(2000.0, s.avgCalories, 0.001)
        assertEquals(250.0, s.avgCarbs, 0.001)
        assertEquals(60.0, s.avgFat, 0.001)
    }

    @Test fun macros() {
        val m = dev.suks.pupdr.domain.Macros(protein = 25.0, carbs = 50.0, fat = 10.0)
        assertEquals(390.0, dev.suks.pupdr.domain.Macros.kcal(25.0, 50.0, 10.0), 0.0)
        val (p, c, f) = m.calorieSplit()
        assertEquals(100f / 390f, p, 0.0001f); assertEquals(200f / 390f, c, 0.0001f); assertEquals(90f / 390f, f, 0.0001f)
        val week = dev.suks.pupdr.domain.dailyMacros(
            listOf(dev.suks.pupdr.data.FoodEntry(epochDay = d.toEpochDay(), name = "x", protein = 10.0, calories = 100)), d,
        )
        assertEquals(7, week.size)
        assertEquals(d, week.last().first)
        assertEquals(10.0, week.last().second.protein, 0.0)
        assertEquals(0.0, week.first().second.protein, 0.0)
    }
}
