package com.memora.app.domain.memory

/**
 * Parses Memory evidence locators of the form `pdf:page:N` (1-based page).
 *
 * Used so meaning-search open can land on the page the Memory summary cites,
 * without inventing a query-best page.
 */
object PdfPageEvidenceLocator {
    private val PATTERN = Regex("""^pdf:page:(\d+)$""")

    fun parsePageNumber(locator: String): Int? {
        val match = PATTERN.matchEntire(locator.trim()) ?: return null
        val page = match.groupValues[1].toIntOrNull() ?: return null
        return page.takeIf { it > 0 }
    }

    fun firstPageNumber(locators: Iterable<String>): Int? =
        locators.asSequence().mapNotNull(::parsePageNumber).firstOrNull()
}
