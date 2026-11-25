package com.example

import com.example.util.formatDuration
import com.example.util.formatSignedSeconds
import org.junit.Assert.assertEquals
import org.junit.Test

class TimeFormatterTest {
    @Test
    fun formatDuration_formatsMinutesAndHours() {
        assertEquals("00:00", formatDuration(0L))
        assertEquals("00:09", formatDuration(9_000L))
        assertEquals("02:15", formatDuration(135_000L))
        assertEquals("1:01:05", formatDuration(3_665_000L))
    }

    @Test
    fun formatSignedSeconds_formatsPositiveAndNegative() {
        assertEquals("+10s", formatSignedSeconds(10))
        assertEquals("-15s", formatSignedSeconds(-15))
        assertEquals("+1:30", formatSignedSeconds(90))
    }
}
