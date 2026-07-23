package com.memora.app.data.pdfbox.isolation

/**
 * Temporary bounds for repository-owned synthetic fixtures only.
 *
 * These are not a real-source policy or a production promise. Representative-device measurement
 * and a separately approved resource policy must replace this before ADR-017 can permit real
 * source parsing.
 */
internal object IsolatedPdfParserSyntheticResultPolicy {
    const val MAXIMUM_PAGE_COUNT = 32
    const val MAXIMUM_CHUNKS_PER_PAGE = 4
    const val MAXIMUM_PAGE_TEXT_CODE_UNITS = 8_192L
    const val MAXIMUM_TOTAL_TEXT_CODE_UNITS = 65_536L
    const val MAXIMUM_CHUNK_TEXT_CODE_UNITS = 2_048

    val limits = IsolatedPdfParserResultLimits(
        maximumPageCount = MAXIMUM_PAGE_COUNT,
        maximumChunksPerPage = MAXIMUM_CHUNKS_PER_PAGE,
        maximumPageTextCodeUnits = MAXIMUM_PAGE_TEXT_CODE_UNITS,
        maximumTotalTextCodeUnits = MAXIMUM_TOTAL_TEXT_CODE_UNITS,
    )
}
