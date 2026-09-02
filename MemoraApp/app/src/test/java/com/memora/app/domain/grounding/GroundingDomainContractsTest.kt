package com.memora.app.domain.grounding

import com.memora.app.domain.memory.MemoryEvidenceId
import com.memora.app.domain.memory.MemoryRevisionId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GroundingDomainContractsTest {
    @Test
    fun evidencePackage_requiresEntries() {
        val entry = EvidencePackageEntry(
            evidenceId = MemoryEvidenceId("e1"),
            revisionId = MemoryRevisionId("rev-1"),
            locator = "pdf:page:1",
            excerpt = "bounded excerpt",
            retrievalPath = RetrievalPathLabel.MEANING,
            selectionRank = 0,
        )
        val pkg = EvidencePackage(
            packageId = "pkg-1",
            schemaVersion = "grounding-v1",
            task = ReasoningTask.ANSWER_QUESTION,
            question = GroundedQuestion("What is the total?"),
            entries = listOf(entry),
            completeness = AnswerCompleteness.PARTIAL,
            corpusCoverageHint = "Indexed 4 of 6 PDF pages",
        )
        assertEquals(setOf(MemoryEvidenceId("e1")), pkg.evidenceIds)
    }

    @Test
    fun reasoningResult_abstainHasNoClaims() {
        val result = ReasoningResult(claims = emptyList(), proposesAbstain = true)
        assertTrue(result.proposesAbstain)
    }

    @Test
    fun structuredAnswer_answeredRequiresClaims() {
        val answer = StructuredAnswer(
            status = StructuredAnswerStatus.ANSWERED,
            completeness = AnswerCompleteness.PARTIAL,
            claims = listOf(
                StructuredAnswerClaim(
                    statement = "Total is 42.",
                    citedEvidenceIds = listOf(MemoryEvidenceId("e1")),
                ),
            ),
            limitations = listOf("Not financial advice."),
            suggestedNextActions = emptyList(),
        )
        assertEquals(StructuredAnswerStatus.ANSWERED, answer.status)
    }
}
