package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.Track

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val id: String,
    val uriString: String,
    val title: String,
    val artist: String,
    val album: String,
    val genre: String,
    val durationMs: Long,
    val artworkPath: String?,
    val artworkSeedColor: Long?,
    val dateAdded: Long,
    val lastPlayedAt: Long?,
    val playCount: Int,
    val isFavorite: Boolean,
    val lyricsText: String?,
    val mimeType: String,
    val fileSizeBytes: Long
) {
    fun toDomain(isAvailable: Boolean = true): Track = Track(
        id = id,
        uriString = uriString,
        title = title,
        artist = artist,
        album = album,
        genre = genre,
        durationMs = durationMs,
        artworkPath = artworkPath,
        artworkSeedColor = artworkSeedColor,
        dateAdded = dateAdded,
        lastPlayedAt = lastPlayedAt,
        playCount = playCount,
        isFavorite = isFavorite,
        lyricsText = lyricsText,
        mimeType = mimeType,
        fileSizeBytes = fileSizeBytes,
        isAvailable = isAvailable
    )

    companion object {
        fun fromDomain(track: Track): TrackEntity = TrackEntity(
            id = track.id,
            uriString = track.uriString,
            title = track.title,
            artist = track.artist,
            album = track.album,
            genre = track.genre,
            durationMs = track.durationMs,
            artworkPath = track.artworkPath,
            artworkSeedColor = track.artworkSeedColor,
            dateAdded = track.dateAdded,
            lastPlayedAt = track.lastPlayedAt,
            playCount = track.playCount,
            isFavorite = track.isFavorite,
            lyricsText = track.lyricsText,
            mimeType = track.mimeType,
            fileSizeBytes = track.fileSizeBytes
        )
    }
}

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val createdAt: Long,
    val updatedAt: Long,
    val coverArtIndex: Int
)

@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["playlistId", "trackId"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TrackEntity::class,
            parentColumns = ["id"],
            childColumns = ["trackId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("playlistId"), Index("trackId")]
)
data class PlaylistTrackEntity(
    val playlistId: String,
    val trackId: String,
    val sortOrder: Int
)
