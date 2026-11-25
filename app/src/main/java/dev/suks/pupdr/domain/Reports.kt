package dev.suks.pupdr.domain

import dev.suks.pupdr.data.SetWithExercise
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.roundToInt

enum class RangePreset(val label: String) {
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    THIS_WEEK("This week"),
    LAST_WEEK("Last week"),
    LAST_7("Last 7 days"),
    THIS_MONTH("This month"),
    LAST_MONTH("Last month"),
    LAST_30("Last 30 days"),
    THIS_YEAR("This year"),
    ALL("All time"),
    CUSTOM("Custom"),
}

data class DateRange(val from: LocalDate, val to: LocalDate) {
    val days: Long get() = to.toEpochDay() - from.toEpochDay() + 1
    fun label(): String = if (from == to) from.format(DAY_FMT) else "${from.format(DAY_FMT)} – ${to.format(DAY_FMT)}"

    companion object {
        val DAY_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.getDefault())

        fun of(preset: RangePreset, today: LocalDate, firstDay: LocalDate? = null): DateRange = when (preset) {
            RangePreset.TODAY -> DateRange(today, today)
            RangePreset.YESTERDAY -> today.minusDays(1).let { DateRange(it, it) }
            RangePreset.THIS_WEEK -> DateRange(today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)), today)
            RangePreset.LAST_WEEK -> today.minusWeeks(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .let { DateRange(it, it.plusDays(6)) }
            RangePreset.LAST_7 -> DateRange(today.minusDays(6), today)
            RangePreset.THIS_MONTH -> DateRange(today.withDayOfMonth(1), today)
            RangePreset.LAST_MONTH -> today.minusMonths(1).let { DateRange(it.withDayOfMonth(1), it.with(TemporalAdjusters.lastDayOfMonth())) }
            RangePreset.LAST_30 -> DateRange(today.minusDays(29), today)
            RangePreset.THIS_YEAR -> DateRange(today.withDayOfYear(1), today)
            RangePreset.ALL, RangePreset.CUSTOM -> DateRange(minOf(firstDay ?: today, today), today)
        }
    }
}

data class ExerciseStat(
    val name: String,
    val sets: Int,
    val reps: Int,
    val volume: Double,
    val bestWeight: Double,
    val bestE1rm: Double,
)

data class DayStat(val date: LocalDate, val exercises: Int, val sets: Int, val volume: Double)

data class Report(
    val range: DateRange,
    val unit: String,
    val daysTrained: Int,
    val totalSets: Int,
    val totalReps: Int,
    val totalVolume: Double,
    val byExercise: List<ExerciseStat>,
    val byMuscle: List<Pair<String, Int>>,
    val byDay: List<DayStat>,
    val rows: List<SetWithExercise>,
)

object ReportBuilder {
    /** A set counts if it was ticked done, or has reps entered. */
    fun performed(s: SetWithExercise) = s.done || s.reps > 0

    /** Epley estimated one-rep max. */
    fun e1rm(weight: Double, reps: Int): Double = if (reps <= 0 || weight <= 0) 0.0 else if (reps == 1) weight else weight * (1 + reps / 30.0)

    fun build(range: DateRange, all: List<SetWithExercise>, unit: String = "kg"): Report {
        val sets = all.filter { performed(it) && it.epochDay in range.from.toEpochDay()..range.to.toEpochDay() }
        val byExercise = sets.groupBy { it.exerciseId }.map { (_, list) ->
            ExerciseStat(
                name = list.first().exerciseName,
                sets = list.size,
                reps = list.sumOf { it.reps },
                volume = list.sumOf { it.reps * it.weight },
                bestWeight = list.maxOf { it.weight },
                bestE1rm = list.maxOf { e1rm(it.weight, it.reps) },
            )
        }.sortedByDescending { it.sets }
        val byMuscle = sets.flatMap { s -> s.primaryMuscles.split(',').filter { it.isNotBlank() }.ifEmpty { listOf("other") } }
            .groupingBy { it }.eachCount().toList().sortedByDescending { it.second }
        val byDay = sets.groupBy { it.epochDay }.toSortedMap().map { (day, list) ->
            DayStat(LocalDate.ofEpochDay(day), list.map { it.exerciseId }.distinct().size, list.size, list.sumOf { it.reps * it.weight })
        }
        return Report(
            range = range,
            unit = unit,
            daysTrained = byDay.size,
            totalSets = sets.size,
            totalReps = sets.sumOf { it.reps },
            totalVolume = sets.sumOf { it.reps * it.weight },
            byExercise = byExercise,
            byMuscle = byMuscle,
            byDay = byDay,
            rows = sets,
        )
    }

    fun fmt(d: Double): String = if (d == d.roundToInt().toDouble()) d.roundToInt().toString() else String.format(Locale.US, "%.1f", d)

    private fun csvCell(s: String) = if (s.any { it == ',' || it == '"' || it == '\n' }) "\"" + s.replace("\"", "\"\"") + "\"" else s

    fun toCsv(r: Report): String = buildString {
        appendLine("date,exercise,set,weight_${r.unit},reps,volume_${r.unit},est_1rm_${r.unit},muscles,category")
        for (s in r.rows) {
            appendLine(
                listOf(
                    LocalDate.ofEpochDay(s.epochDay).toString(),
                    csvCell(s.exerciseName),
                    s.setIndex.toString(),
                    fmt(s.weight),
                    s.reps.toString(),
                    fmt(s.weight * s.reps),
                    fmt(e1rm(s.weight, s.reps)),
                    csvCell(s.primaryMuscles.replace(",", "; ")),
                    csvCell(s.category),
                ).joinToString(",")
            )
        }
    }

    fun toText(r: Report): String = buildString {
        appendLine("PUPDR WORKOUT REPORT")
        appendLine(r.range.label())
        appendLine("=".repeat(40))
        appendLine("Days trained : ${r.daysTrained} of ${r.range.days}")
        appendLine("Total sets   : ${r.totalSets}")
        appendLine("Total reps   : ${r.totalReps}")
        appendLine("Total volume : ${fmt(r.totalVolume)} ${r.unit}")
        if (r.byMuscle.isNotEmpty()) {
            appendLine()
            appendLine("SETS PER MUSCLE")
            r.byMuscle.forEach { (m, c) -> appendLine("  ${m.replaceFirstChar { it.uppercase() }.padEnd(18)} $c") }
        }
        if (r.byExercise.isNotEmpty()) {
            appendLine()
            appendLine("EXERCISES")
            r.byExercise.forEach {
                appendLine("  ${it.name}")
                appendLine("    ${it.sets} sets · ${it.reps} reps · ${fmt(it.volume)} ${r.unit} · best ${fmt(it.bestWeight)} ${r.unit} · e1RM ${fmt(it.bestE1rm)} ${r.unit}")
            }
        }
        if (r.rows.isNotEmpty()) {
            appendLine()
            appendLine("LOG")
            r.rows.groupBy { it.epochDay }.toSortedMap().forEach { (day, list) ->
                appendLine("  ${LocalDate.ofEpochDay(day).format(DateRange.DAY_FMT)}")
                list.groupBy { it.exerciseId }.values.forEach { ex ->
                    val sets = ex.joinToString("  ") { "${fmt(it.weight)}×${it.reps}" }
                    appendLine("    ${ex.first().exerciseName}: $sets")
                }
            }
        } else {
            appendLine()
            appendLine("No workouts logged in this period.")
        }
    }
}
