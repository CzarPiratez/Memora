package com.memora.app.domain.grounding

import com.memora.app.domain.memory.MemoryEvidenceId
import com.memora.app.domain.memory.MemoryRevisionId

/** How a passage entered the evidence package (retrieval path label). */
enum class RetrievalPathLabel {
    KEYWORD,
    MEANING,
    PAGE,
    ANCHOR,
}

/**
 * One bounded excerpt permitted for reasoning — built only from stored evidence.
 */
data class EvidencePackageEntry(
    val evidenceId: MemoryEvidenceId,
    val revisionId: MemoryRevisionId,
    val locator: String,
    val excerpt: String,
    val retrievalPath: RetrievalPathLabel,
    val selectionRank: Int,
) {
    init {
        require(locator.isNotBlank()) { "Evidence package entry needs a locator." }
        require(excerpt.isNotBlank()) { "Evidence package entry needs an excerpt." }
        require(selectionRank >= 0) { "Selection rank must be non-negative." }
    }
}

/**
 * Immutable evidence package (`GROUNDING_ARCHITECTURE.md` §7).
 *
 * Ephemeral by default in v1 — not a durable user history row.
 */
data class EvidencePackage(
    val packageId: String,
    val schemaVersion: String,
    val task: ReasoningTask,
    val question: GroundedQuestion,
    val entries: List<EvidencePackageEntry>,
    val completeness: AnswerCompleteness,
    val corpusCoverageHint: String?,
) {
    init {
        require(packageId.isNotBlank())
        require(schemaVersion.isNotBlank())
        require(entries.isNotEmpty()) { "Evidence package must include at least one entry." }
    }

    val evidenceIds: Set<MemoryEvidenceId>
        get() = entries.map { it.evidenceId }.toSet()
}
