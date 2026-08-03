package com.subtitleextender.data.parser

import com.subtitleextender.data.model.SubtitleEntry
import com.subtitleextender.util.SrtTimeFormatter

/**
 * Parses raw SRT file content into a list of [SubtitleEntry] objects.
 *
 * The parser is intentionally strict: index lines must be purely numeric and
 * every timestamp line must match the standard
 * `HH:MM:SS,mmm --> HH:MM:SS,mmm` format with a valid (end > start) range.
 * Any deviation results in an [SrtParseException] so the caller can surface a
 * clear "invalid file" message instead of silently producing incorrect
 * output.
 *
 * The whole file is parsed in a single forward pass using a plain line index
 * (no recursion, no repeated string concatenation), so parsing stays fast and
 * memory-efficient even for subtitle files with 10,000+ blocks.
 */
class SrtParser {

    /**
     * Parses [rawContent] (the full text of an .srt file) into an ordered
     * list of [SubtitleEntry].
     *
     * @throws SrtParseException if the content is empty, contains no valid
     *   subtitle blocks, or any block is malformed.
     */
    fun parse(rawContent: String): List<SubtitleEntry> {
        val content = rawContent.removePrefix(BYTE_ORDER_MARK)
        val lines = content.split(LINE_SPLIT_REGEX)
        val entries = ArrayList<SubtitleEntry>()

        var lineIndex = 0
        val lineCount = lines.size

        while (lineIndex < lineCount) {
            // Skip any blank separator lines between blocks (tolerating
            // files that have more than one blank line between entries).
            while (lineIndex < lineCount && lines[lineIndex].isBlank()) {
                lineIndex++
            }
            if (lineIndex >= lineCount) break

            // --- Index line ---
            val indexToken = lines[lineIndex].trim()
            if (!INDEX_REGEX.matches(indexToken)) {
                throw SrtParseException("Expected a numeric subtitle index but found: '$indexToken'")
            }
            lineIndex++
            if (lineIndex >= lineCount) {
                throw SrtParseException("File ended unexpectedly after index $indexToken")
            }

            // --- Timestamp line ---
            val timestampLine = lines[lineIndex]
            val timestampMatch = TIMESTAMP_LINE_REGEX.matchEntire(timestampLine.trim())
                ?: throw SrtParseException(
                    "Invalid timestamp line for subtitle $indexToken: '$timestampLine'"
                )
            lineIndex++

            val startMillis = SrtTimeFormatter.parseToMillis(timestampMatch.groupValues[1])
            val endMillis = SrtTimeFormatter.parseToMillis(timestampMatch.groupValues[2])
            if (endMillis <= startMillis) {
                throw SrtParseException(
                    "Subtitle $indexToken has an end time before or equal to its start time"
                )
            }

            // --- Text lines (one or more, until the next blank line or EOF) ---
            val textLines = ArrayList<String>()
            while (lineIndex < lineCount && lines[lineIndex].isNotBlank()) {
                textLines.add(lines[lineIndex])
                lineIndex++
            }
            if (textLines.isEmpty()) {
                throw SrtParseException("Subtitle $indexToken has no text content")
            }

            entries.add(
                SubtitleEntry(
                    index = indexToken,
                    startTimeMillis = startMillis,
                    endTimeMillis = endMillis,
                    text = textLines.joinToString("\n")
                )
            )
        }

        if (entries.isEmpty()) {
            throw SrtParseException("No subtitle entries were found in the file")
        }
        return entries
    }

    private companion object {
        const val BYTE_ORDER_MARK = "\uFEFF"
        val LINE_SPLIT_REGEX = Regex("\r\n|\r|\n")
        val INDEX_REGEX = Regex("""^\d+$""")
        val TIMESTAMP_LINE_REGEX = Regex(
            """^(\d{2}:\d{2}:\d{2},\d{3})\s*-->\s*(\d{2}:\d{2}:\d{2},\d{3}).*$"""
        )
    }
}
