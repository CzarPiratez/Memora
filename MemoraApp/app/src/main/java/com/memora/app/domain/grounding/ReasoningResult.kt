package com.memora.app.domain.grounding

import com.memora.app.domain.memory.MemoryEvidenceId

/** Untrusted proposed claim from [ReasoningEngine] — must pass Verifier. */
data class ReasoningClaim(
    val statement: String,
    val citedEvidenceIds: List<MemoryEvidenceId>,
) {
    init {
        require(statement.isNotBlank()) { "A reasoning claim needs a statement." }
        require(citedEvidenceIds.isNotEmpty()) { "Every claim must cite evidence ids." }
    }
}

/** Untrusted model output (`GROUNDING_ARCHITECTURE.md` §8). */
data class ReasoningResult(
    val claims: List<ReasoningClaim>,
    val proposesAbstain: Boolean,
    val rawNotes: String? = null,
) {
    init {
        if (proposesAbstain) {
            require(claims.isEmpty()) { "Abstain proposals must not include claims." }
        }
    }
}
