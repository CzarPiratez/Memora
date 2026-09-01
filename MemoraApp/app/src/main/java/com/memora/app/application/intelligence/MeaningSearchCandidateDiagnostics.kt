package com.memora.app.application.intelligence

/**
 * Operator signal when an indexed meaning corpus yields no candidates.
 *
 * Pure Kotlin — safe to unit test. Logged at the search boundary when the
 * embedding stores are non-empty but every record was dropped.
 */
internal data class MeaningSearchCandidateDropStats(
    var missingLookup: Int = 0,
    var dimensionMismatch: Int = 0,
    var belowMinScore: Int = 0,
    var blankLabelOrSummary: Int = 0,
    var missingEvidenceRow: Int = 0,
    var blankExcerpt: Int = 0,
) {
    val totalDropped: Int
        get() = missingLookup + dimensionMismatch + belowMinScore +
            blankLabelOrSummary + missingEvidenceRow + blankExcerpt

    fun hasDrops(): Boolean = totalDropped > 0
}

internal fun MeaningSearchCandidateDropStats.toLogMessage(
    summaryIndexed: Int,
    evidenceIndexed: Int,
    query: String,
): String = buildString {
    append("indexed corpus produced zero candidates for query=")
    append(query)
    append("; summaries=")
    append(summaryIndexed)
    append(" evidenceVectors=")
    append(evidenceIndexed)
    append("; drops=")
    append("missingLookup=")
    append(missingLookup)
    append(", dimensionMismatch=")
    append(dimensionMismatch)
    append(", belowMinScore=")
    append(belowMinScore)
    append(", blankLabelOrSummary=")
    append(blankLabelOrSummary)
    append(", missingEvidenceRow=")
    append(missingEvidenceRow)
    append(", blankExcerpt=")
    append(blankExcerpt)
}
