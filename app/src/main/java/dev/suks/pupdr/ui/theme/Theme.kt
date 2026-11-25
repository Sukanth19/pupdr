package dev.suks.pupdr.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight

// brand fallback (used below Android 12 or when dynamic colour is off): ember orange on deep violet
private val Light = lightColorScheme(
    primary = Color(0xFFA33F12),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBCE),
    onPrimaryContainer = Color(0xFF380D00),
    secondary = Color(0xFF77574B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDBCE),
    onSecondaryContainer = Color(0xFF2C160D),
    tertiary = Color(0xFF8E3A5B),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFD9E3),
    onTertiaryContainer = Color(0xFF3A0719),
    background = Color(0xFFFFF8F6),
    onBackground = Color(0xFF231915),
    surface = Color(0xFFFFF8F6),
    onSurface = Color(0xFF231915),
    surfaceVariant = Color(0xFFF5DED6),
    onSurfaceVariant = Color(0xFF53433E),
    outline = Color(0xFF85736D),
    outlineVariant = Color(0xFFD8C2BA),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFFF1EC),
    surfaceContainer = Color(0xFFFCEAE4),
    surfaceContainerHigh = Color(0xFFF6E4DE),
    surfaceContainerHighest = Color(0xFFF0DFD9),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFFFB59A),
    onPrimary = Color(0xFF5B1B00),
    primaryContainer = Color(0xFF7F2B00),
    onPrimaryContainer = Color(0xFFFFDBCE),
    secondary = Color(0xFFE7BDAF),
    onSecondary = Color(0xFF442A20),
    secondaryContainer = Color(0xFF5D4035),
    onSecondaryContainer = Color(0xFFFFDBCE),
    tertiary = Color(0xFFFFB0C9),
    onTertiary = Color(0xFF55102E),
    tertiaryContainer = Color(0xFF722644),
    onTertiaryContainer = Color(0xFFFFD9E3),
    background = Color(0xFF1A110E),
    onBackground = Color(0xFFF1DFD9),
    surface = Color(0xFF1A110E),
    onSurface = Color(0xFFF1DFD9),
    surfaceVariant = Color(0xFF53433E),
    onSurfaceVariant = Color(0xFFD8C2BA),
    outline = Color(0xFFA08D86),
    outlineVariant = Color(0xFF53433E),
    surfaceContainerLowest = Color(0xFF140C09),
    surfaceContainerLow = Color(0xFF231915),
    surfaceContainer = Color(0xFF271D19),
    surfaceContainerHigh = Color(0xFF322823),
    surfaceContainerHighest = Color(0xFF3D322E),
)

private val Type = Typography().let {
    it.copy(
        displaySmall = it.displaySmall.copy(fontWeight = FontWeight.SemiBold),
        headlineLarge = it.headlineLarge.copy(fontWeight = FontWeight.SemiBold),
        headlineMedium = it.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = it.titleLarge.copy(fontWeight = FontWeight.Medium),
    )
}

@Composable
fun PupdrTheme(dynamic: Boolean = true, dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val scheme = when {
        dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(LocalContext.current) else dynamicLightColorScheme(LocalContext.current)
        dark -> Dark
        else -> Light
    }
    MaterialTheme(colorScheme = scheme, typography = Type, content = content)
}
