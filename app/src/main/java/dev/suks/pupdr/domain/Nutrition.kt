package dev.suks.pupdr.domain

import dev.suks.pupdr.data.FoodEntry
import java.time.LocalDate

/** Grams of each macro plus calories. */
data class Macros(val protein: Double = 0.0, val carbs: Double = 0.0, val fat: Double = 0.0, val calories: Double = 0.0) {
    operator fun plus(o: Macros) = Macros(protein + o.protein, carbs + o.carbs, fat + o.fat, calories + o.calories)
    operator fun times(k: Double) = Macros(protein * k, carbs * k, fat * k, calories * k)

    /** Share of calories from protein / carbs / fat (sums to 1 when anything was eaten). */
    fun calorieSplit(): Triple<Float, Float, Float> {
        val p = protein * 4; val c = carbs * 4; val f = fat * 9
        val t = p + c + f
        if (t <= 0) return Triple(0f, 0f, 0f)
        return Triple((p / t).toFloat(), (c / t).toFloat(), (f / t).toFloat())
    }

    companion object {
        fun kcal(protein: Double, carbs: Double, fat: Double) = protein * 4 + carbs * 4 + fat * 9
        fun of(e: FoodEntry) = Macros(e.protein, e.carbs, e.fat, e.calories.toDouble())
        fun sum(entries: List<FoodEntry>) = entries.fold(Macros()) { acc, e -> acc + of(e) }
    }
}

/** A food you can add in one tap: per serving. */
data class FoodPreset(val name: String, val serving: String, val macros: Macros)

object FoodPresets {
    private fun f(name: String, serving: String, p: Double, c: Double, fat: Double, kcal: Int) = FoodPreset(name, serving, Macros(p, c, fat, kcal.toDouble()))

    val ALL = listOf(
        f("Whey protein shake", "1 scoop + water", 24.0, 3.0, 1.5, 120),
        f("Eggs", "2 large", 12.6, 1.1, 10.0, 143),
        f("Egg whites", "4", 14.4, 1.0, 0.2, 68),
        f("Chicken breast", "100 g cooked", 31.0, 0.0, 3.6, 165),
        f("Paneer", "100 g", 18.0, 3.4, 20.8, 265),
        f("Tofu", "100 g", 8.0, 1.9, 4.8, 76),
        f("Greek yogurt", "170 g", 17.0, 6.0, 0.7, 100),
        f("Curd", "1 cup", 8.5, 11.0, 8.0, 150),
        f("Milk", "1 glass, 250 ml", 8.0, 12.0, 8.0, 150),
        f("Dal", "1 bowl", 9.0, 27.0, 3.5, 180),
        f("Rajma / chole", "1 bowl", 12.0, 38.0, 6.0, 250),
        f("Rice", "1 cup cooked", 4.3, 45.0, 0.4, 205),
        f("Roti / chapati", "1", 3.1, 15.0, 3.7, 105),
        f("Oats", "50 g", 6.5, 33.0, 3.5, 190),
        f("Peanut butter", "2 tbsp", 7.0, 6.0, 16.0, 190),
        f("Almonds", "28 g", 6.0, 6.0, 14.0, 164),
        f("Banana", "1 medium", 1.3, 27.0, 0.4, 105),
        f("Apple", "1 medium", 0.5, 25.0, 0.3, 95),
        f("Bread", "2 slices", 5.0, 26.0, 2.0, 150),
        f("Fish", "100 g cooked", 22.0, 0.0, 5.0, 140),
        f("Mutton curry", "1 bowl", 22.0, 6.0, 18.0, 280),
        f("Idli", "2", 4.0, 30.0, 0.4, 140),
        f("Dosa", "1 plain", 4.0, 29.0, 6.0, 170),
        f("Poha", "1 plate", 4.5, 45.0, 7.0, 250),
        f("Sweet potato", "150 g", 2.4, 30.0, 0.2, 130),
    )
}

/** Per-day totals for a run of days ending on [end], for the weekly chart. */
fun dailyMacros(entries: List<FoodEntry>, end: LocalDate, days: Int = 7): List<Pair<LocalDate, Macros>> {
    val byDay = entries.groupBy { it.epochDay }
    return (days - 1 downTo 0).map { back ->
        val d = end.minusDays(back.toLong())
        d to Macros.sum(byDay[d.toEpochDay()].orEmpty())
    }
}
