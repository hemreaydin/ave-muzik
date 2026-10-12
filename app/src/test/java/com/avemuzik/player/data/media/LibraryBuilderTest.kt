package com.avemuzik.player.data.media

import com.avemuzik.player.model.Song
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryBuilderTest {

    private fun song(
        id: Long,
        title: String,
        artist: String,
        artistId: Long,
        album: String,
        albumId: Long,
        track: Int = 0,
        folder: String = "Music",
    ) = Song(
        id = id,
        title = title,
        artist = artist,
        artistId = artistId,
        album = album,
        albumId = albumId,
        durationMs = 60_000,
        trackNumber = track,
        folderPath = folder,
        fileName = "$title.mp3",
        searchKey = SearchNormalizer.normalize("$title $artist $album"),
    )

    private val songs = listOf(
        song(1, "Zeytin", "Ayşe", 10, "Bahar", 100, track = 2),
        song(2, "Ateş", "Ayşe", 10, "Bahar", 100, track = 1),
        song(3, "Deniz", "Mehmet", 11, "Karışık", 200, folder = "Download"),
        song(4, "Çiçek", "Ayşe", 10, "Karışık", 200, folder = "WhatsApp/Media/WhatsApp Audio"),
    )

    private val library = LibraryBuilder.build(songs, variousArtists = "Çeşitli Sanatçılar")

    @Test
    fun albumsAreGroupedAndOrderedByTrack() {
        assertEquals(listOf(2L, 1L), library.songsByAlbum.getValue(100).map { it.id })
        assertEquals("Ayşe", library.albumsById.getValue(100).artist)
        assertEquals(2, library.albumsById.getValue(100).songCount)
    }

    @Test
    fun albumWithSeveralArtistsIsVariousArtists() {
        assertEquals("Çeşitli Sanatçılar", library.albumsById.getValue(200).artist)
    }

    @Test
    fun artistsCountSongsAndAlbums() {
        val ayse = library.artistsById.getValue(10)
        assertEquals(3, ayse.songCount)
        assertEquals(2, ayse.albumCount)
    }

    @Test
    fun foldersUseLastPathSegmentAsName() {
        val names = library.folders.associate { it.path to it.name }
        assertEquals("WhatsApp Audio", names.getValue("WhatsApp/Media/WhatsApp Audio"))
        assertEquals("Download", names.getValue("Download"))
        assertEquals(3, library.folders.size)
    }

    @Test
    fun songsAreSortedByTitle() {
        assertEquals(songs.size, library.songs.size)
        assertEquals("Ateş", library.songs.first().title)
        assertEquals("Zeytin", library.songs.last().title)
    }
}
