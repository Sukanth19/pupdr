package dev.suks.pupdr.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.suks.pupdr.domain.DailyStats
import dev.suks.pupdr.domain.DateRange
import dev.suks.pupdr.domain.toText
import dev.suks.pupdr.ecosystem.Spend
import dev.suks.pupdr.domain.Granularity
import dev.suks.pupdr.domain.Report
import dev.suks.pupdr.domain.ReportBuilder
import dev.suks.pupdr.ui.Exporter
import dev.suks.pupdr.ui.ReportsViewModel
import dev.suks.pupdr.ui.components.AnimatedNumber
import dev.suks.pupdr.ui.components.EmptyState
import dev.suks.pupdr.ui.components.titleCase
import dev.suks.pupdr.ui.theme.Motion
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(vm: ReportsViewModel) {
    val granularity by vm.granularity.collectAsState()
    val offset by vm.offset.collectAsState()
    val custom by vm.custom.collectAsState()
    val range by vm.range.collectAsState()
    val title by vm.title.collectAsState()
    val report by vm.report.collectAsState()
    val unit by vm.unit.collectAsState()
    val daily by vm.daily.collectAsState()
    val spend by vm.fitnessSpend.collectAsState()
    val context = LocalContext.current
    fun fullText(r: Report) = ReportBuilder.toText(r) + (daily?.takeIf { !it.isEmpty }?.let { "\n" + it.toText() } ?: "") +
        (spend?.let { "\nGym & supplement spending (Midas): ${it.format()}\n" } ?: "")
    var pickingRange by remember { mutableStateOf(false) }
    var exporting by remember { mutableStateOf(false) }
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val baseName = "pupdr-report-${range.from}_${range.to}"

    val saveCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        val r = report
        if (uri != null && r != null) Exporter.writeTo(context, uri, ReportBuilder.toCsv(r))
    }
    val saveTxt = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        val r = report
        if (uri != null && r != null) Exporter.writeTo(context, uri, fullText(r))
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            LargeTopAppBar(
                title = { Text("Reports") },
                actions = {
                    IconButton(onClick = { pickingRange = true }) { Icon(Icons.Default.DateRange, "Custom range") }
                    IconButton(onClick = { exporting = true }, enabled = report != null) { Icon(Icons.Default.IosShare, "Export") }
                },
                scrollBehavior = scroll,
            )
        },
    ) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "segments") {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    Granularity.entries.forEachIndexed { i, g ->
                        SegmentedButton(
                            selected = custom == null && granularity == g,
                            onClick = { vm.select(g) },
                            shape = SegmentedButtonDefaults.itemShape(i, Granularity.entries.size),
                            icon = {},
                        ) { Text(g.label, maxLines = 1) }
                    }
                }
            }
            item(key = "period") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val canNavigate = custom == null && granularity != Granularity.ALL
                    IconButton(onClick = { vm.shift(-1) }, enabled = canNavigate) { Icon(Icons.Default.ChevronLeft, "Previous") }
                    AnimatedContent(
                        targetState = Triple(title, range, offset),
                        transitionSpec = {
                            val forward = targetState.third > initialState.third
                            (slideInHorizontally(Motion.enter()) { if (forward) it / 3 else -it / 3 } + fadeIn(Motion.enter()))
                                .togetherWith(slideOutHorizontally(Motion.exit()) { if (forward) -it / 3 else it / 3 } + fadeOut(Motion.exit()))
                        },
                        modifier = Modifier.weight(1f),
                        label = "period",
                    ) { (t, r, _) ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text(t, style = MaterialTheme.typography.titleLarge)
                            Text(r.label(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    IconButton(onClick = { vm.shift(1) }, enabled = canNavigate && offset < 0) { Icon(Icons.Default.ChevronRight, "Next") }
                }
            }
            val r = report
            if (r != null) {
                item(key = "stats") { StatsGrid(r, unit) }
                val d = daily
                if ((d != null && !d.isEmpty) || spend != null) item(key = "habits") { HabitsCard(d, spend) }
                if (r.totalSets == 0) {
                    item(key = "empty") {
                        EmptyState(Icons.Default.Insights, "No workouts in this period", "Log a few sets and your stats will show up here.", Modifier.animateItem())
                    }
                } else {
                    item(key = "chart") { ActivityChart(r) }
                    if (r.byMuscle.isNotEmpty()) item(key = "muscles") { MuscleBars(r) }
                    item(key = "exercises") { ExerciseTable(r, unit) }
                }
            }
        }
    }

    if (exporting && report != null) {
        ModalBottomSheet(onDismissRequest = { exporting = false }) {
            Column(Modifier.padding(bottom = 24.dp)) {
                Text("Export report", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 24.dp))
                Text("$title · ${range.label()}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp))
                Spacer(Modifier.height(8.dp))
                val r = report!!
                ExportRow(Icons.Default.TableChart, "Share spreadsheet (CSV)", "Every set, ready for Excel or Sheets") {
                    exporting = false; Exporter.share(context, "$baseName.csv", "text/csv", ReportBuilder.toCsv(r))
                }
                ExportRow(Icons.Default.Description, "Share summary (text)", "Totals, muscles, PRs and the full log") {
                    exporting = false; Exporter.share(context, "$baseName.txt", "text/plain", fullText(r))
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                ExportRow(Icons.Default.SaveAlt, "Save CSV to device", null) { exporting = false; saveCsv.launch("$baseName.csv") }
                ExportRow(Icons.Default.SaveAlt, "Save text to device", null) { exporting = false; saveTxt.launch("$baseName.txt") }
            }
        }
    }

    if (pickingRange) {
        val state = rememberDateRangePickerState(
            initialSelectedStartDateMillis = range.from.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
            initialSelectedEndDateMillis = range.to.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { pickingRange = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedStartDateMillis?.let { s ->
                            val from = Instant.ofEpochMilli(s).atZone(ZoneOffset.UTC).toLocalDate()
                            val to = state.selectedEndDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() } ?: from
                            vm.setCustom(DateRange(from, to))
                        }
                        pickingRange = false
                    },
                    enabled = state.selectedStartDateMillis != null,
                ) { Text("Apply") }
            },
            dismissButton = { TextButton(onClick = { pickingRange = false }) { Text("Cancel") } },
        ) { DateRangePicker(state, modifier = Modifier.height(520.dp)) }
    }
}

@Composable
private fun ExportRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String?, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        leadingContent = { Icon(icon, null) },
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    )
}

@Composable
private fun StatsGrid(r: Report, unit: String) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Stat("Days trained", "${r.daysTrained}", "of ${r.range.days}", Modifier.weight(1f), highlight = true)
            Stat("Sets", "${r.totalSets}", null, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Stat("Reps", "${r.totalReps}", null, Modifier.weight(1f))
            Stat("Volume", ReportBuilder.fmt(r.totalVolume), unit, Modifier.weight(1f))
        }
    }
}

@Composable
private fun Stat(label: String, value: String, suffix: String?, modifier: Modifier, highlight: Boolean = false) {
    val bg = if (highlight) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow
    val fg = if (highlight) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    Card(modifier, shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = bg)) {
        Column(Modifier.padding(20.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = fg.copy(alpha = 0.75f))
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                AnimatedNumber(value, MaterialTheme.typography.headlineMedium, color = fg)
                if (suffix != null) Text(" $suffix", style = MaterialTheme.typography.bodyMedium, color = fg.copy(alpha = 0.75f), modifier = Modifier.padding(bottom = 4.dp))
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun ActivityChart(r: Report) {
    val primary = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val days = if (r.range.days <= 31) (0 until r.range.days).map { r.range.from.plusDays(it) } else r.byDay.takeLast(31).map { it.date }
    val byDate = r.byDay.associateBy { it.date }
    val useSets = r.byDay.all { it.volume == 0.0 }
    val maxVal = (if (useSets) r.byDay.maxOfOrNull { it.sets.toDouble() } else r.byDay.maxOfOrNull { it.volume })?.coerceAtLeast(1.0) ?: 1.0
    val grow = remember { Animatable(0f) }
    LaunchedEffect(r.range, r.totalSets, r.totalVolume) {
        grow.snapTo(0f)
        grow.animateTo(1f, tween(Motion.LONG + 150, easing = Motion.EmphasizedDecelerate))
    }
    SectionCard(if (useSets) "Sets per day" else "Volume per day (${r.unit})") {
        Canvas(Modifier.fillMaxWidth().height(140.dp)) {
            val n = days.size.coerceAtLeast(1)
            val gap = if (n > 14) 3.dp.toPx() else 6.dp.toPx()
            val w = ((size.width - gap * (n - 1)) / n).coerceAtLeast(2f)
            val radius = CornerRadius(minOf(w / 2, 8.dp.toPx()))
            days.forEachIndexed { i, d ->
                val v = byDate[d]?.let { if (useSets) it.sets.toDouble() else it.volume } ?: 0.0
                val x = i * (w + gap)
                drawRoundRect(track, Offset(x, 0f), Size(w, size.height), radius)
                val h = (v / maxVal * size.height * grow.value).toFloat()
                if (h > 0) drawRoundRect(primary, Offset(x, size.height - h), Size(w, h), radius)
            }
        }
        Spacer(Modifier.height(8.dp))
        val fmt = DateTimeFormatter.ofPattern("d MMM")
        Row(Modifier.fillMaxWidth()) {
            Text(days.first().format(fmt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            Text(days.last().format(fmt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MuscleBars(r: Report) {
    val top = r.byMuscle.take(8)
    val max = top.first().second.toFloat()
    SectionCard("Sets per muscle") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            top.forEach { (m, c) ->
                val frac by animateFloatAsState(c / max, Motion.standard(Motion.LONG), label = "muscle-$m")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(m.titleCase(), Modifier.width(112.dp), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Box(Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(5.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
                        Box(Modifier.fillMaxWidth(frac).height(10.dp).clip(RoundedCornerShape(5.dp)).background(MaterialTheme.colorScheme.tertiary))
                    }
                    Text("$c", Modifier.width(40.dp), style = MaterialTheme.typography.labelLarge, textAlign = androidx.compose.ui.text.style.TextAlign.End)
                }
            }
        }
    }
}

@Composable
private fun ExerciseTable(r: Report, unit: String) {
    SectionCard("Exercises") {
        r.byExercise.forEachIndexed { i, e ->
            if (i > 0) HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(e.name, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("${e.sets} sets · ${e.reps} reps", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (e.bestWeight > 0) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${ReportBuilder.fmt(e.bestWeight)} $unit", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        Text("e1RM ${ReportBuilder.fmt(e.bestE1rm)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun HabitsCard(d: DailyStats?, spend: Spend?) {
    SectionCard("Habits & nutrition") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (d != null && !d.isEmpty) {
                HabitLine("Creatine", "${d.creatineDays} of ${d.days} days", d.creatineDays.toFloat() / d.days)
                HabitLine("Water goal", "${d.waterGoalDays} of ${d.days} days", d.waterGoalDays.toFloat() / d.days)
                if (d.foodDays > 0) {
                    Row {
                        Text("Protein", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Text("${ReportBuilder.fmt(d.avgProtein)} g/day", style = MaterialTheme.typography.titleSmall)
                    }
                    Row {
                        Text("Carbs", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Text("${ReportBuilder.fmt(d.avgCarbs)} g/day", style = MaterialTheme.typography.titleSmall)
                    }
                    Row {
                        Text("Fat", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Text("${ReportBuilder.fmt(d.avgFat)} g/day", style = MaterialTheme.typography.titleSmall)
                    }
                    Row {
                        Text("Calories", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Text("${ReportBuilder.fmt(d.avgCalories)} kcal/day", style = MaterialTheme.typography.titleSmall)
                    }
                    MacroSplit(d.avgProtein, d.avgCarbs, d.avgFat)
                }
            }
            if (spend != null) {
                Row {
                    Column(Modifier.weight(1f)) {
                        Text("Gym & supplements", style = MaterialTheme.typography.bodyLarge)
                        Text("From Midas", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(spend.format(), style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }
}

@Composable
private fun HabitLine(label: String, value: String, fraction: Float) {
    val p by animateFloatAsState(fraction.coerceIn(0f, 1f), Motion.standard(Motion.LONG), label = "habit-$label")
    Column {
        Row {
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(value, style = MaterialTheme.typography.titleSmall)
        }
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
            Box(Modifier.fillMaxWidth(p).height(8.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.primary))
        }
    }
}

/** Average share of calories from each macro, as one segmented bar with a labelled legend. */
@Composable
private fun MacroSplit(protein: Double, carbs: Double, fat: Double) {
    val colors = macroColors()
    val (p, c, f) = dev.suks.pupdr.domain.Macros(protein, carbs, fat).calorieSplit()
    if (p + c + f <= 0f) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth().height(12.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            listOf(p to colors.protein, c to colors.carbs, f to colors.fat).filter { it.first > 0f }.forEach { (share, col) ->
                Box(Modifier.weight(share).fillMaxSize().clip(RoundedCornerShape(4.dp)).background(col))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            listOf(Triple("Protein", p, colors.protein), Triple("Carbs", c, colors.carbs), Triple("Fat", f, colors.fat)).forEach { (l, share, col) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(col))
                    Spacer(Modifier.width(6.dp))
                    Text("$l ${(share * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
