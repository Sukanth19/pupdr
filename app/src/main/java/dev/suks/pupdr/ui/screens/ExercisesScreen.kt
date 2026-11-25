package dev.suks.pupdr.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.suks.pupdr.data.Exercise
import dev.suks.pupdr.data.RoutineSummary
import dev.suks.pupdr.ui.ExercisesViewModel
import dev.suks.pupdr.ui.components.CustomExerciseDialog
import dev.suks.pupdr.ui.components.EmptyState
import dev.suks.pupdr.ui.components.ExerciseAvatar
import dev.suks.pupdr.ui.components.SearchField
import dev.suks.pupdr.ui.components.musclesLabel
import dev.suks.pupdr.ui.components.titleCase
import dev.suks.pupdr.ui.theme.Motion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisesScreen(vm: ExercisesViewModel, routines: List<RoutineSummary>, onLogToday: (String) -> Unit) {
    val list by vm.filtered.collectAsState()
    val all by vm.all.collectAsState()
    val query by vm.query.collectAsState()
    val muscle by vm.muscle.collectAsState()
    val equipment by vm.equipment.collectAsState()
    val muscles by vm.muscles.collectAsState()
    val equipments by vm.equipments.collectAsState()
    var detail by remember { mutableStateOf<Exercise?>(null) }
    var creating by remember { mutableStateOf(false) }
    var filterSheet by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val atTop by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            Column(Modifier.background(MaterialTheme.colorScheme.surface)) {
                TopAppBar(title = { Text("Exercises") })
                SearchField(query, { vm.query.value = it }, "Search ${all.size} exercises", Modifier.padding(horizontal = 16.dp))
                Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DropdownChip("Muscle", muscle?.titleCase(), onClick = { filterSheet = "muscle" }, onClear = { vm.muscle.value = null })
                    DropdownChip("Equipment", equipment?.titleCase(), onClick = { filterSheet = "equipment" }, onClear = { vm.equipment.value = null })
                }
            }
        },
        floatingActionButton = {
            AnimatedVisibility(atTop, enter = scaleIn(Motion.enter()) + fadeIn(Motion.enter()), exit = scaleOut(Motion.exit()) + fadeOut(Motion.exit())) {
                FloatingActionButton(onClick = { creating = true }) { Icon(Icons.Default.Add, "New exercise") }
            }
        },
    ) { pad ->
        LazyColumn(state = listState, modifier = Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
            item(key = "count") {
                Text(
                    "${list.size} exercises",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            if (list.isEmpty() && all.isNotEmpty()) {
                item(key = "none") {
                    EmptyState(Icons.Default.SearchOff, "No matches", "Try a different search, or add it as your own exercise.", Modifier.animateItem()) {
                        Button(onClick = { creating = true }) { Text("Create \"${query.ifBlank { "exercise" }}\"") }
                    }
                }
            }
            items(list, key = { it.id }) { ex ->
                ListItem(
                    headlineContent = { Text(ex.name) },
                    supportingContent = { Text(listOf(musclesLabel(ex.primaryMuscles), ex.equipment.titleCase()).filter { it.isNotBlank() }.joinToString(" · ")) },
                    leadingContent = { ExerciseAvatar(ex.name) },
                    modifier = Modifier.animateItem(fadeInSpec = Motion.enter(), placementSpec = Motion.offsetSpring, fadeOutSpec = Motion.exit())
                        .clickable { detail = ex },
                )
            }
        }
    }

    filterSheet?.let { which ->
        val options = if (which == "muscle") muscles else equipments
        val current = if (which == "muscle") muscle else equipment
        FilterSheet(
            title = if (which == "muscle") "Muscle" else "Equipment",
            options = options,
            selected = current,
            onDismiss = { filterSheet = null },
        ) {
            if (which == "muscle") vm.muscle.value = it else vm.equipment.value = it
            filterSheet = null
        }
    }
    detail?.let { ex -> ExerciseDetailSheet(ex, routines, vm, onDismiss = { detail = null }, onLogToday = { onLogToday(ex.id); detail = null }) }
    if (creating) {
        CustomExerciseDialog(initialName = query, onDismiss = { creating = false }) { n, m, e -> creating = false; vm.createCustom(n, m, e) }
    }
}

@Composable
private fun DropdownChip(label: String, value: String?, onClick: () -> Unit, onClear: () -> Unit) {
    if (value == null) {
        FilterChip(
            selected = false,
            onClick = onClick,
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, Modifier.size(FilterChipDefaults.IconSize)) },
        )
    } else {
        InputChip(
            selected = true,
            onClick = onClick,
            label = { Text(value) },
            trailingIcon = {
                Icon(Icons.Default.Close, "Clear $label", Modifier.size(FilterChipDefaults.IconSize).clip(CircleShape).clickable(onClick = onClear))
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FilterSheet(title: String, options: List<String>, selected: String?, onDismiss: () -> Unit, onSelect: (String?) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                FilterChip(selected = selected == null, onClick = { onSelect(null) }, label = { Text("Any") },
                    leadingIcon = if (selected == null) { { Icon(Icons.Default.Check, null, Modifier.size(FilterChipDefaults.IconSize)) } } else null)
                options.forEach { o ->
                    FilterChip(
                        selected = selected == o,
                        onClick = { onSelect(o) },
                        label = { Text(o.titleCase()) },
                        leadingIcon = if (selected == o) { { Icon(Icons.Default.Check, null, Modifier.size(FilterChipDefaults.IconSize)) } } else null,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ExerciseDetailSheet(ex: Exercise, routines: List<RoutineSummary>, vm: ExercisesViewModel, onDismiss: () -> Unit, onLogToday: () -> Unit) {
    var choosing by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp).verticalScroll(rememberScrollState())) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ExerciseAvatar(ex.name, size = 48.dp)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(ex.name, style = MaterialTheme.typography.headlineSmall)
                    Text(musclesLabel(ex.primaryMuscles).ifBlank { "Custom exercise" }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(ex.category, ex.equipment, ex.level).filter { it.isNotBlank() && it != "none" }.forEach { SuggestionChip(onClick = {}, label = { Text(it.titleCase()) }) }
            }
            if (ex.secondaryMuscles.isNotBlank()) {
                Text("Also works ${musclesLabel(ex.secondaryMuscles).lowercase()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onLogToday, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Today, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Log it")
                }
                OutlinedButton(onClick = { choosing = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.PlaylistAdd, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("To routine")
                }
            }
            if (ex.instructions.isNotBlank()) {
                Spacer(Modifier.height(28.dp))
                Text("How to do it", style = MaterialTheme.typography.titleMedium)
                ex.instructions.split('\n').forEachIndexed { i, step ->
                    Row(Modifier.padding(top = 14.dp)) {
                        Box(Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                            Text("${i + 1}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(step, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
    if (choosing) {
        ModalBottomSheet(onDismissRequest = { choosing = false }) {
            Text("Add to routine", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            if (routines.isEmpty()) Text("Create a routine first in the Routines tab.", Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            routines.forEach { r ->
                ListItem(
                    headlineContent = { Text(r.name) },
                    supportingContent = { Text("${r.exerciseCount} exercises") },
                    leadingContent = { ExerciseAvatar(r.name) },
                    modifier = Modifier.clickable { choosing = false; vm.addToRoutine(r.id, r.name, listOf(ex.id)) },
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
