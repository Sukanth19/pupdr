package dev.suks.pupdr.domain

import dev.suks.pupdr.data.SetWithExercise
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.roundToInt

enum class Granularity(val label: String) { DAY("Day"), WEEK("Week"), MONTH("Month"), YEAR("Year"), ALL("All") }

data class DateRange(val from: LocalDate, val to: LocalDate) {
    val days: Long get() = to.toEpochDay() - from.toEpochDay() + 1
    fun label(): String = when {
        from == to -> from.format(DAY_FMT)
        from.year == to.year && from.month == to.month -> "${from.dayOfMonth} – ${to.format(SHORT_FMT)}"
        from.year == to.year -> "${from.format(DM_FMT)} – ${to.format(SHORT_FMT)}"
        else -> "${from.format(SHORT_FMT)} – ${to.format(SHORT_FMT)}"
    }

    companion object {
        val DAY_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.getDefault())
        private val SHORT_FMT = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
        private val DM_FMT = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
    }
}

/** A reporting period: a granularity plus how many periods back from the current one. */
object Periods {
    fun range(g: Granularity, offset: Int, today: LocalDate, firstDay: LocalDate? = null): DateRange = when (g) {
        Granularity.DAY -> today.plusDays(offset.toLong()).let { DateRange(it, it) }
        Granularity.WEEK -> {
            val start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).plusWeeks(offset.toLong())
            DateRange(start, start.plusDays(6))
        }
        Granularity.MONTH -> today.plusMonths(offset.toLong()).let { DateRange(it.withDayOfMonth(1), it.with(TemporalAdjusters.lastDayOfMonth())) }
        Granularity.YEAR -> today.plusYears(offset.toLong()).let { DateRange(it.withDayOfYear(1), it.with(TemporalAdjusters.lastDayOfYear())) }
        Granularity.ALL -> DateRange(minOf(firstDay ?: today, today), today)
    }

    fun title(g: Granularity, offset: Int, range: DateRange): String = when {
        g == Granularity.ALL -> "All time"
        offset == 0 -> when (g) { Granularity.DAY -> "Today"; Granularity.WEEK -> "This week"; Granularity.MONTH -> "This month"; else -> "This year" }
        offset == -1 -> when (g) { Granularity.DAY -> "Yesterday"; Granularity.WEEK -> "Last week"; Granularity.MONTH -> "Last month"; else -> "Last year" }
        g == Granularity.MONTH -> range.from.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
        g == Granularity.YEAR -> range.from.year.toString()
        g == Granularity.DAY -> range.from.format(DateTimeFormatter.ofPattern("EEEE", Locale.getDefault()))
        else -> "${-offset} weeks ago"
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

/** Habit and nutrition numbers for a report period. */
data class DailyStats(
    val days: Long,
    val creatineDays: Int,
    val waterGoalDays: Int,
    val avgWater: Double,
    val foodDays: Int,
    val avgProtein: Double,
    val avgCalories: Double,
    val avgCarbs: Double = 0.0,
    val avgFat: Double = 0.0,
) {
    val isEmpty get() = creatineDays == 0 && waterGoalDays == 0 && avgWater == 0.0 && foodDays == 0

    companion object {
        /** [logs] are (epochDay, key, value). Food averages are over days with something logged. */
        fun build(
            range: DateRange,
            logs: List<Triple<Long, String, Double>>,
            food: List<dev.suks.pupdr.data.FoodEntry>,
            waterGoal: Int,
        ): DailyStats {
            val water = logs.filter { it.second == "water" && it.third > 0 }
            val byDay = food.groupBy { it.epochDay }
            fun avg(f: (dev.suks.pupdr.data.FoodEntry) -> Double) = if (byDay.isEmpty()) 0.0 else byDay.values.sumOf { d -> d.sumOf(f) } / byDay.size
            return DailyStats(
                days = range.days,
                creatineDays = logs.count { it.second == "creatine" && it.third >= 1 },
                waterGoalDays = water.count { it.third >= waterGoal },
                avgWater = if (water.isEmpty()) 0.0 else water.sumOf { it.third } / water.size,
                foodDays = byDay.size,
                avgProtein = avg { it.protein },
                avgCalories = avg { it.calories.toDouble() },
                avgCarbs = avg { it.carbs },
                avgFat = avg { it.fat },
            )
        }
    }
}

fun DailyStats.toText(): String = buildString {
    appendLine("HABITS & NUTRITION")
    appendLine("  Creatine     : $creatineDays of $days days")
    appendLine("  Water goal   : $waterGoalDays of $days days (avg ${ReportBuilder.fmt(avgWater)} glasses)")
    if (foodDays > 0) {
        appendLine("  Protein      : ${ReportBuilder.fmt(avgProtein)} g/day avg ($foodDays days logged)")
        appendLine("  Carbs        : ${ReportBuilder.fmt(avgCarbs)} g/day avg")
        appendLine("  Fat          : ${ReportBuilder.fmt(avgFat)} g/day avg")
        appendLine("  Calories     : ${ReportBuilder.fmt(avgCalories)} kcal/day avg")
    }
}
