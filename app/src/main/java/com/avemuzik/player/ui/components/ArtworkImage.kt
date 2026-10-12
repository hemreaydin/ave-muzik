package com.avemuzik.player.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Şarkının yerel kapak görselini gösterir; yoksa sade bir yer tutucu çizer.
 * [loadSize] decode edilecek boyuttur (yerleşimi [modifier] belirler) — listelerde küçük tutulur.
 */
@Composable
fun ArtworkImage(
    songId: Long?,
    loadSize: Dp,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(10.dp),
    placeholderIcon: ImageVector = Icons.Rounded.MusicNote,
    placeholderIconSize: Dp = 22.dp,
) {
    val loader = LocalArtworkLoader.current
    val sizePx = with(LocalDensity.current) { loadSize.roundToPx() }
    val image: ImageBitmap? by produceState(
        initialValue = songId?.let { loader.peek(it, sizePx) },
        songId,
        sizePx,
    ) {
        value = songId?.let { loader.peek(it, sizePx) ?: loader.load(it, sizePx) }
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        val bitmap = image
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                imageVector = placeholderIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(placeholderIconSize),
            )
        }
    }
}
