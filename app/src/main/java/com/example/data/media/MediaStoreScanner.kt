package com.example.data.media

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import com.example.data.model.Track
import com.example.ui.theme.DynamicColorExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * High-performance MediaStore scanner for supported local audio formats.
 * Uses O(1) URI deduplication against existing Room tracks and per-album artwork caching
 * so songs sharing an album decode and store only a single downsampled JPEG.
 */
class MediaStoreScanner(private val context: Context) {

    private val artworkDir: File
        get() = File(context.filesDir, "artwork").apply { if (!exists()) mkdirs() }

    suspend fun scanSupportedAudio(existingUris: MutableSet<String>): List<Track> = withContext(Dispatchers.IO) {
        val discovered = ArrayList<Track>()
        val albumArtCache = HashMap<Long, Pair<String?, Long?>>()

        val projection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            PROJECTION_API_30_PLUS
        } else {
            PROJECTION_LEGACY
        }

        // Filter out tiny notification blips / empty placeholders directly in SQLite
        val selection = "${MediaStore.Audio.Media.SIZE} > 0 AND " +
            "(${MediaStore.Audio.Media.IS_MUSIC} != 0 OR ${MediaStore.Audio.Media.DURATION} >= 10000) AND " +
            "${MediaStore.Audio.Media.DURATION} >= 3000"
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        runCatching {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
                val durationCol = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
                val mimeCol = cursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)
                val sizeCol = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)
                val nameCol = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
                val dateCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
                val genreCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    cursor.getColumnIndex(MediaStore.Audio.Media.GENRE)
                } else {
                    -1
                }

                while (cursor.moveToNext()) {
                    val mediaId = cursor.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        mediaId
                    )
                    val uriStr = contentUri.toString()

                    // O(1) skip for already-imported tracks
                    if (uriStr in existingUris) continue

                    val displayName = if (nameCol >= 0) cursor.getString(nameCol).orEmpty() else ""
                    val rawMime = if (mimeCol >= 0) cursor.getString(mimeCol).orEmpty() else ""
                    if (!isSupportedAudio(rawMime, displayName)) continue

                    existingUris.add(uriStr)

                    val fallbackTitle = displayName.substringBeforeLast('.')
                        .replace('_', ' ')
                        .replace('-', ' ')
                        .trim()
                        .ifBlank { "Untitled Track" }

                    val title = (if (titleCol >= 0) cursor.getString(titleCol) else null)
                        ?.trim()
                        ?.takeIf { it.isNotBlank() && !it.equals("<unknown>", ignoreCase = true) }
                        ?: fallbackTitle

                    val artist = (if (artistCol >= 0) cursor.getString(artistCol) else null)
                        ?.trim()
                        ?.takeIf { it.isNotBlank() && !it.equals("<unknown>", ignoreCase = true) }
                        ?: "Unknown Artist"

                    val album = (if (albumCol >= 0) cursor.getString(albumCol) else null)
                        ?.trim()
                        ?.takeIf { it.isNotBlank() && !it.equals("<unknown>", ignoreCase = true) }
                        ?: "Single"

                    val genre = (if (genreCol >= 0) cursor.getString(genreCol) else null)
                        ?.trim()
                        ?.takeIf { it.isNotBlank() && !it.equals("<unknown>", ignoreCase = true) }
                        ?: "Local Audio"

                    val durationMs = if (durationCol >= 0) cursor.getLong(durationCol).coerceAtLeast(0L) else 0L
                    val fileSize = if (sizeCol >= 0) cursor.getLong(sizeCol).coerceAtLeast(0L) else 0L
                    val dateAddedSec = if (dateCol >= 0) cursor.getLong(dateCol) else 0L
                    val dateAddedMs = if (dateAddedSec > 0L) dateAddedSec * 1000L else System.currentTimeMillis()
                    val albumId = if (albumIdCol >= 0) cursor.getLong(albumIdCol) else -1L

                    val (artworkPath, seedColor) = resolveAlbumArtwork(
                        contentUri = contentUri,
                        albumId = albumId,
                        cache = albumArtCache
                    )

                    val resolvedMime = rawMime.ifBlank { inferMimeFromName(displayName) }

                    discovered.add(
                        Track(
                            id = UUID.randomUUID().toString(),
                            uriString = uriStr,
                            title = title,
                            artist = artist,
                            album = album,
                            genre = genre,
                            durationMs = durationMs,
                            artworkPath = artworkPath,
                            artworkSeedColor = seedColor,
                            dateAdded = dateAddedMs,
                            lastPlayedAt = null,
                            playCount = 0,
                            isFavorite = false,
                            lyricsText = null,
                            mimeType = resolvedMime,
                            fileSizeBytes = fileSize,
                            isAvailable = true
                        )
                    )
                }
            }
        }

        discovered
    }

    private fun resolveAlbumArtwork(
        contentUri: Uri,
        albumId: Long,
        cache: HashMap<Long, Pair<String?, Long?>>
    ): Pair<String?, Long?> {
        if (albumId > 0L) {
            cache[albumId]?.let { return it }
            val existingFile = File(artworkDir, "album_$albumId.jpg")
            if (existingFile.exists() && existingFile.length() > 0L) {
                val seed = extractSeedFromFile(existingFile)
                val entry = Pair<String?, Long?>(existingFile.absolutePath, seed)
                cache[albumId] = entry
                return entry
            }
        }

        val bitmap = decodeAlbumThumbnail(contentUri, albumId)
        val entry = if (bitmap != null) {
            val seed = DynamicColorExtractor.extractSeedColorArgb(bitmap)
            val fileName = if (albumId > 0L) "album_$albumId.jpg" else "art_${UUID.randomUUID()}.jpg"
            val artFile = File(artworkDir, fileName)
            val savedPath = runCatching {
                FileOutputStream(artFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
                artFile.absolutePath
            }.getOrNull()
            bitmap.recycle()
            Pair(savedPath, seed)
        } else {
            Pair(null, null)
        }

        if (albumId > 0L) {
            cache[albumId] = entry
        }
        return entry
    }

    private fun decodeAlbumThumbnail(contentUri: Uri, albumId: Long): Bitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching {
                context.contentResolver.loadThumbnail(contentUri, Size(512, 512), null)
            }.getOrNull()?.let { return it }
        }
        if (albumId <= 0L) return null
        return runCatching {
            val albumArtUri = ContentUris.withAppendedId(ALBUM_ART_URI, albumId)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(albumArtUri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
            var sampleSize = 1
            while (bounds.outWidth / sampleSize > 512 || bounds.outHeight / sampleSize > 512) {
                sampleSize *= 2
            }
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            context.contentResolver.openInputStream(albumArtUri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, opts)
            }
        }.getOrNull()
    }

    private fun extractSeedFromFile(file: File): Long? = runCatching {
        val opts = BitmapFactory.Options().apply {
            inSampleSize = 4
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        val bmp = BitmapFactory.decodeFile(file.absolutePath, opts) ?: return@runCatching null
        val seed = DynamicColorExtractor.extractSeedColorArgb(bmp)
        bmp.recycle()
        seed
    }.getOrNull()

    private fun isSupportedAudio(mimeType: String, displayName: String): Boolean {
        val mime = mimeType.lowercase()
        if (mime in SUPPORTED_MIME_TYPES) return true
        val ext = displayName.substringAfterLast('.', "").lowercase()
        return ext in SUPPORTED_EXTENSIONS
    }

    private fun inferMimeFromName(name: String): String {
        return when (name.substringAfterLast('.', "").lowercase()) {
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "m4a", "mp4" -> "audio/mp4"
            "aac" -> "audio/aac"
            "flac" -> "audio/flac"
            "ogg", "oga" -> "audio/ogg"
            "opus" -> "audio/opus"
            "ec3", "eac3" -> "audio/eac3-joc"
            "ac4" -> "audio/ac4"
            "ac3" -> "audio/ac3"
            "mka" -> "audio/x-matroska"
            else -> "audio/*"
        }
    }

    companion object {
        private val ALBUM_ART_URI: Uri = Uri.parse("content://media/external/audio/albumart")

        private val PROJECTION_LEGACY = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DATE_ADDED
        )

        private val PROJECTION_API_30_PLUS = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.GENRE
        )

        private val SUPPORTED_MIME_TYPES = hashSetOf(
            "audio/mpeg",
            "audio/mp3",
            "audio/flac",
            "audio/x-flac",
            "audio/wav",
            "audio/x-wav",
            "audio/mp4",
            "audio/m4a",
            "audio/x-m4a",
            "audio/aac",
            "audio/aac-adts",
            "audio/ogg",
            "application/ogg",
            "audio/opus",
            "audio/vorbis",
            "audio/ac3",
            "audio/eac3",
            "audio/eac3-joc",
            "audio/ac4",
            "audio/x-matroska"
        )

        private val SUPPORTED_EXTENSIONS = hashSetOf(
            "mp3", "flac", "wav", "m4a", "aac", "ogg", "oga", "opus", "ec3", "eac3", "ac4", "ac3", "mka"
        )
    }
}
