package dev.suks.pupdr.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.suks.pupdr.data.RoutineExerciseRow
import dev.suks.pupdr.ui.ExercisesViewModel
import dev.suks.pupdr.ui.RoutineDetailViewModel
import dev.suks.pupdr.ui.components.EmptyState
import dev.suks.pupdr.ui.components.ExercisePicker
import dev.suks.pupdr.ui.components.TextInputDialog
import dev.suks.pupdr.ui.components.musclesLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineDetailScreen(vm: RoutineDetailViewModel, exVm: ExercisesViewModel, onBack: () -> Unit) {
    val routine by vm.routine.collectAsState()
    val items by vm.items.collectAsState()
    var picking by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(routine?.name ?: "Routine") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = { IconButton(onClick = { renaming = true }) { Icon(Icons.Default.Edit, "Rename") } },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { picking = true }, icon = { Icon(Icons.Default.Add, null) }, text = { Text("Add exercises") },
                containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
        },
    ) { pad ->
        if (items.isEmpty()) {
            EmptyState("Empty routine", "Add exercises and set your target sets × reps.", Modifier.padding(pad))
        }
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            itemsIndexed(items, key = { _, it -> it.id }) { i, item ->
                RoutineItemCard(
                    item, i, items.size,
                    onTarget = { s, r -> vm.updateTarget(item.id, s, r) },
                    onMove = { vm.move(item.id, it) },
                    onRemove = { vm.remove(item.id) },
                )
            }
        }
    }
    if (picking) ExercisePicker(exVm, "Add to ${routine?.name ?: "routine"}", onDismiss = { picking = false }) { picking = false; vm.add(it) }
    if (renaming) TextInputDialog("Rename routine", "Name", initial = routine?.name ?: "", onDismiss = { renaming = false }) {
        renaming = false
        vm.rename(it, routine?.notes ?: "")
    }
}

@Composable
private fun RoutineItemCard(item: RoutineExerciseRow, index: Int, total: Int, onTarget: (Int, String) -> Unit, onMove: (Int) -> Unit, onRemove: () -> Unit) {
    var sets by remember(item.id) { mutableStateOf(item.targetSets) }
    var reps by remember(item.id) { mutableStateOf(item.targetReps) }
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${index + 1}. ${item.exerciseName}", style = MaterialTheme.typography.titleMedium)
                    Text(musclesLabel(item.primaryMuscles), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { onMove(-1) }, enabled = index > 0) { Icon(Icons.Default.KeyboardArrowUp, "Move up") }
                IconButton(onClick = { onMove(1) }, enabled = index < total - 1) { Icon(Icons.Default.KeyboardArrowDown, "Move down") }
                IconButton(onClick = onRemove) { Icon(Icons.Default.Delete, "Remove") }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Sets", style = MaterialTheme.typography.labelLarge)
                IconButton(onClick = { if (sets > 1) { sets--; onTarget(sets, reps) } }) { Icon(Icons.Default.Remove, "Fewer sets") }
                Text("$sets", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { if (sets < 50) { sets++; onTarget(sets, reps) } }) { Icon(Icons.Default.Add, "More sets") }
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = reps,
                    onValueChange = { v -> reps = v.filter { it.isDigit() || it == '-' }.take(7); onTarget(sets, reps) },
                    label = { Text("Reps") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f).padding(end = 12.dp),
                )
            }
        }
    }
}
