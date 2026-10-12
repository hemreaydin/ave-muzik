package com.avemuzik.player.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.avemuzik.player.R
import java.util.Locale

/** 3:07 veya 1:02:45 */
fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
    }
}

/** "45 dk" veya "1 sa 12 dk" */
@Composable
fun totalDurationText(ms: Long): String {
    val totalMinutes = (ms / 60_000).toInt()
    return if (totalMinutes >= 60) {
        stringResource(R.string.duration_hours_minutes, totalMinutes / 60, totalMinutes % 60)
    } else {
        stringResource(R.string.duration_minutes, totalMinutes.coerceAtLeast(1))
    }
}
