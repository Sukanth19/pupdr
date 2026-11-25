package dev.suks.pupdr.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.suks.pupdr.PupdrApp
import dev.suks.pupdr.data.Exercise
import dev.suks.pupdr.data.SetWithExercise
import dev.suks.pupdr.domain.DateRange
import dev.suks.pupdr.domain.DraftItem
import dev.suks.pupdr.domain.DraftRoutine
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
import kotlinx.coroutines.flow.update
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

data class ExerciseGroup(val exerciseId: String, val name: String, val muscles: String, val sets: List<SetWithExercise>) {
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
                ExerciseGroup(id, list.first().exerciseName, list.first().primaryMuscles, list.sortedBy { it.setIndex })
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

    /** Parsed import waiting for the user to review; null when no review is open. */
    val draft = MutableStateFlow<List<DraftRoutine>?>(null)
    val parsing = MutableStateFlow(false)

    fun create(name: String, then: (Long) -> Unit) = viewModelScope.launch { then(repo.createRoutine(name)) }
    fun delete(id: Long) = viewModelScope.launch { repo.deleteRoutine(id) }

    /** Parses text and opens the review sheet. Returns false via [onEmpty] if nothing usable was found. */
    fun preview(text: String, onEmpty: () -> Unit = {}) = viewModelScope.launch {
        parsing.value = true
        val result = runCatching { repo.draftImport(text) }.getOrDefault(emptyList())
        parsing.value = false
        if (result.isEmpty()) {
            messages.send("Couldn't find any exercises in that. Try one exercise per line, like \"Bench press 4x8\".")
            onEmpty()
        } else draft.value = result
    }

    fun editDraft(f: (List<DraftRoutine>) -> List<DraftRoutine>) = draft.update { it?.let(f) }

    fun updateItem(routineKey: Long, item: DraftItem) = editDraft { list ->
        list.map { r -> if (r.key != routineKey) r else r.copy(items = r.items.map { if (it.key == item.key) item else it }) }
    }

    fun removeItem(routineKey: Long, itemKey: Long) = editDraft { list ->
        list.map { r -> if (r.key != routineKey) r else r.copy(items = r.items.filterNot { it.key == itemKey }) }
    }

    fun renameDraft(routineKey: Long, name: String) = editDraft { list -> list.map { if (it.key == routineKey) it.copy(name = name) else it } }
    fun removeDraftRoutine(routineKey: Long) = editDraft { list -> list.filterNot { it.key == routineKey } }

    fun commit() = viewModelScope.launch {
        val d = draft.value ?: return@launch
        runCatching { repo.commitImport(d) }
            .onSuccess { r ->
                draft.value = null
                val extra = if (r.created.isNotEmpty()) " · ${r.created.size} new custom" else ""
                messages.send("Imported ${r.routines} routine${if (r.routines != 1) "s" else ""} · ${r.exercises} exercises$extra")
            }
            .onFailure { messages.send("Import failed: ${it.message}") }
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

    fun select(g: Granularity) {
        if (g == Granularity.ALL) refreshFirstDay()
        custom.value = null
        granularity.value = g
        offset.value = 0
    }
    fun shift(delta: Int) { custom.value = null; offset.value = (offset.value + delta).coerceAtMost(0) }
    fun setCustom(r: DateRange) { custom.value = r }
}
