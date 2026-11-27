package com.example.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.net.Uri
import com.example.data.model.Track
import com.example.ui.theme.DynamicColorExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Generates genuine, multi-voice local PCM WAV audio files directly into app-private
 * storage along with editorial cover artwork and synced LRC lyrics, allowing immediate
 * real ExoPlayer playback and 5-band Equalizer testing on devices without local MP3s.
 */
class SampleAudioGenerator(private val context: Context) {

    private data class SynthSpec(
        val fileName: String,
        val title: String,
        val artist: String,
        val album: String,
        val genre: String,
        val bpm: Double,
        val durationSec: Int,
        val chordRootsHz: List<Double>,
        val scaleRatios: List<Double>,
        val paletteTop: Int,
        val paletteBottom: Int,
        val accentColor: Int,
        val lyrics: String
    )

    private val specs = listOf(
        SynthSpec(
            fileName = "solstice_horizon.wav",
            title = "Solstice Horizon",
            artist = "Velvet Telemetry",
            album = "Warm Analogues",
            genre = "Ambient Electronic",
            bpm = 88.0,
            durationSec = 26,
            chordRootsHz = listOf(220.0, 174.61, 261.63, 196.0),
            scaleRatios = listOf(1.0, 1.2, 1.5, 1.8, 2.0),
            paletteTop = 0xFFD95D39.toInt(),
            paletteBottom = 0xFF1A103D.toInt(),
            accentColor = 0xFFFFB580.toInt(),
            lyrics = """
                [00:00.50] Warm tape humming under amber skies
                [00:05.00] Soft harmonics drifting where the daylight lies
                [00:10.50] Echoes folding gently into velvet air
                [00:16.00] Every quiet cadence finds its rhythm there
                [00:21.50] Fading slowly through the evening glow
            """.trimIndent()
        ),
        SynthSpec(
            fileName = "emerald_rain_session.wav",
            title = "Emerald Rain Session",
            artist = "Kora & Loom",
            album = "Botanical Beats",
            genre = "Lo-Fi Chill",
            bpm = 96.0,
            durationSec = 24,
            chordRootsHz = listOf(196.0, 164.81, 220.0, 146.83),
            scaleRatios = listOf(1.0, 1.25, 1.5, 1.875, 2.25),
            paletteTop = 0xFF0E8A6D.toInt(),
            paletteBottom = 0xFF0B2A26.toInt(),
            accentColor = 0xFF7CF4C6.toInt(),
            lyrics = """
                [00:00.50] Glass drops tapping on the studio pane
                [00:05.20] Warm chords blooming in the midnight rain
                [00:10.40] Low-end steady like a heartbeat slow
                [00:15.80] Emerald reflections in the cedar glow
                [00:20.20] Drift away and let the cadence flow
            """.trimIndent()
        ),
        SynthSpec(
            fileName = "neon_glass_arpeggio.wav",
            title = "Neon Glass Arpeggio",
            artist = "Prism District",
            album = "Chromatic Pulse",
            genre = "Synthwave",
            bpm = 112.0,
            durationSec = 24,
            chordRootsHz = listOf(246.94, 196.0, 293.66, 220.0),
            scaleRatios = listOf(1.0, 1.189, 1.498, 1.782, 2.0),
            paletteTop = 0xFF5B4BDB.toInt(),
            paletteBottom = 0xFF1D0F38.toInt(),
            accentColor = 0xFFFF79B0.toInt(),
            lyrics = """
                [00:00.40] Silver signals racing down the avenue
                [00:04.80] Violet shimmer painting everything in view
                [00:09.60] Rising pulse beneath a crystal soundscape bright
                [00:14.80] Chasing melodies across the city light
                [00:19.60] Holding steady till the morning breaks
            """.trimIndent()
        ),
        SynthSpec(
            fileName = "rose_quartz_nocturne.wav",
            title = "Rose Quartz Nocturne",
            artist = "Velvet Telemetry",
            album = "Warm Analogues",
            genre = "Neo-Classical",
            bpm = 76.0,
            durationSec = 26,
            chordRootsHz = listOf(261.63, 220.0, 174.61, 196.0),
            scaleRatios = listOf(1.0, 1.25, 1.5, 2.0, 2.5),
            paletteTop = 0xFFC83E74.toInt(),
            paletteBottom = 0xFF2E1128.toInt(),
            accentColor = 0xFFFFC1D6.toInt(),
            lyrics = """
                [00:00.50] Quiet strings awaken in a pastel room
                [00:06.00] Gentle overtones dispel the midnight gloom
                [00:12.00] Tender harmonies unravel bar by bar
                [00:18.50] Glowing softly like a distant winter star
            """.trimIndent()
        )
    )

    suspend fun generateLocalSamples(): List<Track> = withContext(Dispatchers.IO) {
        val audioDir = File(context.filesDir, "sample_audio").apply { if (!exists()) mkdirs() }
        val artDir = File(context.filesDir, "artwork").apply { if (!exists()) mkdirs() }

        specs.mapIndexed { idx, spec ->
            val wavFile = File(audioDir, spec.fileName)
            if (!wavFile.exists() || wavFile.length() < 1000L) {
                synthesizeWavFile(wavFile, spec)
            }
            val artFile = File(artDir, "sample_art_${idx}.jpg")
            val bitmap = renderEditorialArt(spec, idx)
            if (!artFile.exists()) {
                FileOutputStream(artFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                }
            }
            val seed = DynamicColorExtractor.extractSeedColorArgb(bitmap)
            bitmap.recycle()

            Track(
                id = "sample_${spec.fileName.substringBeforeLast('.')}",
                uriString = Uri.fromFile(wavFile).toString(),
                title = spec.title,
                artist = spec.artist,
                album = spec.album,
                genre = spec.genre,
                durationMs = spec.durationSec * 1000L,
                artworkPath = artFile.absolutePath,
                artworkSeedColor = seed,
                dateAdded = System.currentTimeMillis() - (idx * 60_000L),
                lastPlayedAt = null,
                playCount = 0,
                isFavorite = idx == 0,
                lyricsText = spec.lyrics,
                mimeType = "audio/wav",
                fileSizeBytes = wavFile.length(),
                isAvailable = true
            )
        }
    }

    private fun synthesizeWavFile(file: File, spec: SynthSpec) {
        val sampleRate = 22050
        val channels = 2
        val bitsPerSample = 16
        val totalSamples = sampleRate * spec.durationSec
        val dataBytes = totalSamples * channels * (bitsPerSample / 8)
        val beatDuration = 60.0 / spec.bpm
        val barDuration = beatDuration * 4.0

        BufferedOutputStream(FileOutputStream(file), 32 * 1024).use { out ->
            // Standard 44-byte RIFF/WAVE header
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
                put("RIFF".toByteArray())
                putInt(36 + dataBytes)
                put("WAVE".toByteArray())
                put("fmt ".toByteArray())
                putInt(16) // PCM chunk size
                putShort(1) // PCM format
                putShort(channels.toShort())
                putInt(sampleRate)
                putInt(sampleRate * channels * (bitsPerSample / 8))
                putShort((channels * (bitsPerSample / 8)).toShort())
                putShort(bitsPerSample.toShort())
                put("data".toByteArray())
                putInt(dataBytes)
            }
            out.write(header.array())

            val chunkSamples = 2048
            val buf = ByteBuffer.allocate(chunkSamples * 4).order(ByteOrder.LITTLE_ENDIAN)
            var sampleIdx = 0
            while (sampleIdx < totalSamples) {
                buf.clear()
                val count = minOf(chunkSamples, totalSamples - sampleIdx)
                for (k in 0 until count) {
                    val i = sampleIdx + k
                    val t = i.toDouble() / sampleRate.toDouble()
                    val barIdx = ((t / barDuration).toInt()) % spec.chordRootsHz.size
                    val root = spec.chordRootsHz[barIdx]
                    val barPos = t % barDuration
                    val beatPos = t % beatDuration
                    val eighthPos = t % (beatDuration / 2.0)
                    val eighthIndex = ((t / (beatDuration / 2.0)).toInt()) % spec.scaleRatios.size

                    // Global fade in / out envelope
                    val fadeIn = (t / 0.8).coerceIn(0.0, 1.0)
                    val fadeOut = ((spec.durationSec - t) / 1.5).coerceIn(0.0, 1.0)
                    val masterEnv = fadeIn * fadeOut

                    // Warm pad chord (root + third/fifth + octave shimmer)
                    val padEnv = 0.55 + 0.45 * sin(PI * (barPos / barDuration))
                    val padL = (
                        sin(2.0 * PI * root * t) * 0.24 +
                            sin(2.0 * PI * root * spec.scaleRatios[1] * t) * 0.16 +
                            sin(2.0 * PI * root * spec.scaleRatios[2] * t) * 0.14
                        ) * padEnv
                    val padR = (
                        sin(2.0 * PI * (root * 1.003) * t) * 0.24 +
                            sin(2.0 * PI * (root * spec.scaleRatios[1] * 0.998) * t) * 0.16 +
                            sin(2.0 * PI * (root * spec.scaleRatios[2] * 1.002) * t) * 0.14
                        ) * padEnv

                    // Plucky melodic arpeggio voice
                    val arpFreq = root * 2.0 * spec.scaleRatios[eighthIndex]
                    val arpEnv = exp(-eighthPos * 8.5)
                    val arp = (sin(2.0 * PI * arpFreq * t) + 0.3 * sin(4.0 * PI * arpFreq * t)) * 0.22 * arpEnv

                    // Deep sub bass pulse on beats
                    val bassEnv = exp(-beatPos * 3.8)
                    val bass = sin(2.0 * PI * (root * 0.5) * t) * 0.28 * bassEnv

                    val pan = 0.5 + 0.28 * sin(2.0 * PI * 0.25 * t)
                    val left = ((padL + bass + arp * (1.0 - pan * 0.4)) * masterEnv * 0.72).coerceIn(-0.98, 0.98)
                    val right = ((padR + bass + arp * (0.6 + pan * 0.4)) * masterEnv * 0.72).coerceIn(-0.98, 0.98)

                    buf.putShort((left * Short.MAX_VALUE).toInt().toShort())
                    buf.putShort((right * Short.MAX_VALUE).toInt().toShort())
                }
                out.write(buf.array(), 0, count * 4)
                sampleIdx += count
            }
        }
    }

    private fun renderEditorialArt(spec: SynthSpec, variant: Int): Bitmap {
        val size = 512
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, size.toFloat(), size.toFloat(),
                spec.paletteTop, spec.paletteBottom, Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bgPaint)

        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = spec.accentColor
            alpha = 185
        }
        val softPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            alpha = 42
        }

        // Expressive organic overlapping vinyl/soundwave geometry
        when (variant % 4) {
            0 -> {
                canvas.drawCircle(size * 0.72f, size * 0.30f, size * 0.36f, accentPaint)
                canvas.drawCircle(size * 0.30f, size * 0.78f, size * 0.42f, softPaint)
                accentPaint.style = Paint.Style.STROKE
                accentPaint.strokeWidth = 18f
                canvas.drawCircle(size * 0.5f, size * 0.5f, size * 0.22f, accentPaint)
            }
            1 -> {
                canvas.drawRoundRect(RectF(60f, 120f, 452f, 260f), 90f, 90f, accentPaint)
                canvas.drawRoundRect(RectF(110f, 280f, 460f, 420f), 90f, 90f, softPaint)
                canvas.drawCircle(size * 0.24f, size * 0.36f, 68f, softPaint)
            }
            2 -> {
                for (r in 1..4) {
                    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = if (r % 2 == 0) spec.accentColor else 0xFFFFFFFF.toInt()
                        alpha = 150 - r * 24
                        style = Paint.Style.STROKE
                        strokeWidth = 26f
                    }
                    canvas.drawCircle(size * 0.5f, size * 0.5f, r * 54f, p)
                }
            }
            else -> {
                canvas.drawOval(RectF(-60f, 180f, 420f, 560f), accentPaint)
                canvas.drawCircle(size * 0.74f, size * 0.26f, 118f, softPaint)
                canvas.drawCircle(size * 0.5f, size * 0.5f, 46f, softPaint)
            }
        }
        return bmp
    }
}
