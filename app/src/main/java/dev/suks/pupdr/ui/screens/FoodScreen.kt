package dev.suks.pupdr.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AddShoppingCart
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.suks.pupdr.data.FoodEntry
import dev.suks.pupdr.data.Meal
import dev.suks.pupdr.domain.FoodPreset
import dev.suks.pupdr.domain.FoodPresets
import dev.suks.pupdr.domain.Macros
import dev.suks.pupdr.domain.ReportBuilder
import dev.suks.pupdr.ui.FoodViewModel
import dev.suks.pupdr.ui.components.AnimatedNumber
import dev.suks.pupdr.ui.components.SearchField
import dev.suks.pupdr.ui.theme.Motion
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Macro colours: the first three slots of a validated categorical palette (blue, orange, aqua),
 * checked for colour-blind separation on both surfaces. Always shown with a text label.
 */
data class MacroColors(val protein: Color, val carbs: Color, val fat: Color)

@Composable
fun macroColors(): MacroColors = if (isSystemInDarkTheme())
    MacroColors(Color(0xFF338CED), Color(0xFFE76624), Color(0xFF00A888))
else MacroColors(Color(0xFF257DE0), Color(0xFFF47230), Color(0xFF00B894))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodScreen(vm: FoodViewModel) {
    val day by vm.day.collectAsState()
    val entries by vm.entries.collectAsState()
    val week by vm.week.collectAsState()
    val pGoal by vm.proteinGoal.collectAsState()
    val cGoal by vm.carbGoal.collectAsState()
    val fGoal by vm.fatGoal.collectAsState()
    val kGoal by vm.calorieGoal.collectAsState()
    val midas by vm.midasLinked.collectAsState()
    var adding by remember { mutableStateOf<Meal?>(null) }
    val total = Macros.sum(entries)
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val listState = rememberLazyListState()
    val fabExpanded by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0),
        topBar = { LargeTopAppBar(title = { Text("Food") }, scrollBehavior = scroll) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { adding = defaultMeal() }, expanded = fabExpanded, icon = { Icon(Icons.Default.Add, null) }, text = { Text("Add food") })
        },
    ) { pad ->
        LazyColumn(
            state = listState,
            modifier = Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "day") { DayBar(day, vm::shiftDay) }
            item(key = "today") { TodayCard(total, pGoal, cGoal, fGoal, kGoal) }
            item(key = "week") { WeekChart(week, kGoal, day) }
            Meal.entries.forEach { meal ->
                val list = entries.filter { it.meal == meal.ordinal }
                item(key = "meal-${meal.name}") {
                    MealCard(meal, list, onAdd = { adding = meal }, onDelete = vm::delete, modifier = Modifier.animateItem())
                }
            }
            if (midas) item(key = "midas") {
                Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Column(Modifier.padding(20.dp)) {
                        Text("Running low?", style = MaterialTheme.typography.titleMedium)
                        Text("Add it to your Midas buy list.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Creatine", "Whey protein").forEach { item ->
                                AssistChip(onClick = { vm.addToMidas(item) }, label = { Text(item) }, leadingIcon = { Icon(Icons.Outlined.AddShoppingCart, null, Modifier.size(18.dp)) })
                            }
                        }
                    }
                }
            }
        }
    }

    adding?.let { meal -> AddFoodSheet(meal, onDismiss = { adding = null }) { name, m, chosenMeal -> vm.add(name, m, chosenMeal) } }
}

private fun defaultMeal(): Meal = when (java.time.LocalTime.now().hour) {
    in 4..10 -> Meal.BREAKFAST
    in 11..15 -> Meal.LUNCH
    in 18..22 -> Meal.DINNER
    else -> Meal.SNACK
}

@Composable
private fun DayBar(day: LocalDate, shift: (Long) -> Unit) {
    val today = LocalDate.now()
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { shift(-1) }) { Icon(Icons.Default.ChevronLeft, "Previous day") }
        AnimatedContent(
            targetState = day,
            transitionSpec = {
                val fwd = targetState > initialState
                (slideInHorizontally(Motion.enter()) { if (fwd) it / 4 else -it / 4 } + fadeIn(Motion.enter()))
                    .togetherWith(slideOutHorizontally(Motion.exit()) { if (fwd) -it / 4 else it / 4 } + fadeOut(Motion.exit()))
            },
            modifier = Modifier.weight(1f),
            label = "food-day",
        ) { d ->
            Text(
                when (d) { today -> "Today"; today.minusDays(1) -> "Yesterday"; else -> d.format(DateTimeFormatter.ofPattern("EEEE, d MMM")) },
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
        IconButton(onClick = { shift(1) }, enabled = day < today) { Icon(Icons.Default.ChevronRight, "Next day") }
    }
}

@Composable
private fun TodayCard(total: Macros, pGoal: Int, cGoal: Int, fGoal: Int, kGoal: Int) {
    val colors = macroColors()
    val (sp, sc, sf) = total.calorieSplit()
    val grow = remember { Animatable(0f) }
    LaunchedEffect(Unit) { grow.animateTo(1f, tween(Motion.LONG + 200, easing = Motion.EmphasizedDecelerate)) }
    val ap by animateFloatAsState(sp, Motion.standard(Motion.LONG), label = "sp")
    val ac by animateFloatAsState(sc, Motion.standard(Motion.LONG), label = "sc")
    val af by animateFloatAsState(sf, Motion.standard(Motion.LONG), label = "sf")
    val surface = MaterialTheme.colorScheme.surfaceContainer
    val track = MaterialTheme.colorScheme.surfaceContainerHighest

    Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = surface)) {
        Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            // calorie donut: share of calories from each macro, 2px surface gaps between segments
            Box(Modifier.size(132.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 14.dp.toPx()
                    val inset = stroke / 2
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    val tl = Offset(inset, inset)
                    drawArc(track, 0f, 360f, false, tl, arcSize, style = Stroke(stroke))
                    val sweepTotal = 360f * grow.value
                    val gapDeg = if (ap + ac + af > 0f) 2.dp.toPx() / (arcSize.width / 2) * (180f / Math.PI.toFloat()) else 0f
                    var start = -90f
                    listOf(ap to colors.protein, ac to colors.carbs, af to colors.fat).forEach { (share, c) ->
                        val sweep = share * sweepTotal
                        if (sweep > gapDeg) drawArc(c, start + gapDeg / 2, sweep - gapDeg, false, tl, arcSize, style = Stroke(stroke, cap = StrokeCap.Butt))
                        start += sweep
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AnimatedNumber(ReportBuilder.fmt(total.calories), MaterialTheme.typography.headlineSmall)
                    Text("of $kGoal kcal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.width(20.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MacroBar("Protein", total.protein, pGoal, colors.protein)
                MacroBar("Carbs", total.carbs, cGoal, colors.carbs)
                MacroBar("Fat", total.fat, fGoal, colors.fat)
            }
        }
    }
}

@Composable
private fun MacroBar(label: String, grams: Double, goal: Int, color: Color) {
    val p by animateFloatAsState(if (goal <= 0) 0f else (grams / goal).toFloat().coerceIn(0f, 1f), Motion.standard(Motion.LONG), label = "macro-$label")
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            Text("${ReportBuilder.fmt(grams)}/${goal}g", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
            Box(Modifier.fillMaxWidth(p).height(6.dp).clip(RoundedCornerShape(3.dp)).background(color))
        }
    }
}

/** Calories per day for the last 7 days, stacked by macro, with a dashed goal line. */
@Composable
private fun WeekChart(week: List<Pair<LocalDate, Macros>>, kGoal: Int, selected: LocalDate) {
    if (week.isEmpty()) return
    val colors = macroColors()
    val goalColor = MaterialTheme.colorScheme.outline
    val surface = MaterialTheme.colorScheme.surfaceContainer
    val grow = remember { Animatable(0f) }
    LaunchedEffect(week) { grow.snapTo(0f); grow.animateTo(1f, tween(Motion.LONG + 150, easing = Motion.EmphasizedDecelerate)) }
    val maxKcal = maxOf(kGoal.toDouble(), week.maxOf { (_, m) -> Macros.kcal(m.protein, m.carbs, m.fat) }).coerceAtLeast(1.0) * 1.1
    val avg = week.map { it.second }.filter { it.calories > 0 }.let { l -> if (l.isEmpty()) 0.0 else l.sumOf { it.calories } / l.size }

    Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = surface)) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("Last 7 days", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text("avg ${ReportBuilder.fmt(avg)} kcal", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(16.dp))
            Canvas(Modifier.fillMaxWidth().height(150.dp)) {
                val n = week.size
                val slot = size.width / n
                val barW = (slot * 0.46f).coerceAtMost(28.dp.toPx())
                val gap = 2.dp.toPx()
                week.forEachIndexed { i, (_, m) ->
                    val x = i * slot + (slot - barW) / 2
                    var y = size.height
                    // bottom to top: protein, carbs, fat (kcal); 2px surface gap between segments
                    listOf(m.protein * 4 to colors.protein, m.carbs * 4 to colors.carbs, m.fat * 9 to colors.fat).forEach { (kcal, c) ->
                        val h = (kcal / maxKcal * size.height * grow.value).toFloat()
                        if (h > gap) {
                            drawRoundRect(c, Offset(x, y - h + gap), Size(barW, h - gap), CornerRadius(4.dp.toPx()))
                            y -= h
                        }
                    }
                }
                val gy = (size.height - kGoal / maxKcal * size.height).toFloat()
                drawLine(goalColor, Offset(0f, gy), Offset(size.width, gy), strokeWidth = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)))
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth()) {
                week.forEach { (d, _) ->
                    Text(
                        d.format(DateTimeFormatter.ofPattern("EEEEE")),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (d == selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Legend("Protein", colors.protein); Legend("Carbs", colors.carbs); Legend("Fat", colors.fat)
                Spacer(Modifier.weight(1f))
                Text("- - goal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun Legend(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun MealCard(meal: Meal, list: List<FoodEntry>, onAdd: () -> Unit, onDelete: (Long) -> Unit, modifier: Modifier) {
    val m = Macros.sum(list)
    Card(modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.animateContentSize(Motion.sizeSpring).padding(vertical = 8.dp)) {
            Row(Modifier.padding(start = 20.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(meal.label, style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (list.isEmpty()) "Nothing yet" else "${ReportBuilder.fmt(m.calories)} kcal · P ${ReportBuilder.fmt(m.protein)} · C ${ReportBuilder.fmt(m.carbs)} · F ${ReportBuilder.fmt(m.fat)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onAdd) { Icon(Icons.Default.Add, "Add to ${meal.label}") }
            }
            list.forEach { e ->
                ListItem(
                    headlineContent = { Text(e.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    supportingContent = { Text("P ${ReportBuilder.fmt(e.protein)} · C ${ReportBuilder.fmt(e.carbs)} · F ${ReportBuilder.fmt(e.fat)} g") },
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${e.calories}", style = MaterialTheme.typography.labelLarge)
                            IconButton(onClick = { onDelete(e.id) }) { Icon(Icons.Default.Close, "Remove ${e.name}", Modifier.size(18.dp)) }
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AddFoodSheet(initialMeal: Meal, onDismiss: () -> Unit, onAdd: (String, Macros, Meal) -> Unit) {
    var meal by remember { mutableStateOf(initialMeal) }
    var query by remember { mutableStateOf("") }
    var servings by remember { mutableStateOf(1.0) }
    var name by remember { mutableStateOf("") }
    var p by remember { mutableStateOf("") }
    var c by remember { mutableStateOf("") }
    var f by remember { mutableStateOf("") }
    var k by remember { mutableStateOf("") }
    val presets = remember(query) { FoodPresets.ALL.filter { query.isBlank() || it.name.contains(query, ignoreCase = true) } }
    val autoKcal = Macros.kcal(p.toDoubleOrNull() ?: 0.0, c.toDoubleOrNull() ?: 0.0, f.toDoubleOrNull() ?: 0.0)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp).imePadding().verticalScroll(rememberScrollState())) {
            Text("Add food", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                Meal.entries.forEachIndexed { i, m ->
                    SegmentedButton(selected = meal == m, onClick = { meal = m }, shape = SegmentedButtonDefaults.itemShape(i, Meal.entries.size), icon = {}) {
                        Text(m.label.removeSuffix("s"), maxLines = 1)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            SearchField(query, { query = it }, "Search common foods")
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Servings", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                listOf(0.5, 1.0, 1.5, 2.0).forEach { s ->
                    TextButton(onClick = { servings = s }) {
                        Text("×${ReportBuilder.fmt(s)}", color = if (servings == s) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = if (servings == s) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            presets.take(12).forEach { preset -> PresetRow(preset, servings) { onAdd(preset.name + if (servings != 1.0) " ×${ReportBuilder.fmt(servings)}" else "", preset.macros * servings, meal); onDismiss() } }
            if (presets.isEmpty()) Text("No match. Add it below.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))
            Text("Your own", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(name, { name = it }, label = { Text("Food") }, singleLine = true, shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(Triple("Protein", p) { v: String -> p = v }, Triple("Carbs", c) { v: String -> c = v }, Triple("Fat", f) { v: String -> f = v }).forEach { (l, v, set) ->
                    OutlinedTextField(v, { set(it.filter { ch -> ch.isDigit() || ch == '.' }.take(5)) }, label = { Text("$l g") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(k, { k = it.filter(Char::isDigit).take(5) }, label = { Text("Calories") }, singleLine = true,
                placeholder = { Text(if (autoKcal > 0) "${autoKcal.toInt()} from macros" else "kcal") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    onAdd(name, Macros(p.toDoubleOrNull() ?: 0.0, c.toDoubleOrNull() ?: 0.0, f.toDoubleOrNull() ?: 0.0, (k.toIntOrNull() ?: 0).toDouble()), meal)
                    onDismiss()
                },
                enabled = name.isNotBlank() && (autoKcal > 0 || (k.toIntOrNull() ?: 0) > 0),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Add to ${meal.label.lowercase()}") }
        }
    }
}

@Composable
private fun PresetRow(preset: FoodPreset, servings: Double, onClick: () -> Unit) {
    val m = preset.macros * servings
    ListItem(
        modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick),
        headlineContent = { Text(preset.name) },
        supportingContent = { Text("${preset.serving} · P ${ReportBuilder.fmt(m.protein)} · C ${ReportBuilder.fmt(m.carbs)} · F ${ReportBuilder.fmt(m.fat)}") },
        trailingContent = { Text("${ReportBuilder.fmt(m.calories)} kcal", style = MaterialTheme.typography.labelLarge) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    )
}

