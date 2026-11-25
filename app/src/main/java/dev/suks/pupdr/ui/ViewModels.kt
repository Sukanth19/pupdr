package dev.suks.pupdr.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.suks.pupdr.PupdrApp
import dev.suks.pupdr.data.Exercise
import dev.suks.pupdr.data.SetWithExercise
import dev.suks.pupdr.domain.DateRange
import dev.suks.pupdr.domain.RangePreset
import dev.suks.pupdr.domain.Report
import dev.suks.pupdr.domain.ReportBuilder
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

private fun <T> kotlinx.coroutines.flow.Flow<T>.state(vm: ViewModel, initial: T): StateFlow<T> =
    stateIn(vm.viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)

/** App-wide one-shot messages shown as snackbars. */
class Messages {
    private val _flow = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val flow = _flow.asSharedFlow()
    fun send(msg: String) { _flow.tryEmit(msg) }
}

data class ExerciseGroup(val exerciseId: String, val name: String, val muscles: String, val sets: List<SetWithExercise>)

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutViewModel(private val app: PupdrApp, private val messages: Messages) : ViewModel() {
    private val repo = app.repo
    val day = MutableStateFlow(LocalDate.now())
    val unit = app.prefs.unit

    val groups: StateFlow<List<ExerciseGroup>> = day.flatMapLatest { repo.setsForDay(it.toEpochDay()) }
        .map { sets ->
            sets.groupBy { it.exerciseId }.map { (id, list) ->
                ExerciseGroup(id, list.first().exerciseName, list.first().primaryMuscles, list.sortedBy { it.setIndex })
            }
        }.state(this, emptyList())

    val routines = repo.routines.state(this, emptyList())

    /** Current streak (consecutive days up to today/yesterday) and sessions in the last 7 days. */
    val streak: StateFlow<Pair<Int, Int>> = repo.trainedDays.map { days ->
        val today = LocalDate.now().toEpochDay()
        val set = days.toHashSet()
        var d = if (today in set) today else today - 1
        var streak = 0
        while (d in set) { streak++; d-- }
        streak to days.count { it > today - 7 }
    }.state(this, 0 to 0)

    fun shiftDay(delta: Long) { day.value = day.value.plusDays(delta) }
    fun setDay(d: LocalDate) { day.value = d }

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

class ExercisesViewModel(private val app: PupdrApp, private val messages: Messages) : ViewModel() {
    private val repo = app.repo
    val all = repo.exercises.state(this, emptyList())
    val query = MutableStateFlow("")
    val muscle = MutableStateFlow<String?>(null)
    val equipment = MutableStateFlow<String?>(null)

    val muscles = all.map { list -> list.flatMap { it.primaryList }.distinct().sorted() }.state(this, emptyList())
    val equipments = all.map { list -> list.map { it.equipment }.distinct().sorted() }.state(this, emptyList())

    val filtered: StateFlow<List<Exercise>> = combine(all, query, muscle, equipment) { list, q, m, e ->
        filter(list, q, m, e)
    }.state(this, emptyList())

    fun createCustom(name: String, muscle: String, equipment: String, then: (Exercise) -> Unit = {}) = viewModelScope.launch {
        val ex = repo.addCustomExercise(name, muscle, equipment)
        messages.send("Added ${ex.name}")
        then(ex)
    }

    fun addToRoutine(routineId: Long, routineName: String, ids: List<String>) = viewModelScope.launch {
        repo.addToRoutine(routineId, ids)
        messages.send("Added to $routineName")
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

    fun create(name: String, then: (Long) -> Unit) = viewModelScope.launch { then(repo.createRoutine(name)) }
    fun delete(id: Long) = viewModelScope.launch { repo.deleteRoutine(id) }

    fun import(text: String) = viewModelScope.launch {
        runCatching { repo.importRoutines(text) }
            .onSuccess { r ->
                val extra = if (r.created.isNotEmpty()) " (${r.created.size} new custom exercise${if (r.created.size > 1) "s" else ""})" else ""
                messages.send("Imported ${r.routines} routine${if (r.routines > 1) "s" else ""}, ${r.exercises} exercises$extra")
            }
            .onFailure { messages.send("Import failed: ${it.message ?: "unreadable file"}") }
    }

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
}

@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModel(private val app: PupdrApp) : ViewModel() {
    private val repo = app.repo
    val unit = app.prefs.unit
    val preset = MutableStateFlow(RangePreset.THIS_WEEK)
    val custom = MutableStateFlow(DateRange(LocalDate.now().minusDays(13), LocalDate.now()))
    private val firstDay = MutableStateFlow<LocalDate?>(null)

    init {
        viewModelScope.launch { firstDay.value = repo.firstDay()?.let { LocalDate.ofEpochDay(it) } }
    }

    val range: StateFlow<DateRange> = combine(preset, custom, firstDay) { p, c, f ->
        if (p == RangePreset.CUSTOM) c else DateRange.of(p, LocalDate.now(), f)
    }.state(this, DateRange.of(RangePreset.THIS_WEEK, LocalDate.now()))

    val report: StateFlow<Report?> = combine(range, unit) { r, u -> r to u }
        .flatMapLatest { (r, u) -> repo.setsBetween(r.from.toEpochDay(), r.to.toEpochDay()).map { ReportBuilder.build(r, it, u) } }
        .state(this, null)

    fun select(p: RangePreset) {
        if (p == RangePreset.ALL) viewModelScope.launch { firstDay.value = repo.firstDay()?.let { LocalDate.ofEpochDay(it) } }
        preset.value = p
    }
    fun setCustom(r: DateRange) { custom.value = r; preset.value = RangePreset.CUSTOM }
    fun toggleUnit() = app.prefs.toggleUnit()
}
