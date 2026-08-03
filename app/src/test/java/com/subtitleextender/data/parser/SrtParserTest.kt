package com.subtitleextender.data.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SrtParserTest {

    private val parser = SrtParser()

    @Test
    fun `parses a simple single subtitle`() {
        val srt = """
            1
            00:00:12,000 --> 00:00:13,000
            Hello
        """.trimIndent()

        val entries = parser.parse(srt)

        assertEquals(1, entries.size)
        assertEquals("1", entries[0].index)
        assertEquals(12_000L, entries[0].startTimeMillis)
        assertEquals(13_000L, entries[0].endTimeMillis)
        assertEquals("Hello", entries[0].text)
    }

    @Test
    fun `parses multiple subtitles separated by a blank line`() {
        val srt = """
            1
            00:00:12,000 --> 00:00:13,000
            Hello

            2
            00:00:13,400 --> 00:00:15,000
            How are you?
        """.trimIndent()

        val entries = parser.parse(srt)

        assertEquals(2, entries.size)
        assertEquals("Hello", entries[0].text)
        assertEquals("How are you?", entries[1].text)
        assertEquals(13_400L, entries[1].startTimeMillis)
        assertEquals(15_000L, entries[1].endTimeMillis)
    }

    @Test
    fun `tolerates multiple blank lines between blocks`() {
        val srt = "1\n00:00:01,000 --> 00:00:02,000\nHello\n\n\n\n" +
            "2\n00:00:03,000 --> 00:00:04,000\nWorld"

        val entries = parser.parse(srt)

        assertEquals(2, entries.size)
        assertEquals("World", entries[1].text)
    }

    @Test
    fun `preserves multi-line subtitle text and its internal line break`() {
        val srt = """
            1
            00:00:01,000 --> 00:00:04,000
            First line
            Second line
        """.trimIndent()

        val entries = parser.parse(srt)

        assertEquals("First line\nSecond line", entries[0].text)
    }

    @Test
    fun `preserves UTF-8 and Turkish characters`() {
        val srt = """
            1
            00:00:01,000 --> 00:00:04,000
            Günaydın, nasılsın? Öğretmen çiçek getirdi.
        """.trimIndent()

        val entries = parser.parse(srt)

        assertEquals("Günaydın, nasılsın? Öğretmen çiçek getirdi.", entries[0].text)
    }

    @Test
    fun `strips a leading byte order mark`() {
        val srt = "\uFEFF1\n00:00:01,000 --> 00:00:04,000\nHello"

        val entries = parser.parse(srt)

        assertEquals("1", entries[0].index)
        assertEquals(1, entries.size)
    }

    @Test
    fun `preserves the original index values verbatim`() {
        val srt = "5\n00:00:01,000 --> 00:00:02,000\nA\n\n" +
            "12\n00:00:03,000 --> 00:00:04,000\nB"

        val entries = parser.parse(srt)

        assertEquals("5", entries[0].index)
        assertEquals("12", entries[1].index)
    }

    @Test
    fun `throws for empty content`() {
        assertThrows(SrtParseException::class.java) {
            parser.parse("")
        }
    }

    @Test
    fun `throws for content with only whitespace`() {
        assertThrows(SrtParseException::class.java) {
            parser.parse("   \n\n  \n")
        }
    }

    @Test
    fun `throws for a non-numeric index`() {
        val srt = """
            one
            00:00:01,000 --> 00:00:04,000
            Hello
        """.trimIndent()

        assertThrows(SrtParseException::class.java) {
            parser.parse(srt)
        }
    }

    @Test
    fun `throws for a malformed timestamp line`() {
        val srt = """
            1
            00:00:01 --> 00:00:04
            Hello
        """.trimIndent()

        assertThrows(SrtParseException::class.java) {
            parser.parse(srt)
        }
    }

    @Test
    fun `throws when a block is missing its text`() {
        val srt = "1\n00:00:01,000 --> 00:00:02,000\n\n2\n00:00:03,000 --> 00:00:04,000\nWorld"

        assertThrows(SrtParseException::class.java) {
            parser.parse(srt)
        }
    }

    @Test
    fun `throws when end time is before start time`() {
        val srt = """
            1
            00:00:05,000 --> 00:00:01,000
            Hello
        """.trimIndent()

        assertThrows(SrtParseException::class.java) {
            parser.parse(srt)
        }
    }

    @Test
    fun `throws when end time equals start time`() {
        val srt = """
            1
            00:00:05,000 --> 00:00:05,000
            Hello
        """.trimIndent()

        assertThrows(SrtParseException::class.java) {
            parser.parse(srt)
        }
    }

    @Test
    fun `throws when the file ends right after an index line`() {
        assertThrows(SrtParseException::class.java) {
            parser.parse("1")
        }
    }
}
