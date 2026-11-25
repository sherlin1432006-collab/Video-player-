package com.example.util

import java.util.Locale

fun formatDuration(ms: Long): String {
    if (ms <= 0L) return "00:00"
    val totalSeconds = ms / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

fun formatSignedSeconds(deltaSeconds: Int): String {
    val absSec = kotlin.math.abs(deltaSeconds)
    val mins = absSec / 60
    val secs = absSec % 60
    val prefix = if (deltaSeconds >= 0) "+" else "-"
    return if (mins > 0) {
        String.format(Locale.US, "%s%d:%02d", prefix, mins, secs)
    } else {
        String.format(Locale.US, "%s%ds", prefix, secs)
    }
}
