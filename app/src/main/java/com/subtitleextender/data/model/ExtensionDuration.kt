package com.subtitleextender.data.model

/**
 * The selectable amount of time by which a subtitle's end timestamp may be
 * extended.
 *
 * The actual extension applied to any single subtitle can be smaller than
 * [milliseconds] if extending by the full amount would overlap the following
 * subtitle - see [com.subtitleextender.data.processor.SubtitleExtensionProcessor].
 * The extension is never larger than [milliseconds]; it is only ever shortened,
 * never lengthened, to avoid an overlap.
 */
enum class ExtensionDuration(val milliseconds: Long, val displayLabel: String) {
    HALF_SECOND(milliseconds = 500L, displayLabel = "0.5 s"),
    ONE_SECOND(milliseconds = 1_000L, displayLabel = "1 s"),
    ONE_AND_HALF_SECONDS(milliseconds = 1_500L, displayLabel = "1.5 s"),
    TWO_SECONDS(milliseconds = 2_000L, displayLabel = "2 s");

    companion object {
        /** The duration pre-selected when the app is first opened. */
        val DEFAULT = ONE_SECOND
    }
}
