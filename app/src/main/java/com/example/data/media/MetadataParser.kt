package com.example.data.media

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.model.LyricLine
import com.example.data.model.Track
import com.example.ui.theme.DynamicColorExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class MetadataParser(private val context: Context) {

    private val artworkDir: File
        get() = File(context.filesDir, "artwork").apply { if (!exists()) mkdirs() }

    suspend fun parseUri(uri: Uri): Track? = withContext(Dispatchers.IO) {
        runCatching {
            // Try persisting read permission for SAF documents
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

            var displayName = uri.lastPathSegment?.substringAfterLast('/') ?: "Unknown Track"
            var fileSize = 0L
            if (uri.scheme == "content") {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIdx >= 0) displayName = cursor.getString(nameIdx) ?: displayName
                        if (sizeIdx >= 0) fileSize = cursor.getLong(sizeIdx)
                    }
                }
            } else if (uri.scheme == "file") {
                val f = File(uri.path ?: "")
                if (f.exists()) {
                    displayName = f.name
                    fileSize = f.length()
                }
            }

            val cleanFallbackTitle = displayName.substringBeforeLast('.')
                .replace('_', ' ')
                .replace('-', ' ')
                .trim()
                .ifBlank { "Untitled Track" }

            val mimeType = context.contentResolver.getType(uri)
                ?: inferMimeFromName(displayName)

            val retriever = MediaMetadataRetriever()
            try {
                if (uri.scheme == "file" && uri.path != null) {
                    retriever.setDataSource(uri.path)
                } else {
                    retriever.setDataSource(context, uri)
                }

                val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                    ?.trim()?.takeIf { it.isNotBlank() } ?: cleanFallbackTitle
                val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    ?.trim()?.takeIf { it.isNotBlank() }
                    ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
                        ?.trim()?.takeIf { it.isNotBlank() }
                    ?: "Unknown Artist"
                val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                    ?.trim()?.takeIf { it.isNotBlank() } ?: "Single"
                val genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
                    ?.trim()?.takeIf { it.isNotBlank() } ?: "Local Audio"
                val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 0L

                val trackId = UUID.randomUUID().toString()
                var artworkPath: String? = null
                var seedColor: Long? = null

                val embeddedBytes = retriever.embeddedPicture
                if (embeddedBytes != null && embeddedBytes.isNotEmpty()) {
                    val bitmap = decodeScaledBitmap(embeddedBytes, 600)
                    if (bitmap != null) {
                        val artFile = File(artworkDir, "art_$trackId.jpg")
                        FileOutputStream(artFile).use { out ->
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
                        }
                        artworkPath = artFile.absolutePath
                        seedColor = DynamicColorExtractor.extractSeedColorArgb(bitmap)
                        bitmap.recycle()
                    }
                }

                Track(
                    id = trackId,
                    uriString = uri.toString(),
                    title = title,
                    artist = artist,
                    album = album,
                    genre = genre,
                    durationMs = durationMs,
                    artworkPath = artworkPath,
                    artworkSeedColor = seedColor,
                    dateAdded = System.currentTimeMillis(),
                    lastPlayedAt = null,
                    playCount = 0,
                    isFavorite = false,
                    lyricsText = null,
                    mimeType = mimeType,
                    fileSizeBytes = fileSize,
                    isAvailable = true
                )
            } finally {
                runCatching { retriever.release() }
            }
        }.getOrNull()
    }

    fun isUriAccessible(uriString: String): Boolean {
        return runCatching {
            val uri = Uri.parse(uriString)
            if (uri.scheme == "file") {
                val path = uri.path ?: return false
                File(path).let { it.exists() && it.canRead() && it.length() > 0 }
            } else {
                context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { true } ?: false
            }
        }.getOrDefault(false)
    }

    private fun decodeScaledBitmap(bytes: ByteArray, maxDim: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sampleSize = 1
        while (bounds.outWidth / sampleSize > maxDim * 2 || bounds.outHeight / sampleSize > maxDim * 2) {
            sampleSize *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
    }

    private fun inferMimeFromName(name: String): String {
        val ext = name.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "m4a", "mp4" -> "audio/mp4"
            "aac" -> "audio/aac"
            "flac" -> "audio/flac"
            "ogg", "oga" -> "audio/ogg"
            "ec3", "eac3" -> "audio/eac3-joc"
            "ac4" -> "audio/ac4"
            "ac3" -> "audio/ac3"
            "mkv", "mka" -> "audio/x-matroska"
            else -> "audio/*"
        }
    }

    companion object {
        private val LRC_LINE_REGEX = Regex("""^\[(\d{1,2}):(\d{2})(?:\.(\d{1,3}))?]\s*(.*)$""")

        fun parseLyrics(raw: String?): List<LyricLine> {
            if (raw.isNullOrBlank()) return emptyList()
            return raw.lines()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .map { line ->
                    val match = LRC_LINE_REGEX.matchEntire(line)
                    if (match != null) {
                        val min = match.groupValues[1].toLongOrNull() ?: 0L
                        val sec = match.groupValues[2].toLongOrNull() ?: 0L
                        val fracStr = match.groupValues[3]
                        val ms = when (fracStr.length) {
                            1 -> (fracStr.toLongOrNull() ?: 0L) * 100L
                            2 -> (fracStr.toLongOrNull() ?: 0L) * 10L
                            3 -> fracStr.toLongOrNull() ?: 0L
                            else -> 0L
                        }
                        val text = match.groupValues[4].ifBlank { "♪" }
                        LyricLine(timestampMs = min * 60_000L + sec * 1_000L + ms, text = text)
                    } else {
                        LyricLine(timestampMs = null, text = line)
                    }
                }
        }
    }
}
