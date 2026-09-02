package com.memora.app.domain.grounding

import com.memora.app.domain.memory.MemoryEvidenceId

/** Product-facing grounded answer status (`GROUNDING_ARCHITECTURE.md` §10). */
enum class StructuredAnswerStatus {
    ANSWERED,
    INSUFFICIENT_EVIDENCE,
    CONFLICTING_EVIDENCE,
    CAPABILITY_UNAVAILABLE,
    CANCELLED,
    FAILED_SAFELY,
    SHOW_CANDIDATES_ONLY,
}

data class StructuredAnswerClaim(
    val statement: String,
    val citedEvidenceIds: List<MemoryEvidenceId>,
) {
    init {
        require(statement.isNotBlank())
        require(citedEvidenceIds.isNotEmpty())
    }
}

/**
 * Domain grounded answer — UI maps afterward; no generative prose outside claims.
 */
data class StructuredAnswer(
    val status: StructuredAnswerStatus,
    val completeness: AnswerCompleteness,
    val claims: List<StructuredAnswerClaim>,
    val limitations: List<String>,
    val suggestedNextActions: List<String>,
) {
    init {
        when (status) {
            StructuredAnswerStatus.ANSWERED ->
                require(claims.isNotEmpty()) { "ANSWERED requires at least one claim." }
            StructuredAnswerStatus.INSUFFICIENT_EVIDENCE,
            StructuredAnswerStatus.CONFLICTING_EVIDENCE,
            StructuredAnswerStatus.CAPABILITY_UNAVAILABLE,
            StructuredAnswerStatus.CANCELLED,
            StructuredAnswerStatus.FAILED_SAFELY,
            StructuredAnswerStatus.SHOW_CANDIDATES_ONLY,
            -> require(claims.isEmpty()) { "Non-ANSWERED status must not carry claims." }
        }
    }
}
