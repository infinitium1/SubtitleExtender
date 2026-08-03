package com.subtitleextender.data.parser

import com.subtitleextender.data.model.SubtitleEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class SrtWriterTest {

    private val writer = SrtWriter()

    @Test
    fun `writes a single subtitle in standard SRT format`() {
        val entries = listOf(
            SubtitleEntry(index = "1", startTimeMillis = 12_000L, endTimeMillis = 13_500L, text = "Hello")
        )

        val output = writer.write(entries, lineEnding = "\n")

        val expected = "1\n00:00:12,000 --> 00:00:13,500\nHello\n"
        assertEquals(expected, output)
    }

    @Test
    fun `separates multiple subtitles with exactly one blank line`() {
        val entries = listOf(
            SubtitleEntry(index = "1", startTimeMillis = 1_000L, endTimeMillis = 2_000L, text = "One"),
            SubtitleEntry(index = "2", startTimeMillis = 3_000L, endTimeMillis = 4_000L, text = "Two")
        )

        val output = writer.write(entries, lineEnding = "\n")

        val expected = "1\n00:00:01,000 --> 00:00:02,000\nOne\n\n2\n00:00:03,000 --> 00:00:04,000\nTwo\n"
        assertEquals(expected, output)
    }

    @Test
    fun `writes multi-line text using the requested line ending`() {
        val entries = listOf(
            SubtitleEntry(index = "1", startTimeMillis = 0L, endTimeMillis = 1_000L, text = "First\nSecond")
        )

        val output = writer.write(entries, lineEnding = "\r\n")

        val expected = "1\r\n00:00:00,000 --> 00:00:01,000\r\nFirst\r\nSecond\r\n"
        assertEquals(expected, output)
    }

    @Test
    fun `round trips through the parser without altering data`() {
        val original = "1\n00:00:12,000 --> 00:00:13,500\nHello\n\n" +
            "2\n00:00:14,000 --> 00:00:16,000\nHow are you?"
        val parser = SrtParser()

        val entries = parser.parse(original)
        val rewritten = writer.write(entries, lineEnding = "\n")
        val reparsed = parser.parse(rewritten)

        assertEquals(entries, reparsed)
    }

    @Test
    fun `handles an empty list by producing empty output`() {
        assertEquals("", writer.write(emptyList()))
    }
}
