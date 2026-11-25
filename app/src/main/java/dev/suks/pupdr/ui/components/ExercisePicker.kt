package dev.suks.pupdr.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.suks.pupdr.data.Exercise
import dev.suks.pupdr.ui.ExercisesViewModel

/** Full-screen multi-select exercise picker. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisePicker(
    vm: ExercisesViewModel,
    title: String,
    onDismiss: () -> Unit,
    onPicked: (List<String>) -> Unit,
) {
    val all by vm.all.collectAsState()
    val muscles by vm.muscles.collectAsState()
    var query by remember { mutableStateOf("") }
    var muscle by remember { mutableStateOf<String?>(null) }
    val selected = remember { mutableStateListOf<String>() }
    var creating by remember { mutableStateOf(false) }
    val list = remember(all, query, muscle) { ExercisesViewModel.filter(all, query, muscle, null) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                    actions = { TextButton(onClick = { creating = true }) { Text("Custom") } },
                )
            },
            bottomBar = {
                Row(Modifier.fillMaxWidth().padding(16.dp).imePadding()) {
                    Button(
                        onClick = { onPicked(selected.toList()) },
                        enabled = selected.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(16.dp),
                    ) { Text(if (selected.isEmpty()) "Select exercises" else "Add ${selected.size} exercise${if (selected.size > 1) "s" else ""}") }
                }
            },
        ) { pad ->
            Column(Modifier.padding(pad).fillMaxSize()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    placeholder = { Text("Search ${all.size} exercises") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                )
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(muscles) { m ->
                        FilterChip(selected = muscle == m, onClick = { muscle = if (muscle == m) null else m }, label = { Text(m.titleCase()) })
                    }
                }
                LazyColumn(Modifier.fillMaxSize()) {
                    items(list, key = { it.id }) { ex ->
                        val isSel = ex.id in selected
                        ExerciseRow(ex, isSel) { if (isSel) selected.remove(ex.id) else selected.add(ex.id) }
                    }
                    if (list.isEmpty() && query.isNotBlank()) {
                        item {
                            ListItem(
                                headlineContent = { Text("Create \"$query\"") },
                                supportingContent = { Text("Add it as a custom exercise") },
                                leadingContent = { Icon(Icons.Default.Add, null) },
                                modifier = Modifier.clickable { creating = true },
                            )
                        }
                    }
                }
            }
        }
    }
    if (creating) {
        CustomExerciseDialog(initialName = query, onDismiss = { creating = false }) { name, m, eq ->
            creating = false
            vm.createCustom(name, m, eq) { selected.add(it.id); query = "" }
        }
    }
}

@Composable
private fun ExerciseRow(ex: Exercise, selected: Boolean, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(ex.name) },
        supportingContent = { Text(listOf(musclesLabel(ex.primaryMuscles), ex.equipment.titleCase()).filter { it.isNotBlank() }.joinToString(" · ")) },
        trailingContent = {
            if (selected) Icon(Icons.Default.CheckCircle, "Selected", tint = MaterialTheme.colorScheme.primary)
            else Icon(Icons.Outlined.Circle, null, tint = MaterialTheme.colorScheme.outline)
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}
