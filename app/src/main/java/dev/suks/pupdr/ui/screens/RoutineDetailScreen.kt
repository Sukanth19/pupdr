package dev.suks.pupdr.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.suks.pupdr.data.RoutineExerciseRow
import dev.suks.pupdr.ui.ExercisesViewModel
import dev.suks.pupdr.ui.RoutineDetailViewModel
import dev.suks.pupdr.ui.components.AnimatedNumber
import dev.suks.pupdr.ui.components.EmptyState
import dev.suks.pupdr.ui.components.ExerciseIcon
import dev.suks.pupdr.ui.components.ExercisePicker
import dev.suks.pupdr.ui.components.musclesLabel
import dev.suks.pupdr.ui.theme.Motion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineDetailScreen(
    vm: RoutineDetailViewModel,
    exVm: ExercisesViewModel,
    isNew: Boolean,
    onBack: () -> Unit,
    onStart: (Long, String) -> Unit,
) {
    val routine by vm.routine.collectAsState()
    val items by vm.items.collectAsState()
    // a brand-new routine opens straight into the exercise library
    var picking by rememberSaveable { mutableStateOf(isNew) }
    var editingId by remember { mutableStateOf<Long?>(null) }
    var name by remember(routine?.id) { mutableStateOf(routine?.name ?: "") }
    val nameFocus = remember { FocusRequester() }
    val scroll = TopAppBarDefaults.pinnedScrollBehavior()
    val listState = rememberLazyListState()
    val fabExpanded by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }

    DisposableEffect(Unit) { onDispose { vm.finish(isNew) } }
    // after picking exercises for a new routine, jump to naming it
    LaunchedEffect(picking, routine?.id) {
        if (!picking && routine != null && name.isBlank() && items.isNotEmpty()) runCatching { nameFocus.requestFocus() }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    if (items.isNotEmpty()) {
                        TextButton(onClick = { routine?.let { onStart(it.id, name.ifBlank { "Routine" }) } }) {
                            Icon(Icons.Default.PlayArrow, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Start")
                        }
                    }
                    IconButton(onClick = onBack) { Icon(Icons.Default.Check, "Done") }
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
            item(key = "name") {
                Column(Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 16.dp)) {
                    BasicTextField(
                        value = name,
                        onValueChange = { name = it; vm.rename(it, routine?.notes ?: "") },
                        textStyle = MaterialTheme.typography.headlineLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth().focusRequester(nameFocus),
                        decorationBox = { inner ->
                            Box {
                                if (name.isEmpty()) Text("Name your routine", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.outline)
                                inner()
                            }
                        },
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "${items.size} exercise${if (items.size != 1) "s" else ""} · ${items.sumOf { it.targetSets }} sets",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (items.isEmpty()) {
                item(key = "empty") {
                    EmptyState(Icons.Outlined.FitnessCenter, "No exercises yet", "Add from the library of 870+ exercises. Tap one afterwards to set its sets and reps.", Modifier.animateItem()) {
                        Button(onClick = { picking = true }) { Text("Browse exercises") }
                    }
                }
            }
            itemsIndexed(items, key = { _, it -> it.id }) { i, item ->
                RoutineRow(
                    i, item,
                    modifier = Modifier.animateItem(fadeInSpec = Motion.enter(), placementSpec = Motion.offsetSpring, fadeOutSpec = Motion.exit()),
                    onClick = { editingId = item.id },
                )
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
    if (picking) {
        ExercisePicker(exVm, if (name.isBlank()) "Pick exercises" else "Add to $name", onDismiss = { picking = false }) {
            picking = false
            vm.add(it)
        }
    }
}

@Composable
private fun RoutineRow(index: Int, item: RoutineExerciseRow, modifier: Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(Modifier.clickable(onClick = onClick).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            ExerciseIcon(item.category, custom = item.exerciseId.startsWith("custom_"))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(item.exerciseName, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    "${index + 1} · ${musclesLabel(item.primaryMuscles).ifBlank { "Custom" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(8.dp))
            Text("${item.targetSets} × ${item.targetReps}", style = MaterialTheme.typography.titleMedium)
        }
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
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp).imePadding(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ExerciseIcon(item.category, size = 52.dp, custom = item.exerciseId.startsWith("custom_"))
                Spacer(Modifier.width(16.dp))
                Text(item.exerciseName, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Sets", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                FilledTonalIconButton(onClick = { if (sets > 1) { sets--; onTarget(sets, reps) } }) { Icon(Icons.Default.Remove, "Fewer sets") }
                AnimatedNumber("$sets", MaterialTheme.typography.headlineSmall)
                FilledTonalIconButton(onClick = { if (sets < 50) { sets++; onTarget(sets, reps) } }) { Icon(Icons.Default.Add, "More sets") }
            }
            OutlinedTextField(
                value = reps,
                onValueChange = { v -> reps = v.take(12); onTarget(sets, reps) },
                label = { Text("Reps") },
                supportingText = { Text("e.g. 8, 8-12, 30s") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onMove(-1) }, enabled = canUp) { Icon(Icons.Default.KeyboardArrowUp, null, Modifier.size(18.dp)); Text("Move up") }
                OutlinedButton(onClick = { onMove(1) }, enabled = canDown) { Icon(Icons.Default.KeyboardArrowDown, null, Modifier.size(18.dp)); Text("Move down") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onRemove) { Icon(Icons.Default.Delete, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Remove") }
                Button(onClick = onDismiss) { Text("Done") }
            }
        }
    }
}
