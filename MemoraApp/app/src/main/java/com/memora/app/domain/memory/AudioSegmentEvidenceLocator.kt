package com.memora.app.domain.memory

/**
 * Parses Memory evidence locators of the form `audio:segment:START_MS:END_MS`.
 *
 * Millisecond offsets are relative to the start of the permitted audio Asset.
 * Reserved for post-MVP audio/meeting adapters — same pattern as
 * [PdfPageEvidenceLocator] so meaning-search open can land on a time range
 * without schema churn.
 */
object AudioSegmentEvidenceLocator {
    private val PATTERN = Regex("""^audio:segment:(\d+):(\d+)$""")

    fun formatLocator(startMs: Long, endMs: Long): String {
        require(startMs >= 0) { "Audio segment start must be non-negative." }
        require(endMs > startMs) { "Audio segment end must be after start." }
        return "audio:segment:$startMs:$endMs"
    }

    data class SegmentRange(val startMs: Long, val endMs: Long)

    fun parseSegment(locator: String): SegmentRange? {
        val match = PATTERN.matchEntire(locator.trim()) ?: return null
        val start = match.groupValues[1].toLongOrNull() ?: return null
        val end = match.groupValues[2].toLongOrNull() ?: return null
        if (start < 0 || end <= start) return null
        return SegmentRange(startMs = start, endMs = end)
    }

    fun evidenceIdForSegment(
        evidence: Iterable<MemoryEvidence>,
        startMs: Long,
        endMs: Long,
    ): MemoryEvidenceId? {
        require(startMs >= 0 && endMs > startMs)
        return evidence.firstOrNull { item ->
            parseSegment(item.locator.value)?.let { range ->
                range.startMs == startMs && range.endMs == endMs
            } == true
        }?.id
    }
}
