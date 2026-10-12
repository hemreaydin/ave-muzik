package com.avemuzik.player.ui.nowplaying

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import com.avemuzik.player.R
import com.avemuzik.player.data.repository.LibraryState
import com.avemuzik.player.navigation.AppNavigator
import com.avemuzik.player.player.PlaybackState
import com.avemuzik.player.ui.components.ArtworkImage
import com.avemuzik.player.ui.components.LocalAppContainer
import com.avemuzik.player.ui.components.LocalArtworkLoader
import com.avemuzik.player.ui.components.LocalSongActions
import com.avemuzik.player.ui.components.formatDuration
import com.avemuzik.player.ui.viewmodel.CollectionsViewModel
import com.avemuzik.player.ui.viewmodel.PlayerViewModel

@Composable
fun NowPlayingScreen(
    playerViewModel: PlayerViewModel,
    collectionsViewModel: CollectionsViewModel,
    navigator: AppNavigator,
) {
    val state by playerViewModel.state.collectAsStateWithLifecycle()
    val position = playerViewModel.position.collectAsStateWithLifecycle()
    val favorites by collectionsViewModel.favoriteIds.collectAsStateWithLifecycle()
    val libraryState by LocalAppContainer.current.musicRepository.state.collectAsStateWithLifecycle()
    val songActions = LocalSongActions.current

    val songId = state.songId
    val song = songId?.let { (libraryState as? LibraryState.Ready)?.library?.songsById?.get(it) }
    val durationMs = state.durationMs.takeIf { it > 0 } ?: song?.durationMs ?: 0L
    var showQueue by rememberSaveable { mutableStateOf(false) }

    // Kuyruk tamamen boşaldıysa (ör. son şarkı kuyruktan çıkarıldı) ekranı kapat.
    LaunchedEffect(state.hasMedia, state.isConnected) {
        if (state.isConnected && !state.hasMedia) navigator.back()
    }

    // Arka plan, albüm kapağının baskın rengine göre yumuşakça renklenir.
    val loader = LocalArtworkLoader.current
    val background = MaterialTheme.colorScheme.background
    var artColor by remember { mutableStateOf<Color?>(null) }
    LaunchedEffect(songId) {
        artColor = songId?.let { loader.dominantColor(it) }?.let { Color(it) }
    }
    val tint by animateColorAsState(
        targetValue = artColor?.let { lerp(background, it, 0.45f) } ?: background,
        animationSpec = tween(600),
        label = "nowPlayingTint",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(tint, background))),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = navigator::back) {
                    Icon(
                        Icons.Rounded.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.close),
                        modifier = Modifier.size(30.dp),
                    )
                }
                Text(
                    text = stringResource(R.string.now_playing),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { showQueue = true }) {
                    Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = stringResource(R.string.queue))
                }
                if (song != null) {
                    IconButton(onClick = { songActions.open(song) }) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.more_options))
                    }
                }
            }

            // Kapak, kalan alana sığan en büyük kare olarak yerleşir (yatay ekranda da taşmaz).
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                ArtworkImage(
                    songId = songId,
                    loadSize = 360.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f, matchHeightConstraintsFirst = true)
                        .shadow(elevation = 24.dp, shape = RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    placeholderIconSize = 96.dp,
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = state.title,
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee(),
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = state.artist,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (songId != null) {
                    val isFavorite = songId in favorites
                    IconButton(onClick = { collectionsViewModel.toggleFavorite(songId) }) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = stringResource(if (isFavorite) R.string.favorite_remove else R.string.favorite_add),
                            tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            SeekBar(position = position, durationMs = durationMs, onSeek = playerViewModel::seekTo)
            Spacer(Modifier.height(8.dp))
            PlaybackControls(
                state = state,
                onShuffle = playerViewModel::toggleShuffle,
                onPrevious = playerViewModel::previous,
                onPlayPause = playerViewModel::togglePlayPause,
                onNext = playerViewModel::next,
                onRepeat = playerViewModel::cycleRepeatMode,
            )
            Spacer(Modifier.height(32.dp))
        }
    }

    if (showQueue) {
        QueueSheet(
            playerViewModel = playerViewModel,
            queueVersion = state.queueVersion,
            onDismiss = { showQueue = false },
        )
    }
}

/** Yalnızca bu bileşen konum güncellemeleriyle yeniden çizilir. */
@Composable
private fun SeekBar(position: State<Long>, durationMs: Long, onSeek: (Long) -> Unit) {
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val duration = durationMs.coerceAtLeast(1L)
    val playedFraction = (position.value.toFloat() / duration).coerceIn(0f, 1f)
    val fraction = dragFraction ?: playedFraction
    val shownPosition = dragFraction?.let { (it * duration).toLong() } ?: position.value

    Column {
        Slider(
            value = fraction,
            onValueChange = { dragFraction = it },
            onValueChangeFinished = {
                dragFraction?.let { onSeek((it * duration).toLong()) }
                dragFraction = null
            },
            enabled = durationMs > 0,
            colors = SliderDefaults.colors(
                inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f),
            ),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = formatDuration(shownPosition.coerceAtMost(durationMs)),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatDuration(durationMs),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PlaybackControls(
    state: PlaybackState,
    onShuffle: () -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onRepeat: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ToggleControl(
            icon = Icons.Rounded.Shuffle,
            active = state.shuffleEnabled,
            description = stringResource(if (state.shuffleEnabled) R.string.shuffle_on else R.string.shuffle_off),
            onClick = onShuffle,
        )
        IconButton(onClick = onPrevious, modifier = Modifier.size(60.dp)) {
            Icon(Icons.Rounded.SkipPrevious, contentDescription = stringResource(R.string.previous), modifier = Modifier.size(40.dp))
        }
        FilledIconButton(onClick = onPlayPause, modifier = Modifier.size(76.dp), shape = CircleShape) {
            Icon(
                imageVector = if (state.showPause) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = stringResource(if (state.showPause) R.string.pause else R.string.play),
                modifier = Modifier.size(42.dp),
            )
        }
        IconButton(onClick = onNext, modifier = Modifier.size(60.dp)) {
            Icon(Icons.Rounded.SkipNext, contentDescription = stringResource(R.string.next), modifier = Modifier.size(40.dp))
        }
        ToggleControl(
            icon = if (state.repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
            active = state.repeatMode != Player.REPEAT_MODE_OFF,
            description = stringResource(
                when (state.repeatMode) {
                    Player.REPEAT_MODE_ALL -> R.string.repeat_all
                    Player.REPEAT_MODE_ONE -> R.string.repeat_one
                    else -> R.string.repeat_off
                },
            ),
            onClick = onRepeat,
        )
    }
}

/** Aktifken vurgu renginde ve altında küçük bir nokta ile gösterilen kontrol. */
@Composable
private fun ToggleControl(icon: ImageVector, active: Boolean, description: String, onClick: () -> Unit) {
    val activeColor = MaterialTheme.colorScheme.primary
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = icon,
                contentDescription = description,
                tint = if (active) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(
            Modifier
                .size(4.dp)
                .background(if (active) activeColor else Color.Transparent, CircleShape),
        )
    }
}
