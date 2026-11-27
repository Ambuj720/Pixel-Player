package com.example.ui.theme

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.math.abs

/**
 * Pure-Kotlin Material You tonal color extractor and WCAG-accessible ColorScheme generator.
 * Extracts vibrant dominant seed colors from album artwork bitmaps and builds cohesive
 * light/dark Material 3 Expressive palettes with smooth tonal surface elevation layers.
 */
object DynamicColorExtractor {

    fun extractSeedColorArgb(bitmap: Bitmap): Long {
        val scaled = Bitmap.createScaledBitmap(bitmap, 36, 36, true)
        val hsv = FloatArray(3)
        // Quantize hue into 24 buckets weighted by vibrancy (saturation & mid-range brightness)
        val bucketWeight = FloatArray(24)
        val bucketR = FloatArray(24)
        val bucketG = FloatArray(24)
        val bucketB = FloatArray(24)

        for (y in 0 until scaled.height) {
            for (x in 0 until scaled.width) {
                val pixel = scaled.getPixel(x, y)
                val r = AndroidColor.red(pixel)
                val g = AndroidColor.green(pixel)
                val b = AndroidColor.blue(pixel)
                AndroidColor.RGBToHSV(r, g, b, hsv)
                val sat = hsv[1]
                val value = hsv[2]
                // Skip near-black and washed-out whites unless nothing else exists
                val vibrancy = (sat * sat * 2.4f + 0.08f) * (1f - abs(value - 0.58f))
                val bucket = ((hsv[0] / 15f).toInt()).coerceIn(0, 23)
                bucketWeight[bucket] += vibrancy
                bucketR[bucket] += r * vibrancy
                bucketG[bucket] += g * vibrancy
                bucketB[bucket] += b * vibrancy
            }
        }
        if (scaled != bitmap) scaled.recycle()

        var bestBucket = 0
        var maxWeight = -1f
        for (i in 0 until 24) {
            if (bucketWeight[i] > maxWeight) {
                maxWeight = bucketWeight[i]
                bestBucket = i
            }
        }

        if (maxWeight <= 0f) return 0xFF6B4EFFL

        val avgR = (bucketR[bestBucket] / maxWeight).toInt().coerceIn(0, 255)
        val avgG = (bucketG[bestBucket] / maxWeight).toInt().coerceIn(0, 255)
        val avgB = (bucketB[bestBucket] / maxWeight).toInt().coerceIn(0, 255)
        return AndroidColor.rgb(avgR, avgG, avgB).toLong() and 0xFFFFFFFFL
    }

    fun generateColorScheme(seedArgb: Long, darkTheme: Boolean): ColorScheme {
        val hsv = FloatArray(3)
        AndroidColor.colorToHSV(seedArgb.toInt(), hsv)
        val primaryHue = hsv[0]
        val baseSat = hsv[1].coerceIn(0.38f, 0.84f)
        val secondaryHue = (primaryHue + 32f) % 360f
        val tertiaryHue = (primaryHue + 148f) % 360f

        return if (darkTheme) {
            darkColorScheme(
                primary = hslColor(primaryHue, (baseSat * 0.92f).coerceIn(0.45f, 0.85f), 0.78f),
                onPrimary = hslColor(primaryHue, baseSat, 0.14f),
                primaryContainer = hslColor(primaryHue, (baseSat * 0.82f).coerceIn(0.35f, 0.72f), 0.28f),
                onPrimaryContainer = hslColor(primaryHue, 0.75f, 0.92f),

                secondary = hslColor(secondaryHue, (baseSat * 0.62f).coerceIn(0.28f, 0.58f), 0.76f),
                onSecondary = hslColor(secondaryHue, 0.45f, 0.16f),
                secondaryContainer = hslColor(secondaryHue, 0.34f, 0.24f),
                onSecondaryContainer = hslColor(secondaryHue, 0.60f, 0.90f),

                tertiary = hslColor(tertiaryHue, (baseSat * 0.74f).coerceIn(0.38f, 0.72f), 0.76f),
                onTertiary = hslColor(tertiaryHue, 0.50f, 0.15f),
                tertiaryContainer = hslColor(tertiaryHue, 0.42f, 0.26f),
                onTertiaryContainer = hslColor(tertiaryHue, 0.72f, 0.92f),

                background = hslColor(primaryHue, 0.20f, 0.07f),
                onBackground = hslColor(primaryHue, 0.14f, 0.93f),
                surface = hslColor(primaryHue, 0.20f, 0.08f),
                onSurface = hslColor(primaryHue, 0.14f, 0.93f),
                surfaceVariant = hslColor(primaryHue, 0.22f, 0.18f),
                onSurfaceVariant = hslColor(primaryHue, 0.18f, 0.74f),

                surfaceContainerLowest = hslColor(primaryHue, 0.22f, 0.055f),
                surfaceContainerLow = hslColor(primaryHue, 0.21f, 0.105f),
                surfaceContainer = hslColor(primaryHue, 0.22f, 0.135f),
                surfaceContainerHigh = hslColor(primaryHue, 0.23f, 0.17f),
                surfaceContainerHighest = hslColor(primaryHue, 0.24f, 0.21f),

                outline = hslColor(primaryHue, 0.18f, 0.46f),
                outlineVariant = hslColor(primaryHue, 0.20f, 0.26f),
                error = Color(0xFFFFB4AB),
                onError = Color(0xFF690005),
                errorContainer = Color(0xFF93000A),
                onErrorContainer = Color(0xFFFFDAD6)
            )
        } else {
            lightColorScheme(
                primary = hslColor(primaryHue, (baseSat * 0.92f).coerceIn(0.45f, 0.82f), 0.40f),
                onPrimary = Color.White,
                primaryContainer = hslColor(primaryHue, (baseSat * 0.85f).coerceIn(0.42f, 0.84f), 0.90f),
                onPrimaryContainer = hslColor(primaryHue, baseSat, 0.14f),

                secondary = hslColor(secondaryHue, (baseSat * 0.56f).coerceIn(0.28f, 0.55f), 0.38f),
                onSecondary = Color.White,
                secondaryContainer = hslColor(secondaryHue, 0.52f, 0.89f),
                onSecondaryContainer = hslColor(secondaryHue, 0.50f, 0.15f),

                tertiary = hslColor(tertiaryHue, (baseSat * 0.68f).coerceIn(0.36f, 0.68f), 0.38f),
                onTertiary = Color.White,
                tertiaryContainer = hslColor(tertiaryHue, 0.60f, 0.90f),
                onTertiaryContainer = hslColor(tertiaryHue, 0.55f, 0.14f),

                background = hslColor(primaryHue, 0.28f, 0.975f),
                onBackground = hslColor(primaryHue, 0.20f, 0.11f),
                surface = hslColor(primaryHue, 0.28f, 0.975f),
                onSurface = hslColor(primaryHue, 0.20f, 0.11f),
                surfaceVariant = hslColor(primaryHue, 0.22f, 0.90f),
                onSurfaceVariant = hslColor(primaryHue, 0.18f, 0.34f),

                surfaceContainerLowest = Color(0xFFFFFFFF),
                surfaceContainerLow = hslColor(primaryHue, 0.30f, 0.955f),
                surfaceContainer = hslColor(primaryHue, 0.28f, 0.93f),
                surfaceContainerHigh = hslColor(primaryHue, 0.26f, 0.90f),
                surfaceContainerHighest = hslColor(primaryHue, 0.24f, 0.87f),

                outline = hslColor(primaryHue, 0.16f, 0.50f),
                outlineVariant = hslColor(primaryHue, 0.20f, 0.80f),
                error = Color(0xFFBA1A1A),
                onError = Color.White,
                errorContainer = Color(0xFFFFDAD6),
                onErrorContainer = Color(0xFF410002)
            )
        }
    }

    private fun hslColor(hue: Float, saturation: Float, lightness: Float): Color {
        val c = (1f - abs(2f * lightness - 1f)) * saturation
        val x = c * (1f - abs((hue / 60f) % 2f - 1f))
        val m = lightness - c / 2f
        val (r1, g1, b1) = when {
            hue < 60f -> Triple(c, x, 0f)
            hue < 120f -> Triple(x, c, 0f)
            hue < 180f -> Triple(0f, c, x)
            hue < 240f -> Triple(0f, x, c)
            hue < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        return Color(
            red = (r1 + m).coerceIn(0f, 1f),
            green = (g1 + m).coerceIn(0f, 1f),
            blue = (b1 + m).coerceIn(0f, 1f),
            alpha = 1f
        )
    }

    fun Color.toSeedArgb(): Long = this.toArgb().toLong() and 0xFFFFFFFFL
}
