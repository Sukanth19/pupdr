package dev.suks.pupdr.ui.screens

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import dev.suks.pupdr.data.RoutineSummary
import dev.suks.pupdr.ui.ExercisesViewModel
import dev.suks.pupdr.ui.Exporter
import dev.suks.pupdr.ui.RoutinesViewModel
import dev.suks.pupdr.ui.components.EmptyState
import dev.suks.pupdr.ui.components.ExerciseAvatar
import dev.suks.pupdr.ui.components.ImportReview
import dev.suks.pupdr.ui.components.PasteImportSheet
import dev.suks.pupdr.ui.components.TextInputDialog
import dev.suks.pupdr.ui.theme.Motion

private fun fileSafe(s: String) = s.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifEmpty { "routine" }

private fun shareRoutines(context: Context, vm: RoutinesViewModel, ids: List<Long>, name: String) =
    vm.export(ids) { Exporter.share(context, "pupdr-${fileSafe(name)}.json", "application/json", it) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutinesScreen(vm: RoutinesViewModel, exVm: ExercisesViewModel, onOpen: (Long) -> Unit, onStarted: () -> Unit) {
    val routines by vm.routines.collectAsState()
    val draft by vm.draft.collectAsState()
    val parsing by vm.parsing.collectAsState()
    val context = LocalContext.current
    var creating by remember { mutableStateOf(false) }
    var pasting by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<RoutineSummary?>(null) }
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val listState = rememberLazyListState()
    val fabExpanded by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }

    val openFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.preview(runCatching { Exporter.readFrom(context, uri) }.getOrDefault(""))
    }
    val saveAll = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) vm.export(routines.map { it.id }) { Exporter.writeTo(context, uri, it) }
    }
    fun importFile() = openFile.launch(arrayOf("application/json", "text/*", "application/octet-stream", "*/*"))

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            LargeTopAppBar(
                title = { Text("Routines") },
                actions = {
                    IconButton(onClick = { pasting = true }) { Icon(Icons.Default.ContentPaste, "Paste a routine") }
                    IconButton(onClick = ::importFile) { Icon(Icons.Default.FileOpen, "Import a file") }
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "More") }
                        DropdownMenu(menu, { menu = false }) {
                            DropdownMenuItem(
                                text = { Text("Export all routines") },
                                leadingIcon = { Icon(Icons.Default.SaveAlt, null) },
                                enabled = routines.isNotEmpty(),
                                onClick = { menu = false; saveAll.launch("pupdr-routines.json") },
                            )
                        }
                    }
                },
                scrollBehavior = scroll,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { creating = true }, expanded = fabExpanded, icon = { Icon(Icons.Default.Add, null) }, text = { Text("New routine") })
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
                        "No routines yet",
                        "Build one from scratch, or paste a plan from anywhere. Notes, tables and spreadsheets all work.",
                        Modifier.animateItem(),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            FilledTonalButton(onClick = { pasting = true }) { Text("Paste text") }
                            OutlinedButton(onClick = ::importFile) { Text("Open file") }
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
                    onShare = { shareRoutines(context, vm, listOf(r.id), r.name) },
                    onDelete = { confirmDelete = r },
                )
            }
        }
    }

    if (creating) TextInputDialog("New routine", "Name", confirm = "Create", onDismiss = { creating = false }) {
        creating = false
        vm.create(it, onOpen)
    }
    if (pasting) PasteImportSheet(parsing, onDismiss = { pasting = false }) { text -> vm.preview(text) }
    LaunchedEffect(draft != null) { if (draft != null) pasting = false }
    draft?.let { d ->
        ImportReview(d, vm, exVm, onDismiss = { vm.draft.value = null })
    }
    confirmDelete?.let { r ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            icon = { Icon(Icons.Default.Delete, null) },
            title = { Text("Delete ${r.name}?") },
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
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(Modifier.clickable(onClick = onOpen).padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            ExerciseAvatar(r.name, size = 48.dp)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(r.name, style = MaterialTheme.typography.titleMedium)
                Text("${r.exerciseCount} exercise${if (r.exerciseCount != 1) "s" else ""}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FilledTonalIconButton(onClick = onStart, enabled = r.exerciseCount > 0) { Icon(Icons.Default.PlayArrow, "Start today") }
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
    }
}

