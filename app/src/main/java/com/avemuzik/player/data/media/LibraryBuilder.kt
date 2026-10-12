package com.avemuzik.player.data.media

import com.avemuzik.player.model.Album
import com.avemuzik.player.model.Artist
import com.avemuzik.player.model.Folder
import com.avemuzik.player.model.Library
import com.avemuzik.player.model.Song
import java.text.Collator

/** Taranan şarkı listesinden albüm, sanatçı ve klasör görünümlerini bellekte üretir. */
object LibraryBuilder {

    fun build(songs: List<Song>, variousArtists: String): Library {
        val collator = Collator.getInstance().apply { strength = Collator.PRIMARY }
        val byTitle = compareBy(collator) { song: Song -> song.title }
        val albumOrder = compareBy<Song> { it.trackNumber }.thenBy(collator) { it.title }

        val sortedSongs = songs.sortedWith(byTitle)

        val songsByAlbum = songs.groupBy { it.albumId }.mapValues { (_, list) -> list.sortedWith(albumOrder) }
        val albums = songsByAlbum.map { (albumId, list) ->
            val first = list.first()
            val artist = if (list.all { it.artist == first.artist }) first.artist else variousArtists
            Album(
                id = albumId,
                title = first.album,
                artist = artist,
                songCount = list.size,
                durationMs = list.sumOf { it.durationMs },
                artSongId = first.id,
                searchKey = SearchNormalizer.normalize("${first.album} $artist"),
            )
        }.sortedWith(compareBy(collator) { it.title })

        val songsByArtist = songs.groupBy { it.artistId }.mapValues { (_, list) ->
            list.sortedWith(compareBy(collator) { song: Song -> song.album }.then(albumOrder))
        }
        val artists = songsByArtist.map { (artistId, list) ->
            val first = list.first()
            Artist(
                id = artistId,
                name = first.artist,
                songCount = list.size,
                albumCount = list.distinctBy { it.albumId }.size,
                artSongId = first.id,
                searchKey = SearchNormalizer.normalize(first.artist),
            )
        }.sortedWith(compareBy(collator) { it.name })

        val songsByFolder = songs.groupBy { it.folderPath }.mapValues { (_, list) -> list.sortedWith(byTitle) }
        val folders = songsByFolder.map { (path, list) ->
            Folder(
                path = path,
                name = path.substringAfterLast('/'),
                songCount = list.size,
                artSongId = list.first().id,
            )
        }.sortedWith(compareBy(collator) { it.name })

        return Library(
            songs = sortedSongs,
            albums = albums,
            artists = artists,
            folders = folders,
            songsById = songs.associateBy { it.id },
            albumsById = albums.associateBy { it.id },
            artistsById = artists.associateBy { it.id },
            songsByAlbum = songsByAlbum,
            songsByArtist = songsByArtist,
            songsByFolder = songsByFolder,
        )
    }
}
