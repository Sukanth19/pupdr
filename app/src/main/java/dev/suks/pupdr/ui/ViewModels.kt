package dev.suks.pupdr.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.suks.pupdr.PupdrApp
import dev.suks.pupdr.data.Exercise
import dev.suks.pupdr.data.SetWithExercise
import dev.suks.pupdr.data.FoodEntry
import dev.suks.pupdr.data.Meal
import dev.suks.pupdr.domain.Macros
import dev.suks.pupdr.domain.dailyMacros
import dev.suks.pupdr.domain.DailyStats
import dev.suks.pupdr.domain.DateRange
import dev.suks.pupdr.ecosystem.Spend
import kotlinx.coroutines.Dispatchers
import dev.suks.pupdr.domain.Granularity
import dev.suks.pupdr.domain.Periods
import dev.suks.pupdr.domain.Report
import dev.suks.pupdr.domain.ReportBuilder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

private fun <T> Flow<T>.state(vm: ViewModel, initial: T): StateFlow<T> =
    stateIn(vm.viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)

/** App-wide one-shot messages shown as snackbars. */
class Messages {
    private val _flow = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val flow = _flow.asSharedFlow()
    fun send(msg: String) { _flow.tryEmit(msg) }
}

data class ExerciseGroup(val exerciseId: String, val name: String, val muscles: String, val category: String, val sets: List<SetWithExercise>) {
    val done get() = sets.count { it.done }
}

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutViewModel(private val app: PupdrApp, private val messages: Messages) : ViewModel() {
    private val repo = app.repo
    val day = MutableStateFlow(LocalDate.now())
    val unit = app.prefs.unit
    val dynamicColor = app.prefs.dynamicColor

    val groups: StateFlow<List<ExerciseGroup>> = day.flatMapLatest { repo.setsForDay(it.toEpochDay()) }
        .map { sets ->
            sets.groupBy { it.exerciseId }.map { (id, list) ->
                ExerciseGroup(id, list.first().exerciseName, list.first().primaryMuscles, list.first().category, list.sortedBy { it.setIndex })
            }
        }.state(this, emptyList())

    val routines = repo.routines.state(this, emptyList())
    val trainedDays: StateFlow<Set<Long>> = repo.trainedDays.map { it.toSet() }.state(this, emptySet())

    /** Consecutive trained days ending today (or yesterday, so the streak survives until you train). */
    val streak: StateFlow<Int> = trainedDays.map { set ->
        val today = LocalDate.now().toEpochDay()
        var d = if (today in set) today else today - 1
        var n = 0
        while (d in set) { n++; d-- }
        n
    }.state(this, 0)

    // daily habits, nutrition and linked apps
    val daily: StateFlow<Map<String, Double>> = day.flatMapLatest { repo.dailyLogs(it.toEpochDay()) }.state(this, emptyMap())
    val food: StateFlow<List<FoodEntry>> = day.flatMapLatest { repo.foodFor(it.toEpochDay()) }.state(this, emptyList())
    val waterGoal = app.prefs.waterGoal
    val proteinGoal = app.prefs.proteinGoal
    val calorieGoal = app.prefs.calorieGoal
    val syncVelto = app.prefs.syncVelto
    val syncMidas = app.prefs.syncMidas
    val veltoInstalled = MutableStateFlow(false)
    val midasInstalled = MutableStateFlow(false)

    init { refreshLinks() }

    fun refreshLinks() = viewModelScope.launch(Dispatchers.IO) {
        veltoInstalled.value = app.bridge.veltoInstalled()
        midasInstalled.value = app.bridge.midasInstalled()
    }

    fun addWater(delta: Int) = viewModelScope.launch { repo.addWater(day.value.toEpochDay(), delta) }
    fun setCreatine(taken: Boolean) = viewModelScope.launch { repo.setCreatine(day.value.toEpochDay(), taken) }
    fun setWaterGoal(v: Int) = app.prefs.setWaterGoal(v)
    fun setProteinGoal(v: Int) = app.prefs.setProteinGoal(v)
    fun setCalorieGoal(v: Int) = app.prefs.setCalorieGoal(v)
    val carbGoal = app.prefs.carbGoal
    val fatGoal = app.prefs.fatGoal
    fun setCarbGoal(v: Int) = app.prefs.setCarbGoal(v)
    fun setFatGoal(v: Int) = app.prefs.setFatGoal(v)
    fun setSyncVelto(on: Boolean) = app.prefs.setSyncVelto(on)
    fun setSyncMidas(on: Boolean) = app.prefs.setSyncMidas(on)

    fun setDay(d: LocalDate) { day.value = d }
    fun setUnit(u: String) = app.prefs.setUnit(u)
    fun setDynamicColor(on: Boolean) = app.prefs.setDynamicColor(on)

    fun addExercises(ids: List<String>) = viewModelScope.launch { repo.addExercisesToDay(day.value.toEpochDay(), ids) }
    fun startRoutine(id: Long, name: String) = viewModelScope.launch {
        repo.startRoutine(day.value.toEpochDay(), id)
        messages.send("Started $name")
    }
    fun addSet(group: ExerciseGroup) = viewModelScope.launch { repo.addSet(day.value.toEpochDay(), group.exerciseId, group.sets.lastOrNull()) }
    fun updateSet(id: Long, reps: Int, weight: Double, done: Boolean) = viewModelScope.launch { repo.updateSet(id, reps, weight, done) }
    fun deleteSet(id: Long) = viewModelScope.launch { repo.deleteSet(id) }
    fun removeExercise(exerciseId: String) = viewModelScope.launch { repo.removeExerciseFromDay(day.value.toEpochDay(), exerciseId) }
}

@OptIn(ExperimentalCoroutinesApi::class)
class FoodViewModel(private val app: PupdrApp, private val messages: Messages) : ViewModel() {
    private val repo = app.repo
    val day = MutableStateFlow(LocalDate.now())
    val entries: StateFlow<List<FoodEntry>> = day.flatMapLatest { repo.foodFor(it.toEpochDay()) }.state(this, emptyList())
    /** The 7 days ending on the selected day, for the weekly chart. */
    val week: StateFlow<List<Pair<LocalDate, Macros>>> = day.flatMapLatest { d ->
        repo.foodBetween(d.minusDays(6).toEpochDay(), d.toEpochDay()).map { dailyMacros(it, d) }
    }.state(this, emptyList())
    val proteinGoal = app.prefs.proteinGoal
    val carbGoal = app.prefs.carbGoal
    val fatGoal = app.prefs.fatGoal
    val calorieGoal = app.prefs.calorieGoal
    val midasLinked = MutableStateFlow(false)

    init {
        viewModelScope.launch(Dispatchers.IO) { midasLinked.value = app.bridge.midasInstalled() && app.prefs.syncMidas.value }
    }

    fun setDay(d: LocalDate) { day.value = d }
    fun shiftDay(delta: Long) { day.value = day.value.plusDays(delta) }

    fun add(name: String, macros: Macros, meal: Meal) = viewModelScope.launch {
        repo.addFood(day.value.toEpochDay(), name, macros.protein, macros.carbs, macros.fat, macros.calories.toInt(), meal)
    }
    fun delete(id: Long) = viewModelScope.launch { repo.deleteFood(id) }

    fun addToMidas(item: String) = viewModelScope.launch {
        if (app.bridge.addToMidasBuyList(item)) messages.send("Added $item to your Midas buy list")
        else messages.send("Couldn't reach Midas")
    }
}

class ExercisesViewModel(private val app: PupdrApp, private val messages: Messages) : ViewModel() {
    private val repo = app.repo
    val all = repo.exercises.state(this, emptyList())
    val query = MutableStateFlow("")
    val muscle = MutableStateFlow<String?>(null)
    val equipment = MutableStateFlow<String?>(null)

    val muscles = all.map { list -> list.flatMap { it.primaryList }.distinct().sorted() }.state(this, emptyList())
    val equipments = all.map { list -> list.map { it.equipment }.distinct().sorted() }.state(this, emptyList())

    val filtered: StateFlow<List<Exercise>> = combine(all, query, muscle, equipment) { list, q, m, e -> filter(list, q, m, e) }
        .state(this, emptyList())

    fun createCustom(name: String, muscle: String, equipment: String, then: (Exercise) -> Unit = {}) = viewModelScope.launch {
        val ex = repo.addCustomExercise(name, muscle, equipment)
        messages.send("Added ${ex.name}")
        then(ex)
    }

    fun addToRoutine(routineId: Long, routineName: String, ids: List<String>) = viewModelScope.launch {
        repo.addToRoutine(routineId, ids)
        messages.send("Added to $routineName")
    }

    fun createRoutineWith(name: String, ids: List<String>) = viewModelScope.launch {
        repo.createRoutineWith(name, ids)
        messages.send("Created $name")
    }

    fun deleteCustom(ex: Exercise) = viewModelScope.launch {
        repo.deleteCustomExercise(ex.id)
        messages.send("Deleted ${ex.name}")
    }

    companion object {
        fun filter(list: List<Exercise>, q: String, m: String?, e: String?): List<Exercise> {
            val tokens = q.lowercase().split(' ').filter { it.isNotBlank() }
            return list.filter { ex ->
                (m == null || m in ex.primaryList) &&
                    (e == null || ex.equipment == e) &&
                    tokens.all { t -> ex.name.lowercase().contains(t) || ex.primaryMuscles.contains(t) || ex.equipment.contains(t) }
            }
        }
    }
}

class RoutinesViewModel(private val app: PupdrApp, private val messages: Messages) : ViewModel() {
    private val repo = app.repo
    val routines = repo.routines.state(this, emptyList())

    /** Creates an empty routine and hands back its id so the editor can open straight into the exercise picker. */
    fun createNew(then: (Long) -> Unit) = viewModelScope.launch { then(repo.createRoutine("")) }
    fun delete(id: Long) = viewModelScope.launch { repo.deleteRoutine(id) }

    fun export(ids: List<Long>, then: (String) -> Unit) = viewModelScope.launch { then(repo.exportRoutines(ids)) }

    fun startToday(id: Long, name: String, then: () -> Unit) = viewModelScope.launch {
        repo.startRoutine(LocalDate.now().toEpochDay(), id)
        messages.send("Started $name")
        then()
    }
}

class RoutineDetailViewModel(private val app: PupdrApp, val routineId: Long) : ViewModel() {
    private val repo = app.repo
    val routine = repo.routine(routineId).state(this, null)
    val items = repo.routineItems(routineId).state(this, emptyList())

    fun rename(name: String, notes: String) = viewModelScope.launch { repo.renameRoutine(routineId, name, notes) }
    fun add(ids: List<String>) = viewModelScope.launch { repo.addToRoutine(routineId, ids) }
    fun updateTarget(itemId: Long, sets: Int, reps: String) = viewModelScope.launch { repo.updateTarget(itemId, sets, reps) }
    fun remove(itemId: Long) = viewModelScope.launch { repo.removeRoutineItem(itemId) }
    fun move(itemId: Long, delta: Int) = viewModelScope.launch { repo.moveRoutineItem(routineId, itemId, delta) }

    /** Runs in the app scope so it completes even as this screen's ViewModel is cleared. */
    fun finish(wasNew: Boolean) = app.appScope.launch { repo.finishEditing(routineId, wasNew) }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModel(private val app: PupdrApp) : ViewModel() {
    private val repo = app.repo
    val unit = app.prefs.unit
    val granularity = MutableStateFlow(Granularity.WEEK)
    val offset = MutableStateFlow(0)
    /** When set, overrides granularity/offset. */
    val custom = MutableStateFlow<DateRange?>(null)
    private val firstDay = MutableStateFlow<LocalDate?>(null)

    init { refreshFirstDay() }

    private fun refreshFirstDay() = viewModelScope.launch { firstDay.value = repo.firstDay()?.let { LocalDate.ofEpochDay(it) } }

    val range: StateFlow<DateRange> = combine(granularity, offset, custom, firstDay) { g, o, c, f ->
        c ?: Periods.range(g, o, LocalDate.now(), f)
    }.state(this, Periods.range(Granularity.WEEK, 0, LocalDate.now()))

    val title: StateFlow<String> = combine(granularity, offset, custom, range) { g, o, c, r ->
        if (c != null) "Custom range" else Periods.title(g, o, r)
    }.state(this, "This week")

    val report: StateFlow<Report?> = combine(range, unit) { r, u -> r to u }
        .flatMapLatest { (r, u) -> repo.setsBetween(r.from.toEpochDay(), r.to.toEpochDay()).map { ReportBuilder.build(r, it, u) } }
        .state(this, null)

    val daily: StateFlow<DailyStats?> = combine(range, app.prefs.waterGoal) { r, g -> r to g }
        .flatMapLatest { (r, goal) ->
            combine(repo.dailyBetween(r.from.toEpochDay(), r.to.toEpochDay()), repo.foodBetween(r.from.toEpochDay(), r.to.toEpochDay())) { logs, food ->
                DailyStats.build(r, logs.map { Triple(it.epochDay, it.key, it.value) }, food, goal)
            }
        }.state(this, null)

    /** Gym and supplement spending from Midas for the period, when Midas is installed. */
    val fitnessSpend: StateFlow<Spend?> = range.map { r -> app.bridge.fitnessSpend(r.from.toEpochDay(), r.to.toEpochDay()) }.state(this, null)

    fun select(g: Granularity) {
        if (g == Granularity.ALL) refreshFirstDay()
        custom.value = null
        granularity.value = g
        offset.value = 0
    }
    fun shift(delta: Int) { custom.value = null; offset.value = (offset.value + delta).coerceAtMost(0) }
    fun setCustom(r: DateRange) { custom.value = r }
}
