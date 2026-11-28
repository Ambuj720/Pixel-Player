package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    @Query("SELECT * FROM tracks ORDER BY dateAdded DESC")
    fun observeAllTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE id = :id LIMIT 1")
    suspend fun getTrackById(id: String): TrackEntity?

    @Query("SELECT * FROM tracks WHERE uriString = :uriString LIMIT 1")
    suspend fun getTrackByUri(uriString: String): TrackEntity?

    @Query("SELECT uriString FROM tracks")
    suspend fun getAllUriStrings(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTracks(tracks: List<TrackEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTrack(track: TrackEntity)

    @Query("UPDATE tracks SET isFavorite = :favorite WHERE id = :trackId")
    suspend fun setFavorite(trackId: String, favorite: Boolean)

    @Query("UPDATE tracks SET playCount = playCount + 1, lastPlayedAt = :playedAt WHERE id = :trackId")
    suspend fun recordPlayback(trackId: String, playedAt: Long)

    @Query("UPDATE tracks SET lyricsText = :lyrics WHERE id = :trackId")
    suspend fun updateLyrics(trackId: String, lyrics: String?)

    @Query("DELETE FROM tracks WHERE id = :trackId")
    suspend fun deleteTrack(trackId: String)
}

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY updatedAt DESC")
    fun observePlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlist_tracks ORDER BY playlistId, sortOrder ASC")
    fun observeAllPlaylistTracks(): Flow<List<PlaylistTrackEntity>>

    @Query("SELECT * FROM playlist_tracks WHERE playlistId = :playlistId ORDER BY sortOrder ASC")
    suspend fun getTracksForPlaylist(playlistId: String): List<PlaylistTrackEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId)")
    suspend fun isTrackInPlaylist(playlistId: String, trackId: String): Boolean

    @Query("SELECT MAX(sortOrder) FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun getMaxSortOrder(playlistId: String): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlaylist(playlist: PlaylistEntity)

    @Query("UPDATE playlists SET name = :name, description = :description, updatedAt = :updatedAt WHERE id = :playlistId")
    suspend fun renamePlaylist(playlistId: String, name: String, description: String, updatedAt: Long)

    @Query("UPDATE playlists SET updatedAt = :updatedAt WHERE id = :playlistId")
    suspend fun touchPlaylist(playlistId: String, updatedAt: Long)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistTrack(entry: PlaylistTrackEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistTracks(entries: List<PlaylistTrackEntity>)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun clearPlaylistTracks(playlistId: String)

    @Transaction
    suspend fun replacePlaylistTrackOrder(playlistId: String, orderedTrackIds: List<String>, updatedAt: Long) {
        clearPlaylistTracks(playlistId)
        if (orderedTrackIds.isNotEmpty()) {
            val entries = ArrayList<PlaylistTrackEntity>(orderedTrackIds.size)
            for (index in orderedTrackIds.indices) {
                entries.add(PlaylistTrackEntity(playlistId, orderedTrackIds[index], index))
            }
            insertPlaylistTracks(entries)
        }
        touchPlaylist(playlistId, updatedAt)
    }
}
