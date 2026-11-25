package dev.suks.pupdr.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.suks.pupdr.domain.DraftItem
import dev.suks.pupdr.domain.DraftRoutine
import dev.suks.pupdr.ui.ExercisesViewModel
import dev.suks.pupdr.ui.RoutinesViewModel

/** Full-screen review of an import: every line can be re-matched, edited or dropped before saving. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportReview(drafts: List<DraftRoutine>, vm: RoutinesViewModel, exVm: ExercisesViewModel, onDismiss: () -> Unit) {
    var editing by remember { mutableStateOf<Pair<Long, DraftItem>?>(null) }
    var picking by remember { mutableStateOf<Pair<Long, DraftItem>?>(null) }
    val items = drafts.flatMap { it.items }
    val newCount = items.count { it.isNew }
    val unsure = items.count { it.isUnsure }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Review import") },
                    navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Cancel") } },
                    actions = {
                        TextButton(onClick = vm::commit, enabled = items.isNotEmpty()) { Text("Save") }
                    },
                )
            },
            bottomBar = {
                Surface(tonalElevation = 3.dp) {
                    Button(
                        onClick = vm::commit,
                        enabled = items.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth().padding(16.dp).imePadding(),
                        contentPadding = PaddingValues(vertical = 16.dp),
                    ) { Text("Import ${drafts.size} routine${if (drafts.size != 1) "s" else ""} · ${items.size} exercises") }
                }
            },
        ) { pad ->
            LazyColumn(
                Modifier.padding(pad).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item(key = "summary") {
                    Text(
                        buildString {
                            append("${items.size - newCount - unsure} matched")
                            if (unsure > 0) append(" · $unsure to check")
                            if (newCount > 0) append(" · $newCount new")
                            append(". Tap any exercise to change it.")
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                items(drafts, key = { it.key }) { r ->
                    Card(
                        modifier = Modifier.animateItem().animateContentSize(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    ) {
                        Column(Modifier.padding(vertical = 12.dp)) {
                            Row(Modifier.padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = r.name,
                                    onValueChange = { vm.renameDraft(r.key, it) },
                                    label = { Text("Routine name") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                                    modifier = Modifier.weight(1f),
                                )
                                IconButton(onClick = { vm.removeDraftRoutine(r.key) }) { Icon(Icons.Default.Delete, "Skip routine") }
                            }
                            Spacer(Modifier.height(8.dp))
                            r.items.forEachIndexed { i, item ->
                                if (i > 0) HorizontalDivider(Modifier.padding(start = 72.dp), color = MaterialTheme.colorScheme.outlineVariant)
                                DraftRow(item) { editing = r.key to item }
                            }
                        }
                    }
                }
            }
        }
    }

    editing?.let { (rk, item) ->
        DraftItemSheet(
            item = item,
            onDismiss = { editing = null },
            onChange = { vm.updateItem(rk, it); editing = rk to it },
            onRemove = { vm.removeItem(rk, item.key); editing = null },
            onPick = { picking = rk to item; editing = null },
        )
    }
    picking?.let { (rk, item) ->
        ExercisePicker(exVm, "Match \"${item.source}\"", onDismiss = { picking = null }, single = true) { ids ->
            val ex = exVm.all.value.firstOrNull { it.id == ids.first() }
            if (ex != null) vm.updateItem(rk, item.copy(exerciseId = ex.id, exerciseName = ex.name, confidence = 1.0))
            picking = null
        }
    }
}

@Composable
private fun DraftRow(item: DraftItem, onClick: () -> Unit) {
    val (icon, tint, status) = when {
        item.isNew -> Triple(Icons.Outlined.AddCircleOutline, MaterialTheme.colorScheme.tertiary, "New exercise")
        item.isUnsure -> Triple(Icons.Default.HelpOutline, MaterialTheme.colorScheme.error, "Best guess · check")
        else -> Triple(Icons.Default.CheckCircle, MaterialTheme.colorScheme.primary, null)
    }
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        leadingContent = { Icon(icon, null, tint = tint) },
        overlineContent = if (!item.source.equals(item.exerciseName, ignoreCase = true)) {
            { Text("“${item.source}”", maxLines = 1, overflow = TextOverflow.Ellipsis) }
        } else null,
        headlineContent = { Text(item.exerciseName, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        supportingContent = status?.let { { Text(it, color = tint) } },
        trailingContent = { Text("${item.sets} × ${item.reps}", style = MaterialTheme.typography.labelLarge) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DraftItemSheet(item: DraftItem, onDismiss: () -> Unit, onChange: (DraftItem) -> Unit, onRemove: () -> Unit, onPick: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp).imePadding(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("“${item.source}”", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (item.isNew) {
                OutlinedTextField(
                    value = item.exerciseName,
                    onValueChange = { onChange(item.copy(exerciseName = it)) },
                    label = { Text("New exercise name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Text(item.exerciseName, style = MaterialTheme.typography.headlineSmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onPick) {
                    Icon(Icons.Default.SwapHoriz, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                    Text(if (item.isNew) "Pick from library" else "Change exercise")
                }
                if (!item.isNew) TextButton(onClick = { onChange(item.copy(exerciseId = null, exerciseName = item.source)) }) { Text("Keep as new") }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Sets", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                FilledTonalIconButton(onClick = { onChange(item.copy(sets = (item.sets - 1).coerceAtLeast(1))) }) { Icon(Icons.Default.Remove, "Fewer") }
                AnimatedNumber("${item.sets}", MaterialTheme.typography.titleLarge)
                FilledTonalIconButton(onClick = { onChange(item.copy(sets = (item.sets + 1).coerceAtMost(50))) }) { Icon(Icons.Default.Add, "More") }
            }
            OutlinedTextField(
                value = item.reps,
                onValueChange = { onChange(item.copy(reps = it.take(12))) },
                label = { Text("Reps / time") },
                supportingText = { Text("e.g. 8, 8-12, 30s, AMRAP") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Remove")
                }
                Button(onClick = onDismiss) { Text("Done") }
            }
        }
    }
}
