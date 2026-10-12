package com.avemuzik.player.data.media

import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import com.avemuzik.player.R
import com.avemuzik.player.model.Song

/**
 * Cihazdaki ses dosyalarını MediaStore üzerinden okur (Scoped Storage uyumlu).
 * Dosya sistemine doğrudan erişilmez; ağ hiç kullanılmaz. Ana thread dışında çağrılmalıdır.
 */
class MediaStoreScanner(private val context: Context) {

    fun scan(): List<Song> {
        val unknownArtist = context.getString(R.string.unknown_artist)
        val unknownAlbum = context.getString(R.string.unknown_album)
        val unknownTitle = context.getString(R.string.unknown_title)
        val rootFolder = context.getString(R.string.internal_storage)

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ARTIST_ID,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.RELATIVE_PATH,
            MediaStore.Audio.Media.DISPLAY_NAME,
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        val songs = ArrayList<Song>()
        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            null,
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val artistIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val trackCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val pathCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.RELATIVE_PATH)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)

            songs.ensureCapacity(cursor.count)
            while (cursor.moveToNext()) {
                val fileName = cursor.stringOrNull(nameCol).orEmpty()
                val title = cursor.stringOrNull(titleCol).cleanTag()
                    ?: fileName.substringBeforeLast('.').ifBlank { unknownTitle }
                val artist = cursor.stringOrNull(artistCol).cleanTag() ?: unknownArtist
                val album = cursor.stringOrNull(albumCol).cleanTag() ?: unknownAlbum
                val folder = cursor.stringOrNull(pathCol)?.trim('/')?.takeIf { it.isNotEmpty() } ?: rootFolder

                songs += Song(
                    id = cursor.getLong(idCol),
                    title = title,
                    artist = artist,
                    artistId = cursor.getLong(artistIdCol),
                    album = album,
                    albumId = cursor.getLong(albumIdCol),
                    durationMs = cursor.getLong(durationCol).coerceAtLeast(0L),
                    trackNumber = cursor.getInt(trackCol),
                    folderPath = folder,
                    fileName = fileName,
                    searchKey = SearchNormalizer.normalize("$title $artist $album"),
                )
            }
        }
        return songs
    }

    private fun Cursor.stringOrNull(index: Int): String? = if (isNull(index)) null else getString(index)

    private fun String?.cleanTag(): String? =
        this?.trim()?.takeIf { it.isNotEmpty() && it != MediaStore.UNKNOWN_STRING }
}
