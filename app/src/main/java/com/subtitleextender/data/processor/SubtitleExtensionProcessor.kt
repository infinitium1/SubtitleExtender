package com.subtitleextender.data.processor

import com.subtitleextender.data.model.SubtitleEntry

/**
 * Applies the app's core rule: push every subtitle's end time forward by up
 * to a requested amount, without ever letting it collide with the next
 * subtitle, and without ever touching start times.
 *
 * ### Algorithm
 * For every subtitle at position `i`:
 * 1. Compute the desired end time: `currentEnd + extensionMillis`.
 * 2. If this is the **last** subtitle, there is nothing after it, so it
 *    always receives the full requested extension.
 * 3. Otherwise, look at subtitle `i + 1`'s **start time** (which is never
 *    modified, so it is a stable reference point regardless of processing
 *    order). The latest a subtitle may end without overlapping is exactly
 *    one millisecond before that start time.
 * 4. The applied end time is the desired end time, capped at that latest
 *    non-overlapping millisecond - and never allowed to fall *before* the
 *    subtitle's original end time, so a pathological, already-overlapping
 *    input file can never cause this function to shrink a cue.
 *
 * Because step 3 only ever reads (never writes) start times, subtitles can
 * be processed independently in a single linear pass; the result never
 * depends on processing order.
 *
 * This is a pure, Android-free function so it can be exhaustively unit
 * tested without any device, emulator, or instrumentation.
 */
object SubtitleExtensionProcessor {

    /**
     * Returns a new list where every entry's [SubtitleEntry.endTimeMillis]
     * has been extended by up to [extensionMillis], honoring the no-overlap
     * guarantee described above. Start times, text, and index values are
     * always copied through unchanged.
     *
     * @throws IllegalArgumentException if [extensionMillis] is negative.
     */
    fun extend(entries: List<SubtitleEntry>, extensionMillis: Long): List<SubtitleEntry> {
        require(extensionMillis >= 0) { "extensionMillis must not be negative" }
        if (entries.isEmpty()) return entries

        val result = ArrayList<SubtitleEntry>(entries.size)

        for (position in entries.indices) {
            val current = entries[position]
            val desiredEndMillis = current.endTimeMillis + extensionMillis

            val newEndMillis = if (position == entries.lastIndex) {
                // The last subtitle has no follower, so nothing constrains it.
                desiredEndMillis
            } else {
                val nextStartMillis = entries[position + 1].startTimeMillis
                val latestNonOverlappingEndMillis = nextStartMillis - 1L

                // Never extend past the next subtitle's start, and never
                // shrink below the entry's own original end time.
                maxOf(current.endTimeMillis, minOf(desiredEndMillis, latestNonOverlappingEndMillis))
            }

            result.add(current.copy(endTimeMillis = newEndMillis))
        }

        return result
    }
}
