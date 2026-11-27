package com.example.ui.components

import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
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

    val fileExists = remember(artworkPath) {
        !artworkPath.isNullOrBlank() && File(artworkPath).exists()
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        seedColor?.let { Color(it.toInt()) } ?: MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.secondaryContainer
                    )
                )
            )
    ) {
        if (fileExists && artworkPath != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(File(artworkPath))
                    .crossfade(true)
                    .build(),
                contentDescription = "$title album artwork",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Image(
                painter = painterResource(id = fallbackRes),
                contentDescription = "$title artwork",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

fun formatDurationMs(durationMs: Long): String {
    val totalSec = (durationMs.coerceAtLeast(0L) / 1000L)
    val hrs = totalSec / 3600L
    val mins = (totalSec % 3600L) / 60L
    val secs = totalSec % 60L
    return if (hrs > 0) {
        String.format("%d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format("%d:%02d", mins, secs)
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
