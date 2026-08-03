package com.subtitleextender.util

/**
 * Detects which line-ending convention a source SRT file used, so the
 * generated output can preserve it instead of silently normalizing every
 * file to a single style.
 */
object LineEndingDetector {

    private const val CRLF = "\r\n"
    private const val LF = "\n"

    /**
     * Returns "\r\n" if [content] contains at least one Windows-style line
     * break, otherwise returns "\n". This is a pragmatic heuristic: real SRT
     * files are consistent about their line-ending style, so checking for a
     * single occurrence of CRLF is sufficient and keeps parsing a single
     * fast pass over the content.
     */
    fun detect(content: String): String {
        return if (content.contains(CRLF)) CRLF else LF
    }
}
