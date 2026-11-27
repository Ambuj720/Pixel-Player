package com.example.data

import android.content.Context
import android.net.Uri
import com.example.data.local.AppDatabase
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistTrackEntity
import com.example.data.local.TrackEntity
import com.example.data.media.MetadataParser
import com.example.data.media.SampleAudioGenerator
import com.example.data.model.Playlist
import com.example.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

class MusicRepository(context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val trackDao = db.trackDao()
    private val playlistDao = db.playlistDao()
    private val metadataParser = MetadataParser(context)
    private val sampleGenerator = SampleAudioGenerator(context)

    val tracks: Flow<List<Track>> = trackDao.observeAllTracks().map { entities ->
        entities.map { entity -> entity.toDomain() }
    }

    val playlists: Flow<List<Playlist>> = combine(
        playlistDao.observePlaylists(),
        playlistDao.observeAllPlaylistTracks()
    ) { playlistEntities, mappings ->
        val byPlaylist = mappings.groupBy { it.playlistId }
        playlistEntities.map { entity ->
            val trackIds = byPlaylist[entity.id]
                ?.sortedBy { it.sortOrder }
                ?.map { it.trackId }
                .orEmpty()
            Playlist(
                id = entity.id,
                name = entity.name,
                description = entity.description,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt,
                trackIds = trackIds,
                coverArtIndex = entity.coverArtIndex
            )
        }
    }

    suspend fun importUris(uris: List<Uri>): Int = withContext(Dispatchers.IO) {
        var importedCount = 0
        val newEntities = mutableListOf<TrackEntity>()
        for (uri in uris) {
            val existing = trackDao.getTrackByUri(uri.toString())
            if (existing != null) continue
            val parsed = metadataParser.parseUri(uri)
            if (parsed != null) {
                newEntities.add(TrackEntity.fromDomain(parsed))
                importedCount++
            }
        }
        if (newEntities.isNotEmpty()) {
            trackDao.upsertTracks(newEntities)
        }
        importedCount
    }

    suspend fun generateLocalDemoTracks(): Int = withContext(Dispatchers.IO) {
        val samples = sampleGenerator.generateLocalSamples()
        val entities = samples.map { TrackEntity.fromDomain(it) }
        trackDao.upsertTracks(entities)
        // Also create a starter playlist if none exists yet
        val existingPlaylistId = "playlist_evening_focus"
        val now = System.currentTimeMillis()
        playlistDao.upsertPlaylist(
            PlaylistEntity(
                id = existingPlaylistId,
                name = "Evening Sessions",
                description = "Warm local synth & ambient cuts",
                createdAt = now,
                updatedAt = now,
                coverArtIndex = 0
            )
        )
        playlistDao.replacePlaylistTrackOrder(
            playlistId = existingPlaylistId,
            orderedTrackIds = samples.map { it.id },
            updatedAt = now
        )
        samples.size
    }

    suspend fun verifyTrackAvailable(uriString: String): Boolean = withContext(Dispatchers.IO) {
        metadataParser.isUriAccessible(uriString)
    }

    suspend fun toggleFavorite(trackId: String, currentFavorite: Boolean) {
        trackDao.setFavorite(trackId, !currentFavorite)
    }

    suspend fun recordPlayback(trackId: String) {
        trackDao.recordPlayback(trackId, System.currentTimeMillis())
    }

    suspend fun updateLyrics(trackId: String, lyrics: String?) {
        trackDao.updateLyrics(trackId, lyrics?.takeIf { it.isNotBlank() })
    }

    suspend fun deleteTrack(trackId: String) {
        trackDao.deleteTrack(trackId)
    }

    suspend fun createPlaylist(name: String, description: String, initialTrackIds: List<String> = emptyList()): String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val coverIdx = (name.hashCode().and(0x7fffffff)) % 3
        playlistDao.upsertPlaylist(
            PlaylistEntity(
                id = id,
                name = name.trim().ifBlank { "Untitled Playlist" },
                description = description.trim(),
                createdAt = now,
                updatedAt = now,
                coverArtIndex = coverIdx
            )
        )
        if (initialTrackIds.isNotEmpty()) {
            playlistDao.replacePlaylistTrackOrder(id, initialTrackIds, now)
        }
        return id
    }

    suspend fun renamePlaylist(playlistId: String, name: String, description: String) {
        playlistDao.renamePlaylist(
            playlistId = playlistId,
            name = name.trim().ifBlank { "Untitled Playlist" },
            description = description.trim(),
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun deletePlaylist(playlistId: String) {
        playlistDao.deletePlaylist(playlistId)
    }

    suspend fun addTrackToPlaylist(playlistId: String, trackId: String) {
        val current = playlistDao.getTracksForPlaylist(playlistId)
        if (current.any { it.trackId == trackId }) return
        val nextOrder = (current.maxOfOrNull { it.sortOrder } ?: -1) + 1
        playlistDao.insertPlaylistTrack(PlaylistTrackEntity(playlistId, trackId, nextOrder))
        playlistDao.touchPlaylist(playlistId, System.currentTimeMillis())
    }

    suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
        playlistDao.removeTrackFromPlaylist(playlistId, trackId)
        val remaining = playlistDao.getTracksForPlaylist(playlistId).map { it.trackId }
        playlistDao.replacePlaylistTrackOrder(playlistId, remaining, System.currentTimeMillis())
    }

    suspend fun reorderPlaylistTracks(playlistId: String, newOrderedTrackIds: List<String>) {
        playlistDao.replacePlaylistTrackOrder(playlistId, newOrderedTrackIds, System.currentTimeMillis())
    }
}
