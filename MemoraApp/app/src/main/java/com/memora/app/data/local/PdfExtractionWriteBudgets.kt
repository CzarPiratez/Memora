package com.memora.app.data.local

/**
 * Provisional hard limits for durable PDF extraction writes.
 * Aligned with the temporary synthetic parser envelope until a measured production
 * policy replaces these values. Overflow must return FailedSafely with no partial rows.
 */
object PdfExtractionWriteBudgets {
    const val MAX_PAGE_COUNT = 32
    const val MAX_CHARS_PER_PAGE = 8_192
    const val MAX_TOTAL_CHARS = 65_536
    const val MAX_METADATA_ENTRIES = 32
    const val MAX_METADATA_NAME_CHARS = 128
    const val MAX_METADATA_VALUE_CHARS = 2_048
    const val MAX_TITLE_CHARS = 512

    /** Development ceiling for a single atomic persist on the Medium Phone emulator. */
    const val MAX_PERSIST_ELAPSED_MS = 15_000L

    fun evaluate(record: WriteBudgetInspection): WriteBudgetVerdict {
        if (record.pageCount !in 1..MAX_PAGE_COUNT) {
            return WriteBudgetVerdict.Rejected("page_count")
        }
        if (record.totalChars > MAX_TOTAL_CHARS) {
            return WriteBudgetVerdict.Rejected("total_chars")
        }
        if (record.maxCharsOnAnyPage > MAX_CHARS_PER_PAGE) {
            return WriteBudgetVerdict.Rejected("chars_per_page")
        }
        if (record.metadataEntryCount > MAX_METADATA_ENTRIES) {
            return WriteBudgetVerdict.Rejected("metadata_entries")
        }
        if (record.maxMetadataNameChars > MAX_METADATA_NAME_CHARS) {
            return WriteBudgetVerdict.Rejected("metadata_name_chars")
        }
        if (record.maxMetadataValueChars > MAX_METADATA_VALUE_CHARS) {
            return WriteBudgetVerdict.Rejected("metadata_value_chars")
        }
        if (record.titleChars > MAX_TITLE_CHARS) {
            return WriteBudgetVerdict.Rejected("title_chars")
        }
        return WriteBudgetVerdict.Accepted
    }

    data class WriteBudgetInspection(
        val pageCount: Int,
        val totalChars: Int,
        val maxCharsOnAnyPage: Int,
        val metadataEntryCount: Int,
        val maxMetadataNameChars: Int,
        val maxMetadataValueChars: Int,
        val titleChars: Int,
    )

    sealed interface WriteBudgetVerdict {
        data object Accepted : WriteBudgetVerdict
        data class Rejected(val reasonCode: String) : WriteBudgetVerdict
    }
}
