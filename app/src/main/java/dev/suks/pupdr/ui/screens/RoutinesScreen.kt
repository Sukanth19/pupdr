package dev.suks.pupdr.ui.screens

import android.content.Context
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.suks.pupdr.data.RoutineSummary
import dev.suks.pupdr.ui.Exporter
import dev.suks.pupdr.ui.RoutinesViewModel
import dev.suks.pupdr.ui.components.EmptyState
import dev.suks.pupdr.ui.theme.Motion

private fun fileSafe(s: String) = s.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifEmpty { "routine" }

private fun shareRoutine(context: Context, vm: RoutinesViewModel, r: RoutineSummary) =
    vm.export(listOf(r.id)) { Exporter.share(context, "pupdr-${fileSafe(r.name)}.json", "application/json", it) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutinesScreen(vm: RoutinesViewModel, onOpen: (Long) -> Unit, onCreated: (Long) -> Unit, onStarted: () -> Unit) {
    val routines by vm.routines.collectAsState()
    val context = LocalContext.current
    var confirmDelete by remember { mutableStateOf<RoutineSummary?>(null) }
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val listState = rememberLazyListState()
    val fabExpanded by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }
    val create: () -> Unit = { vm.createNew(onCreated) }

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0),
        topBar = { LargeTopAppBar(title = { Text("Routines") }, scrollBehavior = scroll) },
        floatingActionButton = {
            if (routines.isNotEmpty()) {
                ExtendedFloatingActionButton(onClick = create, expanded = fabExpanded, icon = { Icon(Icons.Default.Add, null) }, text = { Text("New routine") })
            }
        },
    ) { pad ->
        LazyColumn(
            state = listState,
            modifier = Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (routines.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        Icons.AutoMirrored.Filled.List,
                        "Build your first routine",
                        "Pick exercises from the library, set your sets and reps, then start it any day in one tap.",
                        Modifier.animateItem(),
                    ) {
                        Button(onClick = create) {
                            Icon(Icons.Default.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("New routine")
                        }
                    }
                }
            }
            items(routines, key = { it.id }) { r ->
                RoutineCard(
                    r,
                    modifier = Modifier.animateItem(fadeInSpec = Motion.enter(), placementSpec = Motion.offsetSpring, fadeOutSpec = Motion.exit()),
                    onOpen = { onOpen(r.id) },
                    onStart = { vm.startToday(r.id, r.name, onStarted) },
                    onShare = { shareRoutine(context, vm, r) },
                    onDelete = { confirmDelete = r },
                )
            }
        }
    }

    confirmDelete?.let { r ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            icon = { Icon(Icons.Default.Delete, null) },
            title = { Text("Delete ${r.name.ifBlank { "this routine" }}?") },
            text = { Text("The routine is removed. Workouts you've already logged are kept.") },
            confirmButton = { TextButton(onClick = { vm.delete(r.id); confirmDelete = null }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun RoutineCard(r: RoutineSummary, modifier: Modifier, onOpen: () -> Unit, onStart: () -> Unit, onShare: () -> Unit, onDelete: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.clickable(onClick = onOpen).padding(start = 20.dp, end = 8.dp, top = 18.dp, bottom = 18.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(r.name.ifBlank { "Untitled routine" }, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${r.exerciseCount} exercise${if (r.exerciseCount != 1) "s" else ""} · ${r.totalSets} sets",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "More") }
                    DropdownMenu(menu, { menu = false }) {
                        DropdownMenuItem(text = { Text("Edit") }, leadingIcon = { Icon(Icons.Default.Edit, null) }, onClick = { menu = false; onOpen() })
                        DropdownMenuItem(text = { Text("Share") }, leadingIcon = { Icon(Icons.Default.IosShare, null) }, onClick = { menu = false; onShare() })
                        HorizontalDivider()
                        DropdownMenuItem(text = { Text("Delete") }, leadingIcon = { Icon(Icons.Default.Delete, null) }, onClick = { menu = false; onDelete() })
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    r.preview ?: "No exercises yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 12.dp),
                )
                FilledIconButton(onClick = onStart, enabled = r.exerciseCount > 0, modifier = Modifier.padding(end = 8.dp).size(48.dp)) {
                    Icon(Icons.Default.PlayArrow, "Start today")
                }
            }
        }
    }
}
