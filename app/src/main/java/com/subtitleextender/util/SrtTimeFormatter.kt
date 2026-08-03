package com.subtitleextender.util

import com.subtitleextender.data.parser.SrtParseException

/**
 * Converts between SRT timestamp strings (`HH:MM:SS,mmm`) and their
 * millisecond representation.
 *
 * Kept as a standalone, Android-free object so it can be unit tested on the
 * plain JVM without any instrumentation or emulator.
 */
object SrtTimeFormatter {

    private const val MILLIS_PER_SECOND = 1_000L
    private const val MILLIS_PER_MINUTE = 60_000L
    private const val MILLIS_PER_HOUR = 3_600_000L

    private val TIMESTAMP_REGEX = Regex("""^(\d{2}):([0-5]\d):([0-5]\d),(\d{3})$""")

    /**
     * Parses a single SRT timestamp such as "00:00:12,000" into the total
     * number of milliseconds since 00:00:00,000.
     *
     * @throws SrtParseException if [timestamp] does not match the standard
     *   `HH:MM:SS,mmm` format.
     */
    fun parseToMillis(timestamp: String): Long {
        val trimmed = timestamp.trim()
        val match = TIMESTAMP_REGEX.matchEntire(trimmed)
            ?: throw SrtParseException("Invalid timestamp: '$timestamp'")

        val (hours, minutes, seconds, millis) = match.destructured
        return hours.toLong() * MILLIS_PER_HOUR +
            minutes.toLong() * MILLIS_PER_MINUTE +
            seconds.toLong() * MILLIS_PER_SECOND +
            millis.toLong()
    }

    /**
     * Formats [totalMillis] back into a standard SRT timestamp string
     * ("HH:MM:SS,mmm"), zero-padded. Negative values are clamped to zero,
     * which should never occur in practice given how subtitles are validated
     * and extended, but keeps this function total rather than partial.
     */
    fun formatFromMillis(totalMillis: Long): String {
        val clamped = totalMillis.coerceAtLeast(0L)
        val hours = clamped / MILLIS_PER_HOUR
        val minutes = (clamped % MILLIS_PER_HOUR) / MILLIS_PER_MINUTE
        val seconds = (clamped % MILLIS_PER_MINUTE) / MILLIS_PER_SECOND
        val millis = clamped % MILLIS_PER_SECOND
        return "%02d:%02d:%02d,%03d".format(hours, minutes, seconds, millis)
    }
}
