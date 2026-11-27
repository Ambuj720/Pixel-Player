package com.example.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.data.model.ColorPreset
import com.example.data.model.ThemeMode

val LocalReducedMotion = staticCompositionLocalOf { false }

@Composable
fun CadenceTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    colorPreset: ColorPreset = ColorPreset.DYNAMIC_ARTWORK,
    dynamicAlbumColorsEnabled: Boolean = true,
    artworkSeedColor: Long? = null,
    reducedMotion: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val activeSeed = remember(colorPreset, dynamicAlbumColorsEnabled, artworkSeedColor) {
        if (dynamicAlbumColorsEnabled && colorPreset == ColorPreset.DYNAMIC_ARTWORK && artworkSeedColor != null) {
            artworkSeedColor
        } else {
            colorPreset.seedArgb
        }
    }

    val targetScheme = remember(activeSeed, darkTheme) {
        DynamicColorExtractor.generateColorScheme(activeSeed, darkTheme)
    }

    val animatedScheme = animateColorScheme(targetScheme, reducedMotion)

    CompositionLocalProvider(LocalReducedMotion provides reducedMotion) {
        MaterialTheme(
            colorScheme = animatedScheme,
            typography = CadenceTypography,
            shapes = CadenceShapes,
            content = content
        )
    }
}

@Composable
private fun animateColorScheme(target: ColorScheme, reducedMotion: Boolean): ColorScheme {
    val spec = remember(reducedMotion) {
        if (reducedMotion) snap<Color>()
        else spring<Color>(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioNoBouncy)
    }

    @Composable
    fun anim(c: Color): Color {
        val state by animateColorAsState(targetValue = c, animationSpec = spec, label = "theme_color")
        return state
    }

    return target.copy(
        primary = anim(target.primary),
        onPrimary = anim(target.onPrimary),
        primaryContainer = anim(target.primaryContainer),
        onPrimaryContainer = anim(target.onPrimaryContainer),
        secondary = anim(target.secondary),
        onSecondary = anim(target.onSecondary),
        secondaryContainer = anim(target.secondaryContainer),
        onSecondaryContainer = anim(target.onSecondaryContainer),
        tertiary = anim(target.tertiary),
        onTertiary = anim(target.onTertiary),
        tertiaryContainer = anim(target.tertiaryContainer),
        onTertiaryContainer = anim(target.onTertiaryContainer),
        background = anim(target.background),
        onBackground = anim(target.onBackground),
        surface = anim(target.surface),
        onSurface = anim(target.onSurface),
        surfaceVariant = anim(target.surfaceVariant),
        onSurfaceVariant = anim(target.onSurfaceVariant),
        surfaceContainerLowest = anim(target.surfaceContainerLowest),
        surfaceContainerLow = anim(target.surfaceContainerLow),
        surfaceContainer = anim(target.surfaceContainer),
        surfaceContainerHigh = anim(target.surfaceContainerHigh),
        surfaceContainerHighest = anim(target.surfaceContainerHighest),
        outline = anim(target.outline),
        outlineVariant = anim(target.outlineVariant)
    )
}
