package dev.suks.pupdr.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.suks.pupdr.data.SetWithExercise
import dev.suks.pupdr.domain.ReportBuilder
import dev.suks.pupdr.ui.ExerciseGroup
import dev.suks.pupdr.ui.ExercisesViewModel
import dev.suks.pupdr.ui.WorkoutViewModel
import dev.suks.pupdr.ui.components.EmptyState
import dev.suks.pupdr.ui.components.ExercisePicker
import dev.suks.pupdr.ui.components.PupLogo
import dev.suks.pupdr.ui.components.StatTile
import dev.suks.pupdr.ui.components.musclesLabel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(vm: WorkoutViewModel, exVm: ExercisesViewModel) {
    val day by vm.day.collectAsState()
    val groups by vm.groups.collectAsState()
    val routines by vm.routines.collectAsState()
    val streak by vm.streak.collectAsState()
    val unit by vm.unit.collectAsState()
    var picking by remember { mutableStateOf(false) }
    var choosingRoutine by remember { mutableStateOf(false) }
    var pickingDate by remember { mutableStateOf(false) }

    val allSets = groups.flatMap { it.sets }
    val doneSets = allSets.filter { ReportBuilder.performed(it) }
    val volume = doneSets.sumOf { it.reps * it.weight }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PupLogo(34.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("pupdr", style = MaterialTheme.typography.headlineSmall)
                    }
                },
                actions = {
                    IconButton(onClick = { choosingRoutine = true }) { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, "Start routine") }
                    IconButton(onClick = { pickingDate = true }) { Icon(Icons.Default.CalendarMonth, "Pick date") }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { picking = true },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Add exercise") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
    ) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { DayNavigator(day, onShift = vm::shiftDay, onToday = { vm.setDay(LocalDate.now()) }) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("day streak", "🔥 ${streak.first}", Modifier.weight(1f), highlight = true)
                    StatTile("sessions / 7d", "${streak.second}", Modifier.weight(1f))
                    StatTile("volume $unit", ReportBuilder.fmt(volume), Modifier.weight(1f))
                }
            }
            if (groups.isEmpty()) {
                item {
                    EmptyState(
                        title = "Nothing logged ${if (day == LocalDate.now()) "today" else "this day"}",
                        body = "Start one of your routines or add exercises from the library of 870+ moves.",
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            FilledTonalButton(onClick = { choosingRoutine = true }) { Text("Start routine") }
                            OutlinedButton(onClick = { picking = true }) { Text("Add exercise") }
                        }
                    }
                }
            }
            items(groups, key = { it.exerciseId }) { g ->
                ExerciseCard(
                    group = g,
                    unit = unit,
                    onAddSet = { vm.addSet(g) },
                    onUpdate = vm::updateSet,
                    onDeleteSet = vm::deleteSet,
                    onRemove = { vm.removeExercise(g.exerciseId) },
                )
            }
        }
    }

    if (picking) {
        ExercisePicker(exVm, "Add to workout", onDismiss = { picking = false }) { ids ->
            picking = false
            vm.addExercises(ids)
        }
    }
    if (choosingRoutine) {
        AlertDialog(
            onDismissRequest = { choosingRoutine = false },
            title = { Text("Start a routine") },
            text = {
                if (routines.isEmpty()) Text("No routines yet. Create or import one in the Routines tab.")
                else Column {
                    routines.forEach { r ->
                        ListItem(
                            headlineContent = { Text(r.name) },
                            supportingContent = { Text("${r.exerciseCount} exercises") },
                            modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable {
                                choosingRoutine = false
                                vm.startRoutine(r.id, r.name)
                            },
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { choosingRoutine = false }) { Text("Close") } },
        )
    }
    if (pickingDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = day.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { vm.setDay(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                    pickingDate = false
                }) { Text("Go") }
            },
            dismissButton = { TextButton(onClick = { pickingDate = false }) { Text("Cancel") } },
        ) { DatePicker(state) }
    }
}

@Composable
private fun DayNavigator(day: LocalDate, onShift: (Long) -> Unit, onToday: () -> Unit) {
    val today = LocalDate.now()
    val label = when (day) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        today.plusDays(1) -> "Tomorrow"
        else -> day.format(DateTimeFormatter.ofPattern("EEEE"))
    }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceContainer).padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { onShift(-1) }) { Icon(Icons.Default.ChevronLeft, "Previous day") }
        Column(Modifier.weight(1f).clickable(onClick = onToday), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            Text(day.format(DateTimeFormatter.ofPattern("d MMMM yyyy")), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = { onShift(1) }) { Icon(Icons.Default.ChevronRight, "Next day") }
    }
}

@Composable
private fun ExerciseCard(
    group: ExerciseGroup,
    unit: String,
    onAddSet: () -> Unit,
    onUpdate: (Long, Int, Double, Boolean) -> Unit,
    onDeleteSet: (Long) -> Unit,
    onRemove: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(group.name, style = MaterialTheme.typography.titleMedium)
                    Text(musclesLabel(group.muscles), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "More") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Remove from day") }, onClick = { menu = false; onRemove() })
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Header("SET", Modifier.width(36.dp))
                Header(unit.uppercase(), Modifier.weight(1f))
                Header("REPS", Modifier.weight(1f))
                Spacer(Modifier.width(84.dp))
            }
            group.sets.forEach { s -> SetRow(s, onUpdate, onDeleteSet) }
            TextButton(onClick = onAddSet) {
                Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add set")
            }
        }
    }
}

@Composable
private fun Header(text: String, modifier: Modifier) {
    Text(text, modifier, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
}

@Composable
private fun SetRow(s: SetWithExercise, onUpdate: (Long, Int, Double, Boolean) -> Unit, onDelete: (Long) -> Unit) {
    var weight by remember(s.id) { mutableStateOf(if (s.weight == 0.0) "" else ReportBuilder.fmt(s.weight)) }
    var reps by remember(s.id) { mutableStateOf(if (s.reps == 0) "" else s.reps.toString()) }
    var done by remember(s.id) { mutableStateOf(s.done) }
    fun push() = onUpdate(s.id, reps.toIntOrNull() ?: 0, weight.replace(',', '.').toDoubleOrNull() ?: 0.0, done)
    val rowColor by animateColorAsState(if (done) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surfaceContainer, label = "row")

    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(12.dp)).background(rowColor).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("${s.setIndex}", Modifier.width(36.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
        NumberCell(weight, { weight = it; push() }, KeyboardType.Decimal, Modifier.weight(1f))
        NumberCell(reps, { reps = it.filter(Char::isDigit); push() }, KeyboardType.Number, Modifier.weight(1f))
        Box(
            Modifier.padding(horizontal = 6.dp).size(36.dp).clip(CircleShape)
                .background(if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest)
                .clickable { done = !done; push() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Check, "Done", tint = if (done) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
        IconButton(onClick = { onDelete(s.id) }, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Close, "Delete set", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun NumberCell(value: String, onChange: (String) -> Unit, type: KeyboardType, modifier: Modifier) {
    BasicTextField(
        value = value,
        onValueChange = { v -> if (v.length <= 7) onChange(v.filter { it.isDigit() || it == '.' || it == ',' }) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = type),
        textStyle = TextStyle(fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface, fontSize = MaterialTheme.typography.titleMedium.fontSize),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = modifier.padding(horizontal = 4.dp),
        decorationBox = { inner ->
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest).padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (value.isEmpty()) Text("–", color = MaterialTheme.colorScheme.outline)
                inner()
            }
        },
    )
}
