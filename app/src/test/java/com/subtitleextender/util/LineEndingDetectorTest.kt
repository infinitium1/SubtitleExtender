package com.subtitleextender.util

import org.junit.Assert.assertEquals
import org.junit.Test

class LineEndingDetectorTest {

    @Test
    fun `detects Windows-style line endings`() {
        val content = "1\r\n00:00:01,000 --> 00:00:02,000\r\nHello"
        assertEquals("\r\n", LineEndingDetector.detect(content))
    }

    @Test
    fun `defaults to Unix-style line endings`() {
        val content = "1\n00:00:01,000 --> 00:00:02,000\nHello"
        assertEquals("\n", LineEndingDetector.detect(content))
    }

    @Test
    fun `defaults to Unix-style line endings for empty content`() {
        assertEquals("\n", LineEndingDetector.detect(""))
    }
}
