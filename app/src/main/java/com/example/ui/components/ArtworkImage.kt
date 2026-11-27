package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import java.io.File
import java.util.Calendar

@Composable
fun TrackArtwork(
    artworkPath: String?,
    title: String,
    seedColor: Long? = null,
    fallbackIndex: Int = 0,
    shape: Shape = MaterialTheme.shapes.medium,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val fallbackRes = remember(title, fallbackIndex) {
        val hash = (title.hashCode().and(0x7fffffff) + fallbackIndex) % 2
        if (hash == 0) R.drawable.img_art_vinyl_abstract_1795773855441
        else R.drawable.img_art_synth_wave_1795773854270
    }

    val startColor = seedColor?.let { Color(it.toInt()) } ?: MaterialTheme.colorScheme.primaryContainer
    val endColor = MaterialTheme.colorScheme.secondaryContainer
    val gradientBrush = remember(startColor, endColor) {
        Brush.linearGradient(listOf(startColor, endColor))
    }

    // Coil decodes off the main thread and automatically downsamples to the exact rendered view size
    val imageRequest = remember(context, artworkPath, fallbackRes) {
        val dataModel: Any = if (!artworkPath.isNullOrBlank()) File(artworkPath) else fallbackRes
        ImageRequest.Builder(context)
            .data(dataModel)
            .error(fallbackRes)
            .fallback(fallbackRes)
            .crossfade(150)
            .build()
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(gradientBrush)
    ) {
        AsyncImage(
            model = imageRequest,
            contentDescription = "$title artwork",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

fun formatDurationMs(durationMs: Long): String {
    val totalSec = durationMs.coerceAtLeast(0L) / 1000L
    val hrs = totalSec / 3600L
    val mins = (totalSec % 3600L) / 60L
    val secs = totalSec % 60L
    return buildString(8) {
        if (hrs > 0L) {
            append(hrs)
            append(':')
            if (mins < 10L) append('0')
        }
        append(mins)
        append(':')
        if (secs < 10L) append('0')
        append(secs)
    }
}

fun timeOfDayGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Late night listening"
    }
}
