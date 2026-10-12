package com.avemuzik.player.model

import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.runtime.Immutable

@Immutable
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val artistId: Long,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val trackNumber: Int,
    /** MediaStore RELATIVE_PATH, sondaki "/" olmadan. Örn: "Music/Rock". */
    val folderPath: String,
    val fileName: String,
    /** Arama için önceden normalize edilmiş "başlık sanatçı albüm". */
    val searchKey: String,
) {
    val uri: Uri get() = uriFor(id)

    companion object {
        fun uriFor(id: Long): Uri =
            ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
    }
}

@Immutable
data class Album(
    val id: Long,
    val title: String,
    val artist: String,
    val songCount: Int,
    val durationMs: Long,
    /** Kapak görseli bu şarkının gömülü kapağından okunur. */
    val artSongId: Long,
    val searchKey: String,
)

@Immutable
data class Artist(
    val id: Long,
    val name: String,
    val songCount: Int,
    val albumCount: Int,
    val artSongId: Long,
    val searchKey: String,
)

@Immutable
data class Folder(
    val path: String,
    val name: String,
    val songCount: Int,
    val artSongId: Long,
)

@Immutable
data class Playlist(
    val id: Long,
    val name: String,
    val songCount: Int,
)

/** Bir tarama sonucunda oluşan, bellekte tutulan müzik kütüphanesi. */
@Immutable
data class Library(
    val songs: List<Song>,
    val albums: List<Album>,
    val artists: List<Artist>,
    val folders: List<Folder>,
    val songsById: Map<Long, Song>,
    val albumsById: Map<Long, Album>,
    val artistsById: Map<Long, Artist>,
    val songsByAlbum: Map<Long, List<Song>>,
    val songsByArtist: Map<Long, List<Song>>,
    val songsByFolder: Map<String, List<Song>>,
)

enum class ThemeMode(val key: String) {
    SYSTEM("system"),
    DARK("dark"),
    LIGHT("light");

    companion object {
        /** İlk açılışta tema koyu (Ave-Müzik koyu tema ağırlıklıdır). */
        val Default = DARK

        fun fromKey(key: String?): ThemeMode = entries.firstOrNull { it.key == key } ?: Default
    }
}
