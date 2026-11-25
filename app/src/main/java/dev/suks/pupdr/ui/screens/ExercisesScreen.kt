package dev.suks.pupdr.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.suks.pupdr.data.Exercise
import dev.suks.pupdr.data.RoutineSummary
import dev.suks.pupdr.ui.ExercisesViewModel
import dev.suks.pupdr.ui.components.CustomExerciseDialog
import dev.suks.pupdr.ui.components.musclesLabel
import dev.suks.pupdr.ui.components.titleCase

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

    Scaffold(
        topBar = { TopAppBar(title = { Text("Exercises") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { creating = true }, containerColor = MaterialTheme.colorScheme.primary) { Icon(Icons.Default.Add, "Custom exercise") }
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = { vm.query.value = it },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                placeholder = { Text("Search ${all.size} exercises") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(muscles) { m -> FilterChip(muscle == m, { vm.muscle.value = if (muscle == m) null else m }, { Text(m.titleCase()) }) }
            }
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(equipments) { e -> FilterChip(equipment == e, { vm.equipment.value = if (equipment == e) null else e }, { Text(e.titleCase()) }) }
            }
            Text(
                "${list.size} results",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 88.dp)) {
                items(list, key = { it.id }) { ex ->
                    ListItem(
                        headlineContent = { Text(ex.name) },
                        supportingContent = {
                            Text(listOf(musclesLabel(ex.primaryMuscles), ex.equipment.titleCase(), ex.level.titleCase()).filter { it.isNotBlank() }.joinToString(" · "))
                        },
                        modifier = Modifier.clickable { detail = ex },
                    )
                }
            }
        }
    }

    detail?.let { ex -> ExerciseDetailSheet(ex, routines, vm, onDismiss = { detail = null }, onLogToday = { onLogToday(ex.id); detail = null }) }
    if (creating) {
        CustomExerciseDialog(initialName = query, onDismiss = { creating = false }) { n, m, e -> creating = false; vm.createCustom(n, m, e) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseDetailSheet(ex: Exercise, routines: List<RoutineSummary>, vm: ExercisesViewModel, onDismiss: () -> Unit, onLogToday: () -> Unit) {
    var choosing by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp).verticalScroll(rememberScrollState())) {
            Text(ex.name, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(ex.category, ex.equipment, ex.level).filter { it.isNotBlank() }.forEach { AssistChip(onClick = {}, label = { Text(it.titleCase()) }) }
            }
            Spacer(Modifier.height(8.dp))
            Text("Primary: ${musclesLabel(ex.primaryMuscles).ifBlank { "—" }}", color = MaterialTheme.colorScheme.primary)
            if (ex.secondaryMuscles.isNotBlank()) Text("Secondary: ${musclesLabel(ex.secondaryMuscles)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onLogToday, modifier = Modifier.weight(1f)) { Text("Log on current day") }
                OutlinedButton(onClick = { choosing = true }, modifier = Modifier.weight(1f)) { Text("Add to routine") }
            }
            if (ex.instructions.isNotBlank()) {
                Spacer(Modifier.height(20.dp))
                Text("How to", style = MaterialTheme.typography.titleMedium)
                ex.instructions.split('\n').forEachIndexed { i, step ->
                    Text("${i + 1}. $step", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    }
    if (choosing) {
        AlertDialog(
            onDismissRequest = { choosing = false },
            title = { Text("Add to routine") },
            text = {
                if (routines.isEmpty()) Text("Create a routine first in the Routines tab.")
                else Column {
                    routines.forEach { r ->
                        ListItem(
                            headlineContent = { Text(r.name) },
                            supportingContent = { Text("${r.exerciseCount} exercises") },
                            modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable {
                                choosing = false
                                vm.addToRoutine(r.id, r.name, listOf(ex.id))
                            },
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { choosing = false }) { Text("Close") } },
        )
    }
}
