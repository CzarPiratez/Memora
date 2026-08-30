package com.memora.app.application.memory

import com.memora.app.domain.asset.AssetType
import javax.inject.Inject

/**
 * App product-facing retrieval boundary (ADR-049).
 *
 * Thin façade: KEYWORD candidate generation delegates to [SearchMemoryEvidence].
 * PDF / screenshot / photo / note keyword Finds enter here. Meaning Find (L8)
 * and local ranking (L7) remain outside until MIG-07B.
 *
 * Does not implement RecallRanker, Grounded Answers, or shared multi-path ranking.
 */
class CanonicalRecall @Inject constructor(
    private val searchMemoryEvidence: SearchMemoryEvidence,
) {
    /**
     * Keyword / literal recall over stored Memory evidence.
     *
     * @param assetType optional scope for per-asset Find screens; null searches
     *   all asset types.
     */
    suspend operator fun invoke(
        rawQuery: String,
        limit: Int = MemoryEvidenceLiteralSearchSupport.MAX_RESULTS,
        assemblySchemaVersion: String = SearchMemoryEvidence.DEFAULT_ASSEMBLY_SCHEMA,
        assetType: AssetType? = null,
    ): MemoryEvidenceSearchOutcome =
        searchMemoryEvidence(
            rawQuery = rawQuery,
            limit = limit,
            assemblySchemaVersion = assemblySchemaVersion,
            assetType = assetType,
        )
}
