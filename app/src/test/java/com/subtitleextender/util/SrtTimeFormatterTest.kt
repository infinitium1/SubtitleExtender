package com.subtitleextender.util

import com.subtitleextender.data.parser.SrtParseException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SrtTimeFormatterTest {

    @Test
    fun `parseToMillis converts a standard timestamp correctly`() {
        assertEquals(12_000L, SrtTimeFormatter.parseToMillis("00:00:12,000"))
        assertEquals(73_500L, SrtTimeFormatter.parseToMillis("00:01:13,500"))
        assertEquals(3_661_001L, SrtTimeFormatter.parseToMillis("01:01:01,001"))
    }

    @Test
    fun `parseToMillis tolerates surrounding whitespace`() {
        assertEquals(12_000L, SrtTimeFormatter.parseToMillis("  00:00:12,000  "))
    }

    @Test
    fun `formatFromMillis produces a zero-padded standard timestamp`() {
        assertEquals("00:00:12,000", SrtTimeFormatter.formatFromMillis(12_000L))
        assertEquals("00:01:13,500", SrtTimeFormatter.formatFromMillis(73_500L))
        assertEquals("01:01:01,001", SrtTimeFormatter.formatFromMillis(3_661_001L))
    }

    @Test
    fun `formatFromMillis clamps negative values to zero`() {
        assertEquals("00:00:00,000", SrtTimeFormatter.formatFromMillis(-500L))
    }

    @Test
    fun `format and parse round trip for many values`() {
        val samples = listOf(0L, 1L, 999L, 12_000L, 73_500L, 3_661_001L, 359_999_000L)
        for (original in samples) {
            val formatted = SrtTimeFormatter.formatFromMillis(original)
            val parsed = SrtTimeFormatter.parseToMillis(formatted)
            assertEquals("Round trip failed for $original ms", original, parsed)
        }
    }

    @Test
    fun `parseToMillis rejects malformed timestamps`() {
        assertThrows(SrtParseException::class.java) {
            SrtTimeFormatter.parseToMillis("not a timestamp")
        }
        assertThrows(SrtParseException::class.java) {
            SrtTimeFormatter.parseToMillis("00:00:60,000")
        }
        assertThrows(SrtParseException::class.java) {
            SrtTimeFormatter.parseToMillis("0:0:12,000")
        }
        assertThrows(SrtParseException::class.java) {
            SrtTimeFormatter.parseToMillis("00:00:12.000")
        }
    }
}
