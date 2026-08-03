package com.subtitleextender.data.parser

import com.subtitleextender.data.model.SubtitleEntry
import com.subtitleextender.util.SrtTimeFormatter

/**
 * Serializes a list of [SubtitleEntry] objects back into valid SRT file
 * content, preserving original indices, text, multi-line formatting, and the
 * caller-specified line-ending style.
 *
 * Uses a single [StringBuilder] rather than repeated string concatenation so
 * that writing stays fast for files with thousands of subtitle blocks.
 */
class SrtWriter {

    /**
     * Builds the full SRT file text for [entries].
     *
     * @param lineEnding the line-break sequence to use between lines, e.g.
     *   "\n" or "\r\n". See [com.subtitleextender.util.LineEndingDetector].
     */
    fun write(entries: List<SubtitleEntry>, lineEnding: String = "\n"): String {
        val builder = StringBuilder()

        entries.forEachIndexed { position, entry ->
            builder.append(entry.index).append(lineEnding)
            builder.append(SrtTimeFormatter.formatFromMillis(entry.startTimeMillis))
            builder.append(" --> ")
            builder.append(SrtTimeFormatter.formatFromMillis(entry.endTimeMillis))
            builder.append(lineEnding)
            builder.append(entry.text.replace("\n", lineEnding))
            builder.append(lineEnding)

            val isLastEntry = position == entries.lastIndex
            if (!isLastEntry) {
                builder.append(lineEnding)
            }
        }

        return builder.toString()
    }
}
