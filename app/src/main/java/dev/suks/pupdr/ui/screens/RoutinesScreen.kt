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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.suks.pupdr.data.RoutineSummary
import dev.suks.pupdr.ui.Exporter
import dev.suks.pupdr.ui.RoutinesViewModel
import dev.suks.pupdr.ui.components.EmptyState
import dev.suks.pupdr.ui.components.PasteImportDialog
import dev.suks.pupdr.ui.components.TextInputDialog

private fun fileSafe(s: String) = s.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifEmpty { "routine" }

private fun shareRoutines(context: Context, vm: RoutinesViewModel, ids: List<Long>, name: String) =
    vm.export(ids) { Exporter.share(context, "pupdr-${fileSafe(name)}.json", "application/json", it) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutinesScreen(vm: RoutinesViewModel, onOpen: (Long) -> Unit, onStarted: () -> Unit) {
    val routines by vm.routines.collectAsState()
    val context = LocalContext.current
    var creating by remember { mutableStateOf(false) }
    var pasting by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<RoutineSummary?>(null) }

    val openFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.import(Exporter.readFrom(context, uri))
    }
    val saveAll = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) vm.export(routines.map { it.id }) { Exporter.writeTo(context, uri, it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Routines") },
                actions = {
                    IconButton(onClick = { openFile.launch(arrayOf("application/json", "text/*", "application/octet-stream")) }) { Icon(Icons.Default.FileDownload, "Import from file") }
                    IconButton(onClick = { pasting = true }) { Icon(Icons.Default.ContentPaste, "Paste routine") }
                    IconButton(onClick = { saveAll.launch("pupdr-routines.json") }, enabled = routines.isNotEmpty()) { Icon(Icons.Default.FileUpload, "Export all") }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { creating = true }, icon = { Icon(Icons.Default.Add, null) }, text = { Text("New routine") },
                containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
        },
    ) { pad ->
        if (routines.isEmpty()) {
            Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState("No routines yet", "Build your own split, or import one from a file or pasted text (e.g. \"Bench Press 4x8\").") {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FilledTonalButton(onClick = { openFile.launch(arrayOf("application/json", "text/*", "application/octet-stream")) }) { Text("Import file") }
                        OutlinedButton(onClick = { pasting = true }) { Text("Paste") }
                    }
                }
            }
        } else {
            LazyColumn(
                Modifier.padding(pad).fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(routines, key = { it.id }) { r ->
                    RoutineCard(
                        r,
                        onOpen = { onOpen(r.id) },
                        onStart = { vm.startToday(r.id, r.name, onStarted) },
                        onShare = { shareRoutines(context, vm, listOf(r.id), r.name) },
                        onDelete = { confirmDelete = r },
                    )
                }
            }
        }
    }

    if (creating) TextInputDialog("New routine", "Name (e.g. Push Day)", confirm = "Create", onDismiss = { creating = false }) {
        creating = false
        vm.create(it, onOpen)
    }
    if (pasting) PasteImportDialog(onDismiss = { pasting = false }) { pasting = false; vm.import(it) }
    confirmDelete?.let { r ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Delete ${r.name}?") },
            text = { Text("Your logged workouts are kept.") },
            confirmButton = { TextButton(onClick = { vm.delete(r.id); confirmDelete = null }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun RoutineCard(r: RoutineSummary, onOpen: () -> Unit, onStart: () -> Unit, onShare: () -> Unit, onDelete: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Card(
        onClick = onOpen,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 6.dp, top = 14.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(r.name, style = MaterialTheme.typography.titleMedium)
                Text("${r.exerciseCount} exercises", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (r.notes.isNotBlank()) Text(r.notes, style = MaterialTheme.typography.bodySmall, maxLines = 2)
            }
            FilledTonalButton(onClick = onStart, enabled = r.exerciseCount > 0) {
                Icon(Icons.Default.PlayArrow, null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Start")
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "More") }
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem(text = { Text("Edit") }, onClick = { menu = false; onOpen() })
                    DropdownMenuItem(text = { Text("Share / export") }, onClick = { menu = false; onShare() })
                    DropdownMenuItem(text = { Text("Delete") }, onClick = { menu = false; onDelete() })
                }
            }
        }
    }
}
