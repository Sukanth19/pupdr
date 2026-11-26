package dev.suks.pupdr.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.suks.pupdr.domain.ReportBuilder
import dev.suks.pupdr.ui.components.AnimatedNumber
import dev.suks.pupdr.ui.theme.Motion

@Composable
fun DailyRow(
    water: Int,
    waterGoal: Int,
    creatine: Boolean,
    protein: Double,
    proteinGoal: Int,
    onWater: (Int) -> Unit,
    onCreatine: (Boolean) -> Unit,
    onNutrition: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        RingTile(
            icon = Icons.Outlined.WaterDrop,
            value = "$water",
            suffix = "/$waterGoal",
            label = "Water · tap +1",
            progress = water.toFloat() / waterGoal,
            onClick = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); onWater(1) },
            onLongClick = { if (water > 0) onWater(-1) },
            modifier = Modifier.weight(1f),
        )
        CreatineTile(creatine, { haptics.performHapticFeedback(if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff); onCreatine(it) }, Modifier.weight(1f))
        RingTile(
            icon = Icons.Outlined.Restaurant,
            value = ReportBuilder.fmt(protein),
            suffix = "/${proteinGoal}g",
            label = "Protein",
            progress = (protein / proteinGoal).toFloat(),
            onClick = onNutrition,
            onLongClick = onNutrition,
            modifier = Modifier.weight(1f),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RingTile(
    icon: ImageVector,
    value: String,
    suffix: String,
    label: String,
    progress: Float,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier,
) {
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), Motion.standard(Motion.LONG), label = "ring")
    Card(modifier, shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick).padding(14.dp)) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { p },
                    modifier = Modifier.size(40.dp),
                    strokeWidth = 3.dp,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    strokeCap = StrokeCap.Round,
                    gapSize = 0.dp,
                )
                Icon(icon, null, Modifier.size(18.dp))
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                AnimatedNumber(value, MaterialTheme.typography.titleLarge)
                Text(suffix, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 3.dp), maxLines = 1)
            }
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun CreatineTile(taken: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier) {
    val bg by animateColorAsState(if (taken) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer, Motion.standard(), label = "cr-bg")
    val fg by animateColorAsState(if (taken) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, Motion.standard(), label = "cr-fg")
    val scale by animateFloatAsState(if (taken) 1f else 0.85f, Motion.bouncy, label = "cr-scale")
    Card(
        onClick = { onChange(!taken) },
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = bg, contentColor = fg),
    ) {
        Column(Modifier.padding(14.dp)) {
            Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                Icon(if (taken) Icons.Default.Check else Icons.Outlined.Science, null, Modifier.size(22.dp * scale))
            }
            Spacer(Modifier.height(12.dp))
            Text(if (taken) "Taken" else "Not yet", style = MaterialTheme.typography.titleLarge, maxLines = 1)
            Text("Creatine", style = MaterialTheme.typography.labelMedium, color = fg.copy(alpha = 0.7f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsSheet(
    waterGoal: Int, proteinGoal: Int, carbGoal: Int, fatGoal: Int, calorieGoal: Int,
    setWater: (Int) -> Unit, setProtein: (Int) -> Unit, setCarbs: (Int) -> Unit, setFat: (Int) -> Unit, setCalories: (Int) -> Unit,
    veltoInstalled: Boolean, midasInstalled: Boolean,
    syncVelto: Boolean, syncMidas: Boolean,
    setSyncVelto: (Boolean) -> Unit, setSyncMidas: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp).verticalScroll(rememberScrollState())) {
            Text("Daily goals", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))
            Stepper("Water", "$waterGoal glasses", { setWater(waterGoal - 1) }, { setWater(waterGoal + 1) })
            Stepper("Protein", "$proteinGoal g", { setProtein(proteinGoal - 5) }, { setProtein(proteinGoal + 5) })
            Stepper("Carbs", "$carbGoal g", { setCarbs(carbGoal - 10) }, { setCarbs(carbGoal + 10) })
            Stepper("Fat", "$fatGoal g", { setFat(fatGoal - 5) }, { setFat(fatGoal + 5) })
            Stepper("Calories", "$calorieGoal kcal", { setCalories(calorieGoal - 50) }, { setCalories(calorieGoal + 50) })
            Spacer(Modifier.height(24.dp))
            Text("Connected apps", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(4.dp))
            LinkRow("Velto", if (veltoInstalled) "Creatine, water and workouts tick off as habits in both apps" else "Not installed", veltoInstalled, syncVelto, setSyncVelto)
            LinkRow("Midas", if (midasInstalled) "Send supplements to your buy list, see gym spending in reports" else "Not installed", midasInstalled, syncMidas, setSyncMidas)
        }
    }
}

@Composable
private fun Stepper(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        FilledTonalIconButton(onClick = onMinus) { Icon(Icons.Default.Remove, "Less") }
        AnimatedNumber(value, MaterialTheme.typography.titleMedium, Modifier.width(110.dp).padding(horizontal = 8.dp))
        FilledTonalIconButton(onClick = onPlus) { Icon(Icons.Default.Add, "More") }
    }
}

@Composable
private fun LinkRow(name: String, detail: String, installed: Boolean, on: Boolean, set: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(name) },
        supportingContent = { Text(detail) },
        leadingContent = { Icon(Icons.Outlined.Link, null) },
        trailingContent = { Switch(checked = installed && on, onCheckedChange = set, enabled = installed) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    )
}

