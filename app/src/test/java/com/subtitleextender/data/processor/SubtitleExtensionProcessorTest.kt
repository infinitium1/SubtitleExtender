package com.subtitleextender.data.processor

import com.subtitleextender.data.model.SubtitleEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SubtitleExtensionProcessorTest {

    @Test
    fun `extends end time by the full amount when there is room`() {
        val entries = listOf(
            SubtitleEntry("1", startTimeMillis = 12_000L, endTimeMillis = 13_000L, text = "Hello")
        )

        val result = SubtitleExtensionProcessor.extend(entries, extensionMillis = 500L)

        assertEquals(12_000L, result[0].startTimeMillis)
        assertEquals(13_500L, result[0].endTimeMillis)
    }

    @Test
    fun `applies each of the four allowed durations correctly when room is unlimited`() {
        // 0.5s, 1s, 1.5s, 2s - the exact set of options offered in the UI.
        val durationsMs = listOf(500L, 1_000L, 1_500L, 2_000L)
        for (durationMs in durationsMs) {
            val entries = listOf(
                SubtitleEntry("1", startTimeMillis = 0L, endTimeMillis = 1_000L, text = "Only")
            )
            val result = SubtitleExtensionProcessor.extend(entries, extensionMillis = durationMs)
            assertEquals(
                "Extension of ${durationMs}ms did not apply in full when room was unlimited",
                1_000L + durationMs,
                result[0].endTimeMillis
            )
        }
    }

    @Test
    fun `never changes any start time`() {
        val entries = listOf(
            SubtitleEntry("1", startTimeMillis = 12_000L, endTimeMillis = 13_000L, text = "Hello"),
            SubtitleEntry("2", startTimeMillis = 20_000L, endTimeMillis = 21_000L, text = "World")
        )

        val result = SubtitleExtensionProcessor.extend(entries, extensionMillis = 2_000L)

        assertEquals(12_000L, result[0].startTimeMillis)
        assertEquals(20_000L, result[1].startTimeMillis)
    }

    @Test
    fun `shortens the extension to avoid overlapping the next subtitle (spec example)`() {
        // Exact scenario from the requirements: a naive +1s extension would
        // push 13,000 to 14,000, overlapping subtitle 2, which starts at
        // 13,400. The extension must be shortened instead, ending exactly
        // one millisecond before subtitle 2 begins.
        val entries = listOf(
            SubtitleEntry("1", startTimeMillis = 12_000L, endTimeMillis = 13_000L, text = "Hello"),
            SubtitleEntry("2", startTimeMillis = 13_400L, endTimeMillis = 15_000L, text = "How are you?")
        )

        val result = SubtitleExtensionProcessor.extend(entries, extensionMillis = 1_000L)

        assertEquals(13_399L, result[0].endTimeMillis)
        // The following subtitle must be completely untouched.
        assertEquals(13_400L, result[1].startTimeMillis)
        assertEquals(15_000L, result[1].endTimeMillis)
    }

    @Test
    fun `ends exactly one millisecond before the next subtitle's start for every duration option`() {
        val durationsMs = listOf(500L, 1_000L, 1_500L, 2_000L)
        for (durationMs in durationsMs) {
            val entries = listOf(
                SubtitleEntry("1", startTimeMillis = 0L, endTimeMillis = 1_000L, text = "A"),
                // Next subtitle starts only 100ms after this one ends, far
                // less than any of the allowed extensions, so every option
                // must be clamped down to the same 1ms-before-next boundary.
                SubtitleEntry("2", startTimeMillis = 1_100L, endTimeMillis = 2_000L, text = "B")
            )

            val result = SubtitleExtensionProcessor.extend(entries, extensionMillis = durationMs)

            assertEquals("Failed for ${durationMs}ms extension", 1_099L, result[0].endTimeMillis)
        }
    }

    @Test
    fun `never produces overlapping subtitles for any gap size`() {
        val gaps = listOf(0L, 1L, 50L, 100L, 400L, 999L, 1_000L, 5_000L)
        for (gap in gaps) {
            val entries = listOf(
                SubtitleEntry("1", startTimeMillis = 0L, endTimeMillis = 1_000L, text = "A"),
                SubtitleEntry("2", startTimeMillis = 1_000L + gap, endTimeMillis = 2_000L + gap, text = "B")
            )

            val result = SubtitleExtensionProcessor.extend(entries, extensionMillis = 2_000L)

            assertTrue(
                "Gap of ${gap}ms produced an overlap: end=${result[0].endTimeMillis}, " +
                    "nextStart=${result[1].startTimeMillis}",
                result[0].endTimeMillis < result[1].startTimeMillis
            )
        }
    }

    @Test
    fun `never exceeds the user-selected extension even with unlimited room`() {
        val entries = listOf(
            SubtitleEntry("1", startTimeMillis = 0L, endTimeMillis = 1_000L, text = "A"),
            SubtitleEntry("2", startTimeMillis = 1_000_000L, endTimeMillis = 1_001_000L, text = "B")
        )

        val result = SubtitleExtensionProcessor.extend(entries, extensionMillis = 500L)

        // Plenty of room exists (999,000ms gap), but the extension must never
        // exceed the 500ms the user selected.
        assertEquals(1_500L, result[0].endTimeMillis)
    }

    @Test
    fun `extends the last subtitle by the full amount regardless of what precedes it`() {
        val entries = listOf(
            SubtitleEntry("1", startTimeMillis = 0L, endTimeMillis = 1_000L, text = "A"),
            SubtitleEntry("2", startTimeMillis = 1_000L, endTimeMillis = 2_000L, text = "Last")
        )

        val result = SubtitleExtensionProcessor.extend(entries, extensionMillis = 2_000L)

        assertEquals(4_000L, result[1].endTimeMillis)
    }

    @Test
    fun `extends the last subtitle by the full amount for every duration option`() {
        val durationsMs = listOf(500L, 1_000L, 1_500L, 2_000L)
        for (durationMs in durationsMs) {
            val entries = listOf(
                SubtitleEntry("1", startTimeMillis = 0L, endTimeMillis = 1_000L, text = "A"),
                SubtitleEntry("2", startTimeMillis = 1_000L, endTimeMillis = 2_000L, text = "Last")
            )
            val result = SubtitleExtensionProcessor.extend(entries, extensionMillis = durationMs)
            assertEquals(2_000L + durationMs, result[1].endTimeMillis)
        }
    }

    @Test
    fun `never shrinks a subtitle even when there is zero room to extend`() {
        val entries = listOf(
            SubtitleEntry("1", startTimeMillis = 0L, endTimeMillis = 1_000L, text = "A"),
            SubtitleEntry("2", startTimeMillis = 1_000L, endTimeMillis = 2_000L, text = "B")
        )

        val result = SubtitleExtensionProcessor.extend(entries, extensionMillis = 2_000L)

        // The next subtitle starts exactly when this one ends - there is no
        // room at all, so the end time must stay exactly where it was.
        assertEquals(1_000L, result[0].endTimeMillis)
    }

    @Test
    fun `never shrinks a subtitle even if the input file already has an overlap`() {
        // Pathological input: subtitle 2 starts *before* subtitle 1 ends.
        // The processor must never make subtitle 1 shorter than it already
        // was - it simply cannot extend it at all in this case.
        val entries = listOf(
            SubtitleEntry("1", startTimeMillis = 0L, endTimeMillis = 5_000L, text = "A"),
            SubtitleEntry("2", startTimeMillis = 3_000L, endTimeMillis = 6_000L, text = "B")
        )

        val result = SubtitleExtensionProcessor.extend(entries, extensionMillis = 2_000L)

        assertEquals(5_000L, result[0].endTimeMillis)
    }

    @Test
    fun `preserves multiline subtitle text and index while changing only the end time`() {
        val entries = listOf(
            SubtitleEntry(
                index = "7",
                startTimeMillis = 5_000L,
                endTimeMillis = 6_000L,
                text = "Some text\nSecond line"
            )
        )

        val result = SubtitleExtensionProcessor.extend(entries, extensionMillis = 500L)

        assertEquals("7", result[0].index)
        assertEquals("Some text\nSecond line", result[0].text)
        assertEquals(5_000L, result[0].startTimeMillis)
        assertEquals(6_500L, result[0].endTimeMillis)
    }

    @Test
    fun `preserves UTF-8 Turkish characters in subtitle text untouched`() {
        val turkishText = "Günaydın, nasılsın?\nÖğretmen çiçek getirdi. İyi şanslar!"
        val entries = listOf(
            SubtitleEntry("1", startTimeMillis = 1_000L, endTimeMillis = 2_000L, text = turkishText)
        )

        val result = SubtitleExtensionProcessor.extend(entries, extensionMillis = 1_500L)

        assertEquals(turkishText, result[0].text)
        assertEquals(3_500L, result[0].endTimeMillis)
    }

    @Test
    fun `handles a subtitle with empty text without altering timing logic`() {
        // The parser itself rejects empty-text blocks (see SrtParserTest),
        // but the processor is a pure function over SubtitleEntry and should
        // not depend on text being non-empty - it only ever reads timing.
        val entries = listOf(
            SubtitleEntry("1", startTimeMillis = 0L, endTimeMillis = 1_000L, text = ""),
            SubtitleEntry("2", startTimeMillis = 1_300L, endTimeMillis = 2_000L, text = "B")
        )

        val result = SubtitleExtensionProcessor.extend(entries, extensionMillis = 1_000L)

        assertEquals("", result[0].text)
        assertEquals(1_299L, result[0].endTimeMillis)
    }

    @Test
    fun `handles an empty list`() {
        val result = SubtitleExtensionProcessor.extend(emptyList(), extensionMillis = 1_000L)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `handles a single subtitle with no neighbors`() {
        val entries = listOf(
            SubtitleEntry("1", startTimeMillis = 0L, endTimeMillis = 1_000L, text = "Only")
        )

        val result = SubtitleExtensionProcessor.extend(entries, extensionMillis = 2_000L)

        assertEquals(3_000L, result[0].endTimeMillis)
    }

    @Test
    fun `handles a large subtitle file with thousands of tightly packed blocks without any overlap`() {
        // Simulates a large, realistic file (well above the 10,000-block
        // performance target divided across this and other scenarios):
        // 10,000 subtitles, each starting 700ms after the previous one
        // starts and lasting 500ms, so a naive +1s extension would cascade
        // into overlaps across the entire file if the next-start-time cap
        // were not applied correctly.
        val entryCount = 10_000
        val entries = (0 until entryCount).map { i ->
            val start = i * 700L
            SubtitleEntry(
                index = (i + 1).toString(),
                startTimeMillis = start,
                endTimeMillis = start + 500L,
                text = "Line $i"
            )
        }

        val result = SubtitleExtensionProcessor.extend(entries, extensionMillis = 1_000L)

        assertEquals(entryCount, result.size)
        for (i in 0 until result.lastIndex) {
            assertTrue(
                "Overlap detected between entry $i and ${i + 1}",
                result[i].endTimeMillis < result[i + 1].startTimeMillis
            )
        }
        // Start times must be untouched throughout the whole chain, and the
        // very last entry must receive the full, uncapped extension.
        for (i in entries.indices) {
            assertEquals(entries[i].startTimeMillis, result[i].startTimeMillis)
        }
        assertEquals(entries.last().endTimeMillis + 1_000L, result.last().endTimeMillis)
    }

    @Test
    fun `rejects a negative extension`() {
        val entries = listOf(
            SubtitleEntry("1", startTimeMillis = 0L, endTimeMillis = 1_000L, text = "A")
        )

        assertThrows(IllegalArgumentException::class.java) {
            SubtitleExtensionProcessor.extend(entries, extensionMillis = -100L)
        }
    }
}
