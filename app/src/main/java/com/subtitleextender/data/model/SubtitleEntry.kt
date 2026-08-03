package com.subtitleextender.data.model

/**
 * A single parsed subtitle cue from an SRT file.
 *
 * All timing values are stored as milliseconds elapsed since 00:00:00,000,
 * which makes arithmetic (extending, comparing) trivial and avoids repeated
 * string parsing. The original textual [index] is preserved verbatim rather
 * than stored as an [Int] so that re-serializing never renumbers a file.
 *
 * @property index the original block number exactly as it appeared in the
 *   source file (e.g. "1", "2", "3"). Never changed by this app.
 * @property startTimeMillis the cue's start time, in milliseconds. This value
 *   is never modified anywhere in the app - only end times may be extended.
 * @property endTimeMillis the cue's end time, in milliseconds. This is the
 *   only field [com.subtitleextender.data.processor.SubtitleExtensionProcessor]
 *   is allowed to change.
 * @property text the cue's text content. Multiple lines are joined with "\n"
 *   so that multi-line subtitles round-trip exactly through parse -> write.
 */
data class SubtitleEntry(
    val index: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val text: String
)
