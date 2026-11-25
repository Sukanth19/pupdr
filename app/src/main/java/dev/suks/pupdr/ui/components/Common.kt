package dev.suks.pupdr.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Hardware
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.SportsGymnastics
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.suks.pupdr.R
import dev.suks.pupdr.ui.theme.Motion

private val LogoGradient = Brush.linearGradient(listOf(Color.Black, Color.Black))

@Composable
fun PupLogo(size: Dp = 36.dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size).clip(CircleShape).background(LogoGradient), contentAlignment = Alignment.Center) {
        Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = null, modifier = Modifier.size(size * 1.5f))
    }
}

/** Material 3 style search field (pill shaped, no underline). */
@Composable
fun SearchField(value: String, onChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    TextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Default.Search, null) },
        trailingIcon = { if (value.isNotEmpty()) IconButton(onClick = { onChange("") }) { Icon(Icons.Default.Close, "Clear") } },
        singleLine = true,
        shape = CircleShape,
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    )
}

@Composable
fun EmptyState(icon: ImageVector, title: String, body: String, modifier: Modifier = Modifier, actions: @Composable () -> Unit = {}) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(96.dp).clip(RoundedCornerShape(32.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(44.dp), tint = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.height(24.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        actions()
    }
}

/** Text that rolls up/down when its value changes. */
@Composable
fun AnimatedNumber(value: String, style: TextStyle, modifier: Modifier = Modifier, color: Color = Color.Unspecified) {
    AnimatedContent(
        targetState = value,
        transitionSpec = {
            val up = (targetState.filter(Char::isDigit).toLongOrNull() ?: 0) >= (initialState.filter(Char::isDigit).toLongOrNull() ?: 0)
            (slideInVertically(Motion.enter()) { if (up) it else -it } + fadeIn(Motion.enter()))
                .togetherWith(slideOutVertically(Motion.exit()) { if (up) -it else it } + fadeOut(Motion.exit()))
        },
        label = "number",
        modifier = modifier,
    ) { Text(it, style = style, color = color) }
}

/** Round monochrome avatar with initials, used for routines. */
@Composable
fun ExerciseAvatar(name: String, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    val initials = name.split(' ', '-').filter { it.firstOrNull()?.isLetter() == true }.take(2).joinToString("") { it.first().uppercase() }
    Box(modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest), contentAlignment = Alignment.Center) {
        Text(initials.ifEmpty { "•" }, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

fun categoryIcon(category: String): ImageVector = when (category.lowercase()) {
    "stretching" -> Icons.Outlined.SelfImprovement
    "cardio" -> Icons.AutoMirrored.Outlined.DirectionsRun
    "plyometrics" -> Icons.Outlined.SportsGymnastics
    "olympic weightlifting" -> Icons.Outlined.EmojiEvents
    "strongman" -> Icons.Outlined.Hardware
    "powerlifting" -> Icons.Outlined.FitnessCenter
    else -> Icons.Outlined.FitnessCenter
}

/** Rounded-square icon tile showing the exercise's category. */
@Composable
fun ExerciseIcon(category: String, modifier: Modifier = Modifier, size: Dp = 44.dp, custom: Boolean = false) {
    Box(
        modifier.size(size).clip(RoundedCornerShape(size * 0.32f)).background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (custom) Icons.Outlined.Edit else categoryIcon(category),
            contentDescription = null,
            modifier = Modifier.size(size * 0.5f),
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}

fun String.titleCase(): String = split(' ').joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }

fun musclesLabel(csv: String): String = csv.split(',').filter { it.isNotBlank() }.joinToString(", ") { it.titleCase() }
