package com.subtitleextender.util

/**
 * Builds the suggested output filename for an extended subtitle file, e.g.
 * "movie.srt" -> "movie_extended.srt".
 */
object SubtitleFileNaming {

    private const val SUFFIX = "_extended"
    private const val EXTENSION = ".srt"
    private const val FALLBACK_BASE_NAME = "subtitle"

    /**
     * Returns the suggested filename for the extended copy of
     * [originalFileName]. Falls back to a generic name if [originalFileName]
     * is blank (for example, if the document provider did not expose a
     * display name for the originally selected file).
     */
    fun buildExtendedFileName(originalFileName: String): String {
        val trimmedName = originalFileName.trim()
        if (trimmedName.isEmpty()) {
            return "$FALLBACK_BASE_NAME$SUFFIX$EXTENSION"
        }

        val dotIndex = trimmedName.lastIndexOf('.')
        val baseName = if (dotIndex > 0) trimmedName.substring(0, dotIndex) else trimmedName
        return "$baseName$SUFFIX$EXTENSION"
    }
}
