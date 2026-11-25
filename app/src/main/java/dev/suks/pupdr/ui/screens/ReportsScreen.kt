package dev.suks.pupdr.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.suks.pupdr.domain.DateRange
import dev.suks.pupdr.domain.RangePreset
import dev.suks.pupdr.domain.Report
import dev.suks.pupdr.domain.ReportBuilder
import dev.suks.pupdr.ui.Exporter
import dev.suks.pupdr.ui.ReportsViewModel
import dev.suks.pupdr.ui.components.StatTile
import dev.suks.pupdr.ui.components.titleCase
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReportsScreen(vm: ReportsViewModel) {
    val preset by vm.preset.collectAsState()
    val range by vm.range.collectAsState()
    val report by vm.report.collectAsState()
    val unit by vm.unit.collectAsState()
    val context = LocalContext.current
    var pickingRange by remember { mutableStateOf(false) }
    val baseName = "pupdr-report-${range.from}_${range.to}"

    val saveCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        val r = report
        if (uri != null && r != null) Exporter.writeTo(context, uri, ReportBuilder.toCsv(r))
    }
    val saveTxt = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        val r = report
        if (uri != null && r != null) Exporter.writeTo(context, uri, ReportBuilder.toText(r))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reports") },
                actions = { TextButton(onClick = vm::toggleUnit) { Text(unit.uppercase()) } },
            )
        },
    ) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RangePreset.entries.forEach { p ->
                        FilterChip(
                            selected = preset == p,
                            onClick = { if (p == RangePreset.CUSTOM) pickingRange = true else vm.select(p) },
                            label = { Text(p.label) },
                        )
                    }
                }
            }
            item { Text(range.label(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) }
            val r = report
            if (r != null) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatTile("days trained", "${r.daysTrained}/${r.range.days}", Modifier.weight(1f), highlight = true)
                            StatTile("sets", "${r.totalSets}", Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatTile("reps", "${r.totalReps}", Modifier.weight(1f))
                            StatTile("volume $unit", ReportBuilder.fmt(r.totalVolume), Modifier.weight(1f))
                        }
                    }
                }
                item {
                    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Export this report", style = MaterialTheme.typography.titleMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(onClick = { Exporter.share(context, "$baseName.csv", "text/csv", ReportBuilder.toCsv(r)) }, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.TableChart, null); Spacer(Modifier.width(6.dp)); Text("Share CSV")
                                }
                                FilledTonalButton(onClick = { Exporter.share(context, "$baseName.txt", "text/plain", ReportBuilder.toText(r)) }, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.Description, null); Spacer(Modifier.width(6.dp)); Text("Share text")
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(onClick = { saveCsv.launch("$baseName.csv") }, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.Save, null); Spacer(Modifier.width(6.dp)); Text("Save CSV")
                                }
                                OutlinedButton(onClick = { saveTxt.launch("$baseName.txt") }, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.Save, null); Spacer(Modifier.width(6.dp)); Text("Save text")
                                }
                            }
                        }
                    }
                }
                if (r.byDay.isNotEmpty()) item { VolumeChart(r) }
                if (r.byMuscle.isNotEmpty()) item { MuscleBars(r) }
                if (r.byExercise.isNotEmpty()) {
                    item { Text("Exercises", style = MaterialTheme.typography.titleMedium) }
                    items(r.byExercise) { e ->
                        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                            Column(Modifier.fillMaxWidth().padding(14.dp)) {
                                Text(e.name, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "${e.sets} sets · ${e.reps} reps · ${ReportBuilder.fmt(e.volume)} $unit\nbest ${ReportBuilder.fmt(e.bestWeight)} $unit · est. 1RM ${ReportBuilder.fmt(e.bestE1rm)} $unit",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                } else {
                    item {
                        Text("No sets logged in this period yet. Log a workout and it'll show up here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
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
                        val s = state.selectedStartDateMillis
                        if (s != null) {
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
private fun VolumeChart(r: Report) {
    val primary = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    // show at most the last 31 days as bars so it stays readable
    val days = if (r.range.days <= 31) (0 until r.range.days).map { r.range.from.plusDays(it) } else r.byDay.takeLast(31).map { it.date }
    val byDate = r.byDay.associateBy { it.date }
    val useSets = r.byDay.all { it.volume == 0.0 }
    val maxVal = if (useSets) (r.byDay.maxOfOrNull { it.sets } ?: 1).toDouble() else r.byDay.maxOf { it.volume }.coerceAtLeast(1.0)
    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp)) {
            Text(if (useSets) "Sets per day" else "Volume per day (${r.unit})", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            Canvas(Modifier.fillMaxWidth().height(120.dp)) {
                val n = days.size.coerceAtLeast(1)
                val gap = 4.dp.toPx()
                val w = ((size.width - gap * (n - 1)) / n).coerceAtLeast(2f)
                days.forEachIndexed { i, d ->
                    val v = byDate[d]?.let { if (useSets) it.sets.toDouble() else it.volume } ?: 0.0
                    val x = i * (w + gap)
                    drawRoundRect(track, Offset(x, 0f), Size(w, size.height), CornerRadius(w / 2, w / 2))
                    val h = (v / maxVal * size.height).toFloat()
                    if (h > 0) drawRoundRect(primary, Offset(x, size.height - h), Size(w, h), CornerRadius(w / 2, w / 2))
                }
            }
            Spacer(Modifier.height(6.dp))
            val fmt = DateTimeFormatter.ofPattern("d MMM")
            Row(Modifier.fillMaxWidth()) {
                Text(days.first().format(fmt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                Text(days.last().format(fmt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun MuscleBars(r: Report) {
    val top = r.byMuscle.take(8)
    val max = top.first().second.toFloat()
    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Sets per muscle", style = MaterialTheme.typography.titleMedium)
            top.forEach { (m, c) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(m.titleCase(), Modifier.width(110.dp), style = MaterialTheme.typography.bodySmall)
                    Box(Modifier.weight(1f).height(12.dp).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
                        Box(Modifier.fillMaxWidth(c / max).height(12.dp).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.secondary))
                    }
                    Text("$c", Modifier.width(36.dp).padding(start = 8.dp), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
