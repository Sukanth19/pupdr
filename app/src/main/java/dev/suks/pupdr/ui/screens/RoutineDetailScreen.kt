package dev.suks.pupdr.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.suks.pupdr.data.RoutineExerciseRow
import dev.suks.pupdr.ui.ExercisesViewModel
import dev.suks.pupdr.ui.RoutineDetailViewModel
import dev.suks.pupdr.ui.components.AnimatedNumber
import dev.suks.pupdr.ui.components.EmptyState
import dev.suks.pupdr.ui.components.ExerciseAvatar
import dev.suks.pupdr.ui.components.ExercisePicker
import dev.suks.pupdr.ui.components.TextInputDialog
import dev.suks.pupdr.ui.components.musclesLabel
import dev.suks.pupdr.ui.theme.Motion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineDetailScreen(vm: RoutineDetailViewModel, exVm: ExercisesViewModel, onBack: () -> Unit, onStart: (Long, String) -> Unit) {
    val routine by vm.routine.collectAsState()
    val items by vm.items.collectAsState()
    var picking by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var editingId by remember { mutableStateOf<Long?>(null) }
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val listState = rememberLazyListState()
    val fabExpanded by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            LargeTopAppBar(
                title = { Text(routine?.name ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = { renaming = true }) { Icon(Icons.Default.Edit, "Rename") }
                    IconButton(onClick = { routine?.let { onStart(it.id, it.name) } }, enabled = items.isNotEmpty()) { Icon(Icons.Default.PlayArrow, "Start today") }
                },
                scrollBehavior = scroll,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { picking = true }, expanded = fabExpanded, icon = { Icon(Icons.Default.Add, null) }, text = { Text("Add exercises") })
        },
    ) { pad ->
        LazyColumn(
            state = listState,
            modifier = Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (items.isEmpty()) {
                item(key = "empty") {
                    EmptyState(Icons.AutoMirrored.Filled.List, "Empty routine", "Add exercises, then tap one to set its sets and reps.", Modifier.animateItem())
                }
            }
            itemsIndexed(items, key = { _, it -> it.id }) { i, item ->
                Card(
                    modifier = Modifier.animateItem(fadeInSpec = Motion.enter(), placementSpec = Motion.offsetSpring, fadeOutSpec = Motion.exit()),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                ) {
                    ListItem(
                        modifier = Modifier.clickable { editingId = item.id },
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        leadingContent = { ExerciseAvatar(item.exerciseName) },
                        overlineContent = { Text("${i + 1}") },
                        headlineContent = { Text(item.exerciseName, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                        supportingContent = { Text(musclesLabel(item.primaryMuscles).ifBlank { "Custom" }) },
                        trailingContent = { Text("${item.targetSets} × ${item.targetReps}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary) },
                    )
                }
            }
        }
    }

    editingId?.let { id ->
        val idx = items.indexOfFirst { it.id == id }
        if (idx >= 0) {
            ItemSheet(
                item = items[idx],
                canUp = idx > 0,
                canDown = idx < items.lastIndex,
                onDismiss = { editingId = null },
                onTarget = { s, r -> vm.updateTarget(id, s, r) },
                onMove = { vm.move(id, it) },
                onRemove = { vm.remove(id); editingId = null },
            )
        } else editingId = null
    }
    if (picking) ExercisePicker(exVm, "Add to ${routine?.name ?: "routine"}", onDismiss = { picking = false }) { picking = false; vm.add(it) }
    if (renaming) TextInputDialog("Rename routine", "Name", initial = routine?.name ?: "", onDismiss = { renaming = false }) {
        renaming = false
        vm.rename(it, routine?.notes ?: "")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemSheet(
    item: RoutineExerciseRow,
    canUp: Boolean,
    canDown: Boolean,
    onDismiss: () -> Unit,
    onTarget: (Int, String) -> Unit,
    onMove: (Int) -> Unit,
    onRemove: () -> Unit,
) {
    var sets by remember(item.id) { mutableStateOf(item.targetSets) }
    var reps by remember(item.id) { mutableStateOf(item.targetReps) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp).imePadding(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ExerciseAvatar(item.exerciseName, size = 48.dp)
                Spacer(Modifier.width(16.dp))
                Text(item.exerciseName, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Sets", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                FilledTonalIconButton(onClick = { if (sets > 1) { sets--; onTarget(sets, reps) } }) { Icon(Icons.Default.Remove, "Fewer sets") }
                AnimatedNumber("$sets", MaterialTheme.typography.titleLarge)
                FilledTonalIconButton(onClick = { if (sets < 50) { sets++; onTarget(sets, reps) } }) { Icon(Icons.Default.Add, "More sets") }
            }
            OutlinedTextField(
                value = reps,
                onValueChange = { v -> reps = v.take(12); onTarget(sets, reps) },
                label = { Text("Reps / time") },
                supportingText = { Text("e.g. 8, 8-12, 30s, AMRAP") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onMove(-1) }, enabled = canUp) { Icon(Icons.Default.KeyboardArrowUp, null, Modifier.size(18.dp)); Text("Up") }
                OutlinedButton(onClick = { onMove(1) }, enabled = canDown) { Icon(Icons.Default.KeyboardArrowDown, null, Modifier.size(18.dp)); Text("Down") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onRemove) { Icon(Icons.Default.Delete, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Remove") }
                Button(onClick = onDismiss) { Text("Done") }
            }
        }
    }
}
