package com.avemuzik.player.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Uzun listeler için hafif hızlı kaydırma tutamağı. Kaydırma sırasında sağ kenarda belirir;
 * tutup sürükleyerek binlerce şarkı arasında anında gezinilir. Konum hesabı yalnızca
 * yerleşim aşamasında okunur, kaydırma sırasında recomposition üretmez.
 */
@Composable
fun BoxScope.FastScroller(state: LazyListState, itemCount: Int) {
    if (itemCount < MIN_ITEMS) return

    val scope = rememberCoroutineScope()
    var dragging by remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(false) }
    var trackHeight by remember { mutableIntStateOf(0) }
    val thumbHeightPx = with(LocalDensity.current) { THUMB_HEIGHT.toPx() }

    val scrolling = state.isScrollInProgress
    LaunchedEffect(scrolling, dragging) {
        if (scrolling || dragging) {
            visible = true
        } else {
            delay(HIDE_DELAY_MS)
            visible = false
        }
    }
    val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(if (visible) 120 else 400), label = "scroller")

    val progress by remember(state, itemCount) {
        derivedStateOf {
            val visibleCount = state.layoutInfo.visibleItemsInfo.size
            val maxFirst = (itemCount - visibleCount).coerceAtLeast(1)
            (state.firstVisibleItemIndex.toFloat() / maxFirst).coerceIn(0f, 1f)
        }
    }

    fun jumpTo(y: Float) {
        if (trackHeight <= 0) return
        val fraction = (y / trackHeight).coerceIn(0f, 1f)
        val target = (fraction * (itemCount - 1)).roundToInt()
        scope.launch { state.scrollToItem(target) }
    }

    Box(
        modifier = Modifier
            .align(Alignment.CenterEnd)
            .fillMaxHeight()
            .width(TOUCH_WIDTH)
            .onSizeChanged { trackHeight = it.height }
            .graphicsLayer { this.alpha = alpha }
            .then(
                if (visible) {
                    Modifier.pointerInput(itemCount) {
                        detectVerticalDragGestures(
                            onDragStart = { offset -> dragging = true; jumpTo(offset.y) },
                            onDragEnd = { dragging = false },
                            onDragCancel = { dragging = false },
                        ) { change, _ ->
                            change.consume()
                            jumpTo(change.position.y)
                        }
                    }
                } else {
                    Modifier
                },
            ),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset { IntOffset(0, ((trackHeight - thumbHeightPx) * progress).roundToInt().coerceAtLeast(0)) }
                .padding(end = 4.dp)
                .size(width = 6.dp, height = THUMB_HEIGHT)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}

private const val MIN_ITEMS = 60
private const val HIDE_DELAY_MS = 1_200L
private val THUMB_HEIGHT = 52.dp
private val TOUCH_WIDTH = 28.dp
