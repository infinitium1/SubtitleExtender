package com.subtitleextender.util

import org.junit.Assert.assertEquals
import org.junit.Test

class SubtitleFileNamingTest {

    @Test
    fun `appends _extended before the srt extension`() {
        assertEquals("movie_extended.srt", SubtitleFileNaming.buildExtendedFileName("movie.srt"))
    }

    @Test
    fun `handles file names with multiple dots`() {
        assertEquals(
            "episode.s01e02_extended.srt",
            SubtitleFileNaming.buildExtendedFileName("episode.s01e02.srt")
        )
    }

    @Test
    fun `handles a file name without an extension`() {
        assertEquals("subtitle_extended.srt", SubtitleFileNaming.buildExtendedFileName("subtitle"))
    }

    @Test
    fun `falls back to a generic name for blank input`() {
        assertEquals("subtitle_extended.srt", SubtitleFileNaming.buildExtendedFileName("   "))
    }

    @Test
    fun `preserves Turkish characters in the base name`() {
        assertEquals(
            "altyazı_extended.srt",
            SubtitleFileNaming.buildExtendedFileName("altyazı.srt")
        )
    }
}
