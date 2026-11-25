package dev.suks.pupdr.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/** Material 3 motion tokens. */
object Motion {
    val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
    val Standard = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    const val SHORT = 150
    const val MEDIUM = 300
    const val LONG = 450

    fun <T> enter(duration: Int = MEDIUM, delay: Int = 0): FiniteAnimationSpec<T> = tween(duration, delay, EmphasizedDecelerate)
    fun <T> exit(duration: Int = SHORT): FiniteAnimationSpec<T> = tween(duration, 0, EmphasizedAccelerate)
    fun <T> standard(duration: Int = MEDIUM): FiniteAnimationSpec<T> = tween(duration, 0, Standard)

    val offsetSpring = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMediumLow, IntOffset.VisibilityThreshold)
    val sizeSpring = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMediumLow, IntSize.VisibilityThreshold)
    val bouncy = spring<Float>(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)
}
