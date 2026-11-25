package dev.suks.pupdr.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
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
import dev.suks.pupdr.ui.ExercisesViewModel
import dev.suks.pupdr.ui.theme.Motion

/** Full-screen exercise picker. Multi-select by default; [single] returns on the first tap. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisePicker(
    vm: ExercisesViewModel,
    title: String,
    onDismiss: () -> Unit,
    single: Boolean = false,
    onPicked: (List<String>) -> Unit,
) {
    val all by vm.all.collectAsState()
    val muscles by vm.muscles.collectAsState()
    var query by remember { mutableStateOf("") }
    var muscle by remember { mutableStateOf<String?>(null) }
    val selected = remember { mutableStateListOf<String>() }
    var creating by remember { mutableStateOf(false) }
    val list = remember(all, query, muscle) { ExercisesViewModel.filter(all, query, muscle, null) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                    actions = { TextButton(onClick = { creating = true }) { Text("New") } },
                )
            },
            floatingActionButton = {
                AnimatedVisibility(
                    visible = selected.isNotEmpty(),
                    enter = slideInVertically(Motion.enter()) { it * 2 } + fadeIn(Motion.enter()),
                    exit = slideOutVertically(Motion.exit()) { it * 2 } + fadeOut(Motion.exit()),
                ) {
                    ExtendedFloatingActionButton(
                        onClick = { onPicked(selected.toList()) },
                        icon = { Icon(Icons.Default.Check, null) },
                        text = { Text("Add ${selected.size}") },
                        modifier = Modifier.imePadding(),
                    )
                }
            },
        ) { pad ->
            Column(Modifier.padding(pad).fillMaxSize()) {
                SearchField(query, { query = it }, "Search ${all.size} exercises", Modifier.padding(horizontal = 16.dp))
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(muscles, key = { it }) { m ->
                        FilterChip(
                            selected = muscle == m,
                            onClick = { muscle = if (muscle == m) null else m },
                            label = { Text(m.titleCase()) },
                            leadingIcon = if (muscle == m) { { Icon(Icons.Default.Check, null, Modifier.padding(0.dp)) } } else null,
                            colors = FilterChipDefaults.filterChipColors(),
                        )
                    }
                }
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
                    if (list.isEmpty() && query.isNotBlank()) {
                        item(key = "create") {
                            ListItem(
                                headlineContent = { Text("Create \"${query.trim()}\"") },
                                supportingContent = { Text("Add it as your own exercise") },
                                leadingContent = { Icon(Icons.Default.Add, null) },
                                modifier = Modifier.clickable { creating = true }.animateItem(),
                            )
                        }
                    }
                    items(list, key = { it.id }) { ex ->
                        val isSel = ex.id in selected
                        ListItem(
                            headlineContent = { Text(ex.name) },
                            supportingContent = { Text(listOf(musclesLabel(ex.primaryMuscles), ex.equipment.titleCase()).filter { it.isNotBlank() }.joinToString(" · ")) },
                            leadingContent = { ExerciseAvatar(ex.name) },
                            trailingContent = if (single) null else { { Checkbox(checked = isSel, onCheckedChange = null) } },
                            colors = ListItemDefaults.colors(
                                containerColor = if (isSel) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                            ),
                            modifier = Modifier.animateItem().clickable {
                                if (single) onPicked(listOf(ex.id))
                                else if (isSel) selected.remove(ex.id) else selected.add(ex.id)
                            },
                        )
                    }
                }
            }
        }
    }
    if (creating) {
        CustomExerciseDialog(initialName = query, onDismiss = { creating = false }) { name, m, eq ->
            creating = false
            vm.createCustom(name, m, eq) {
                if (single) onPicked(listOf(it.id)) else { selected.add(it.id); query = "" }
            }
        }
    }
}

