package dev.suks.pupdr.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.suks.pupdr.data.SetWithExercise
import dev.suks.pupdr.domain.ReportBuilder
import dev.suks.pupdr.ui.ExerciseGroup
import dev.suks.pupdr.ui.ExercisesViewModel
import dev.suks.pupdr.ui.WorkoutViewModel
import dev.suks.pupdr.ui.components.AnimatedNumber
import dev.suks.pupdr.ui.components.EmptyState
import dev.suks.pupdr.ui.components.ExerciseAvatar
import dev.suks.pupdr.ui.components.ExercisePicker
import dev.suks.pupdr.ui.components.musclesLabel
import dev.suks.pupdr.ui.theme.Motion
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

private const val WEEK_PAGES = 1200
private const val CURRENT_WEEK_PAGE = 1100

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(vm: WorkoutViewModel, exVm: ExercisesViewModel) {
    val day by vm.day.collectAsState()
    val groups by vm.groups.collectAsState()
    val routines by vm.routines.collectAsState()
    val streak by vm.streak.collectAsState()
    val trained by vm.trainedDays.collectAsState()
    val unit by vm.unit.collectAsState()
    val dynamic by vm.dynamicColor.collectAsState()
    var picking by remember { mutableStateOf(false) }
    var choosingRoutine by remember { mutableStateOf(false) }
    var pickingDate by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }

    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val listState = rememberLazyListState()
    val fabExpanded by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }
    val today = LocalDate.now()

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            LargeTopAppBar(
                title = {
                    AnimatedContent(
                        targetState = day,
                        transitionSpec = {
                            val forward = targetState > initialState
                            (slideInHorizontally(Motion.enter()) { if (forward) it / 4 else -it / 4 } + fadeIn(Motion.enter()))
                                .togetherWith(slideOutHorizontally(Motion.exit()) { if (forward) -it / 4 else it / 4 } + fadeOut(Motion.exit()))
                        },
                        label = "day-title",
                    ) { d -> Text(dayTitle(d, today), maxLines = 1, overflow = TextOverflow.Ellipsis) }
                },
                actions = {
                    IconButton(onClick = { choosingRoutine = true }) { Icon(Icons.AutoMirrored.Filled.PlaylistPlay, "Start a routine") }
                    IconButton(onClick = { pickingDate = true }) { Icon(Icons.Default.CalendarMonth, "Pick a date") }
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "Settings") }
                        SettingsMenu(menu, { menu = false }, unit, vm::setUnit, dynamic, vm::setDynamicColor)
                    }
                },
                scrollBehavior = scroll,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { picking = true },
                expanded = fabExpanded,
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Exercise") },
            )
        },
    ) { pad ->
        LazyColumn(
            state = listState,
            modifier = Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "week") { WeekStrip(day, today, trained, vm::setDay) }
            item(key = "summary") {
                DaySummary(groups, unit, streak, Modifier.padding(horizontal = 16.dp))
            }
            if (groups.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        icon = Icons.Default.FitnessCenter,
                        title = if (day == today) "Ready when you are" else "Nothing logged",
                        body = "Start one of your routines, or add exercises one by one.",
                        modifier = Modifier.animateItem(),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            FilledTonalButton(onClick = { choosingRoutine = true }) { Text("Start routine") }
                            OutlinedButton(onClick = { picking = true }) { Text("Add exercise") }
                        }
                    }
                }
            }
            items(groups, key = { it.exerciseId }) { g ->
                ExerciseCard(
                    group = g,
                    unit = unit,
                    onAddSet = { vm.addSet(g) },
                    onUpdate = vm::updateSet,
                    onDeleteSet = vm::deleteSet,
                    onRemove = { vm.removeExercise(g.exerciseId) },
                    modifier = Modifier.padding(horizontal = 16.dp).animateItem(
                        fadeInSpec = Motion.enter(), placementSpec = Motion.offsetSpring, fadeOutSpec = Motion.exit(),
                    ),
                )
            }
        }
    }

    if (picking) {
        ExercisePicker(exVm, "Add exercises", onDismiss = { picking = false }) { ids ->
            picking = false
            vm.addExercises(ids)
        }
    }
    if (choosingRoutine) {
        ModalBottomSheet(onDismissRequest = { choosingRoutine = false }) {
            Text("Start a routine", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            if (routines.isEmpty()) {
                Text(
                    "You don't have any routines yet. Create or import one in the Routines tab.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                )
            }
            routines.forEach { r ->
                ListItem(
                    headlineContent = { Text(r.name) },
                    supportingContent = { Text("${r.exerciseCount} exercises") },
                    leadingContent = { ExerciseAvatar(r.name) },
                    modifier = Modifier.clickable {
                        choosingRoutine = false
                        vm.startRoutine(r.id, r.name)
                    },
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }
    if (pickingDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = day.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { vm.setDay(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                    pickingDate = false
                }) { Text("Go") }
            },
            dismissButton = { TextButton(onClick = { pickingDate = false }) { Text("Cancel") } },
        ) { DatePicker(state) }
    }
}

private fun dayTitle(d: LocalDate, today: LocalDate) = when (d) {
    today -> "Today"
    today.minusDays(1) -> "Yesterday"
    today.plusDays(1) -> "Tomorrow"
    else -> d.format(DateTimeFormatter.ofPattern(if (d.year == today.year) "EEE, d MMM" else "d MMM yyyy"))
}

@Composable
private fun SettingsMenu(open: Boolean, onClose: () -> Unit, unit: String, setUnit: (String) -> Unit, dynamic: Boolean, setDynamic: (Boolean) -> Unit) {
    DropdownMenu(expanded = open, onDismissRequest = onClose) {
        Text("Weight unit", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        listOf("kg", "lb").forEach { u ->
            DropdownMenuItem(
                text = { Text(if (u == "kg") "Kilograms" else "Pounds") },
                leadingIcon = { RadioButton(selected = unit == u, onClick = null) },
                onClick = { setUnit(u) },
            )
        }
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text("Wallpaper colours") },
            trailingIcon = { Switch(checked = dynamic, onCheckedChange = null) },
            onClick = { setDynamic(!dynamic) },
        )
    }
}

@Composable
private fun WeekStrip(day: LocalDate, today: LocalDate, trained: Set<Long>, onSelect: (LocalDate) -> Unit) {
    val thisWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val pager = rememberPagerState(initialPage = CURRENT_WEEK_PAGE) { WEEK_PAGES }
    LaunchedEffect(day) {
        val target = CURRENT_WEEK_PAGE + ChronoUnit.WEEKS.between(thisWeek, day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))).toInt()
        if (target != pager.currentPage && target in 0 until WEEK_PAGES) pager.animateScrollToPage(target)
    }
    HorizontalPager(pager, contentPadding = PaddingValues(horizontal = 12.dp), pageSpacing = 8.dp) { page ->
        val start = thisWeek.plusWeeks((page - CURRENT_WEEK_PAGE).toLong())
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            (0L until 7L).map { start.plusDays(it) }.forEach { d ->
                DayPill(d, selected = d == day, isToday = d == today, trained = d.toEpochDay() in trained, onClick = { onSelect(d) })
            }
        }
    }
}

@Composable
private fun DayPill(d: LocalDate, selected: Boolean, isToday: Boolean, trained: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface, Motion.standard(), label = "pill-bg")
    val fg by animateColorAsState(if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, Motion.standard(), label = "pill-fg")
    val scale by animateFloatAsState(if (selected) 1f else 0.94f, Motion.bouncy, label = "pill-scale")
    Column(
        Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .then(if (isToday && !selected) Modifier.border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(20.dp)) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp)
            .width(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(d.format(DateTimeFormatter.ofPattern("EEEEE")), style = MaterialTheme.typography.labelMedium, color = fg.copy(alpha = 0.75f))
        Text("${d.dayOfMonth}", style = MaterialTheme.typography.titleMedium, color = fg)
        Box(
            Modifier.padding(top = 4.dp).size(5.dp).clip(CircleShape)
                .background(if (trained) (if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary) else bg)
        )
    }
}

@Composable
private fun DaySummary(groups: List<ExerciseGroup>, unit: String, streak: Int, modifier: Modifier) {
    val sets = groups.flatMap { it.sets }
    val done = sets.count { it.done }
    val volume = sets.filter(ReportBuilder::performed).sumOf { it.reps * it.weight }
    val progress by animateFloatAsState(if (sets.isEmpty()) 0f else done.toFloat() / sets.size, Motion.standard(500), label = "progress")
    Column(modifier.animateContentSize(Motion.sizeSpring)) {
        if (streak > 0) {
            AssistChip(
                onClick = {},
                label = { Text("$streak day streak") },
                leadingIcon = { Icon(Icons.Default.LocalFireDepartment, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(AssistChipDefaults.IconSize)) },
            )
        }
        AnimatedVisibility(sets.isNotEmpty(), enter = expandVertically(Motion.enter()) + fadeIn(Motion.enter()), exit = shrinkVertically(Motion.exit()) + fadeOut(Motion.exit())) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.padding(top = 4.dp),
            ) {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        AnimatedNumber("$done", MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        Text(" / ${sets.size} sets", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.padding(bottom = 6.dp))
                        Spacer(Modifier.weight(1f))
                        Column(horizontalAlignment = Alignment.End) {
                            AnimatedNumber(ReportBuilder.fmt(volume), MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            Text("$unit volume", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                        gapSize = 0.dp,
                        drawStopIndicator = {},
                    )
                }
            }
        }
    }
}

@Composable
private fun ExerciseCard(
    group: ExerciseGroup,
    unit: String,
    onAddSet: () -> Unit,
    onUpdate: (Long, Int, Double, Boolean) -> Unit,
    onDeleteSet: (Long) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier,
) {
    var menu by remember { mutableStateOf(false) }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.animateContentSize(Motion.sizeSpring).padding(vertical = 8.dp)) {
            Row(Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                ExerciseAvatar(group.name)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(group.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${musclesLabel(group.muscles).ifBlank { "Exercise" }} · ${group.done}/${group.sets.size} done",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "More") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Remove exercise") }, leadingIcon = { Icon(Icons.Default.Delete, null) }, onClick = { menu = false; onRemove() })
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            group.sets.forEach { s -> key(s.id) { SetRow(s, unit, onUpdate, onDeleteSet) } }
            TextButton(onClick = onAddSet, modifier = Modifier.padding(start = 8.dp)) {
                Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Add set")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SetRow(s: SetWithExercise, unit: String, onUpdate: (Long, Int, Double, Boolean) -> Unit, onDelete: (Long) -> Unit) {
    var weight by remember(s.id) { mutableStateOf(if (s.weight == 0.0) "" else ReportBuilder.fmt(s.weight)) }
    var reps by remember(s.id) { mutableStateOf(if (s.reps == 0) "" else s.reps.toString()) }
    var done by remember(s.id) { mutableStateOf(s.done) }
    val haptics = LocalHapticFeedback.current
    fun push() = onUpdate(s.id, reps.toIntOrNull() ?: 0, weight.replace(',', '.').toDoubleOrNull() ?: 0.0, done)

    val dismiss = rememberSwipeToDismissBoxState()
    LaunchedEffect(dismiss.currentValue) {
        if (dismiss.currentValue == SwipeToDismissBoxValue.EndToStart) onDelete(s.id)
    }
    val rowBg by animateColorAsState(
        if (done) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        Motion.standard(), label = "set-bg",
    )
    val badgeScale by animateFloatAsState(if (done) 1f else 0.86f, Motion.bouncy, label = "badge")

    SwipeToDismissBox(
        state = dismiss,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 2.dp).clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.errorContainer).padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) { Icon(Icons.Default.Delete, "Delete set", tint = MaterialTheme.colorScheme.onErrorContainer) }
        },
    ) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp).clip(RoundedCornerShape(16.dp)).background(rowBg)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    Modifier.size(32.dp).graphicsLayer { scaleX = badgeScale; scaleY = badgeScale }.clip(CircleShape)
                        .background(if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "${s.setIndex}",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (done) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                NumberField(weight, { weight = it; push() }, unit, KeyboardType.Decimal, Modifier.weight(1f))
                NumberField(reps, { reps = it.filter(Char::isDigit); push() }, "reps", KeyboardType.Number, Modifier.weight(1f))
                Checkbox(
                    checked = done,
                    onCheckedChange = {
                        done = it
                        haptics.performHapticFeedback(if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
                        push()
                    },
                )
            }
        }
    }
}

@Composable
private fun NumberField(value: String, onChange: (String) -> Unit, suffix: String, type: KeyboardType, modifier: Modifier) {
    val color = MaterialTheme.colorScheme.onSurface
    BasicTextField(
        value = value,
        onValueChange = { v -> if (v.length <= 7) onChange(v.filter { it.isDigit() || it == '.' || it == ',' }) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = type),
        textStyle = MaterialTheme.typography.titleMedium.copy(color = color, textAlign = TextAlign.End),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = modifier,
        decorationBox = { inner ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                    if (value.isEmpty()) Text("0", style = MaterialTheme.typography.titleMedium.copy(textAlign = TextAlign.End), color = MaterialTheme.colorScheme.outline)
                    inner()
                }
                Text(" $suffix", style = TextStyle(fontSize = MaterialTheme.typography.labelMedium.fontSize), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
    )
}
