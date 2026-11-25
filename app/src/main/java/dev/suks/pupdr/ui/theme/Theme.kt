package dev.suks.pupdr.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val PupOrange = Color(0xFFFF7A3D)
val PupCoral = Color(0xFFFF5E5B)
val PupPink = Color(0xFFD6336C)
val PupInk = Color(0xFF1E1B2E)
val PupCream = Color(0xFFFFF4E6)

private val Dark = darkColorScheme(
    primary = PupOrange,
    onPrimary = PupInk,
    primaryContainer = Color(0xFF5A2A12),
    onPrimaryContainer = Color(0xFFFFDBCB),
    secondary = Color(0xFFFF8FA3),
    onSecondary = PupInk,
    secondaryContainer = Color(0xFF5C1B33),
    onSecondaryContainer = Color(0xFFFFD9E0),
    tertiary = Color(0xFF8BD3C7),
    background = Color(0xFF15131F),
    onBackground = Color(0xFFEDE6F2),
    surface = Color(0xFF15131F),
    onSurface = Color(0xFFEDE6F2),
    surfaceVariant = Color(0xFF2A2638),
    onSurfaceVariant = Color(0xFFC9C1D6),
    surfaceContainer = Color(0xFF1F1C2B),
    surfaceContainerHigh = Color(0xFF29253A),
    surfaceContainerHighest = Color(0xFF332E45),
    surfaceContainerLow = Color(0xFF1A1825),
    outline = Color(0xFF6E6680),
    outlineVariant = Color(0xFF3B3650),
)

private val Light = lightColorScheme(
    primary = Color(0xFFD9541A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBCB),
    onPrimaryContainer = Color(0xFF3A1400),
    secondary = PupPink,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFD9E0),
    onSecondaryContainer = Color(0xFF3F0019),
    tertiary = Color(0xFF00796B),
    background = Color(0xFFFFFBF8),
    onBackground = PupInk,
    surface = Color(0xFFFFFBF8),
    onSurface = PupInk,
    surfaceVariant = Color(0xFFF3E7E0),
    onSurfaceVariant = Color(0xFF52443D),
    surfaceContainer = Color(0xFFF8EEE8),
    surfaceContainerHigh = Color(0xFFF2E6DF),
    surfaceContainerHighest = Color(0xFFECDFD8),
    surfaceContainerLow = Color(0xFFFCF4EF),
)

private val Type = Typography().let {
    it.copy(
        headlineMedium = it.headlineMedium.copy(fontWeight = FontWeight.Black),
        headlineSmall = it.headlineSmall.copy(fontWeight = FontWeight.Bold),
        titleLarge = it.titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = it.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 0.2.sp),
    )
}

@Composable
fun PupdrTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) Dark else Light, typography = Type, content = content)
}
