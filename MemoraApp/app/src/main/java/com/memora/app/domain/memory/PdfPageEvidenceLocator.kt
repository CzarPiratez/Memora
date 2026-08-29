package com.memora.app.domain.memory

/**
 * Parses Memory evidence locators of the form `pdf:page:N` (1-based page).
 *
 * Used so meaning-search open can land on the page the Memory summary cites,
 * without inventing a query-best page. Also resolves page → [MemoryEvidence.id]
 * for MIG-05 dual-write (locator is never itself an evidence id).
 */
object PdfPageEvidenceLocator {
    private val PATTERN = Regex("""^pdf:page:(\d+)$""")

    fun formatLocator(pageNumber: Int): String {
        require(pageNumber > 0) { "PDF page numbers are 1-based." }
        return "pdf:page:$pageNumber"
    }

    fun parsePageNumber(locator: String): Int? {
        val match = PATTERN.matchEntire(locator.trim()) ?: return null
        val page = match.groupValues[1].toIntOrNull() ?: return null
        return page.takeIf { it > 0 }
    }

    fun firstPageNumber(locators: Iterable<String>): Int? =
        locators.asSequence().mapNotNull(::parsePageNumber).firstOrNull()

    /**
     * Returns the [MemoryEvidence.id] whose locator is `pdf:page:N` for
     * [pageNumber], or null when no matching evidence exists.
     *
     * Does not invent ids and must never treat the locator string as the id
     * ([DeterministicMemoryBuilder] assigns `e{n}`).
     */
    fun evidenceIdForPage(
        evidence: Iterable<MemoryEvidence>,
        pageNumber: Int,
    ): MemoryEvidenceId? {
        require(pageNumber > 0) { "PDF page numbers are 1-based." }
        return evidence.firstOrNull { item ->
            parsePageNumber(item.locator.value) == pageNumber
        }?.id
    }
}
