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
import androidx.compose.ui.unit.sp

// monochrome "ink" scheme: black and white with greys for depth (dynamic wallpaper colour is optional)
private val Light = lightColorScheme(
    primary = Color(0xFF000000),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE8E8E8),
    onPrimaryContainer = Color(0xFF000000),
    inversePrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF3A3A3A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEDEDED),
    onSecondaryContainer = Color(0xFF111111),
    tertiary = Color(0xFF5E5E5E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE2E2E2),
    onTertiaryContainer = Color(0xFF111111),
    error = Color(0xFFB3261E),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF0A0A0A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0A0A0A),
    surfaceVariant = Color(0xFFEDEDED),
    onSurfaceVariant = Color(0xFF5C5C5C),
    surfaceTint = Color(0xFF000000),
    outline = Color(0xFF8A8A8A),
    outlineVariant = Color(0xFFDADADA),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F7F7),
    surfaceContainer = Color(0xFFF2F2F2),
    surfaceContainerHigh = Color(0xFFECECEC),
    surfaceContainerHighest = Color(0xFFE5E5E5),
    inverseSurface = Color(0xFF111111),
    inverseOnSurface = Color(0xFFF2F2F2),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFFFFFFF),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF2B2B2B),
    onPrimaryContainer = Color(0xFFFFFFFF),
    inversePrimary = Color(0xFF000000),
    secondary = Color(0xFFD6D6D6),
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF242424),
    onSecondaryContainer = Color(0xFFF0F0F0),
    tertiary = Color(0xFFB0B0B0),
    onTertiary = Color(0xFF000000),
    tertiaryContainer = Color(0xFF303030),
    onTertiaryContainer = Color(0xFFF0F0F0),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF000000),
    onBackground = Color(0xFFF5F5F5),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFF5F5F5),
    surfaceVariant = Color(0xFF1F1F1F),
    onSurfaceVariant = Color(0xFFA3A3A3),
    surfaceTint = Color(0xFFFFFFFF),
    outline = Color(0xFF6B6B6B),
    outlineVariant = Color(0xFF2A2A2A),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF0E0E0E),
    surfaceContainer = Color(0xFF141414),
    surfaceContainerHigh = Color(0xFF1C1C1C),
    surfaceContainerHighest = Color(0xFF262626),
    inverseSurface = Color(0xFFF2F2F2),
    inverseOnSurface = Color(0xFF111111),
)

private val Type = Typography().let {
    it.copy(
        displaySmall = it.displaySmall.copy(fontWeight = FontWeight.SemiBold),
        headlineLarge = it.headlineLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
        headlineMedium = it.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.25).sp),
        headlineSmall = it.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = it.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = it.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    )
}

@Composable
fun PupdrTheme(dynamic: Boolean = false, dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val scheme = when {
        dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(LocalContext.current) else dynamicLightColorScheme(LocalContext.current)
        dark -> Dark
        else -> Light
    }
    MaterialTheme(colorScheme = scheme, typography = Type, content = content)
}
