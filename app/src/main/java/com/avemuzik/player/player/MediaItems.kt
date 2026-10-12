package com.avemuzik.player.player

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.avemuzik.player.model.Song

/**
 * Şarkıyı Media3 öğesine çevirir. mediaId = MediaStore kimliği.
 * artworkUri olarak şarkının kendi content:// adresi verilir; kapak [ArtworkBitmapLoader]
 * tarafından cihaz içinden okunur.
 */
fun Song.toMediaItem(): MediaItem {
    val uri = uri
    return MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(uri)
        .setRequestMetadata(MediaItem.RequestMetadata.Builder().setMediaUri(uri).build())
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setArtworkUri(uri)
                .setIsPlayable(true)
                .setIsBrowsable(false)
                .build(),
        )
        .build()
}

/**
 * Controller'dan servise aktarılan öğelerde oynatma adresi (localConfiguration) bulunmayabilir;
 * bu durumda adres requestMetadata'dan veya mediaId'den yeniden kurulur.
 */
fun MediaItem.withPlaybackUri(): MediaItem {
    if (localConfiguration != null) return this
    val uri = requestMetadata.mediaUri
        ?: mediaId.toLongOrNull()?.let(Song::uriFor)
        ?: return this
    return buildUpon().setUri(uri).build()
}
