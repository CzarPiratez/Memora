package com.memora.app.application.memory

import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.memory.EvidenceLocator
import com.memora.app.domain.memory.MemoryEvidenceId
import com.memora.app.domain.memory.MemoryEvidenceKind
import com.memora.app.domain.memory.MemoryId
import com.memora.app.domain.memory.MemoryRevisionId
import com.memora.app.domain.memory.PdfPageEvidenceLocator
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Literal search over stored [com.memora.app.domain.memory.MemoryEvidence] excerpts.
 *
 * Keyword / substring candidate generation into future Canonical Recall (ADR-049).
 * MIG-07 PDF keyword Find is cut over to this use case (AssetType.PDF filter).
 * Screenshot / photo / note keyword Finds remain on Live L2–L4 until authorized.
 */
class SearchMemoryEvidence @Inject constructor(
    private val excerptSearch: MemoryEvidenceExcerptSearch,
) {
    /**
     * @param assetType optional scope for per-asset Find screens; null searches all
     *   asset types (MIG-06 default).
     */
    suspend operator fun invoke(
        rawQuery: String,
        limit: Int = MemoryEvidenceLiteralSearchSupport.MAX_RESULTS,
        assemblySchemaVersion: String = DEFAULT_ASSEMBLY_SCHEMA,
        assetType: AssetType? = null,
    ): MemoryEvidenceSearchOutcome = withContext(Dispatchers.IO) {
        require(limit > 0)
        val query = MemoryEvidenceLiteralSearchSupport.normalizeQuery(rawQuery)
            ?: return@withContext MemoryEvidenceSearchOutcome.BlankQuery

        if (excerptSearch.countCurrentReadyEvidence(assemblySchemaVersion, assetType) == 0) {
            return@withContext MemoryEvidenceSearchOutcome.NothingSavedToSearch(query = query)
        }

        val rows = excerptSearch.searchByExcerpt(
            escapedNeedle = MemoryEvidenceLiteralSearchSupport.escapeForLike(query),
            assemblySchemaVersion = assemblySchemaVersion,
            limit = limit,
            assetType = assetType,
        )

        val hits = rows.map { row ->
            MemoryEvidenceSearchHit(
                memoryId = row.memoryId,
                revisionId = row.revisionId,
                evidenceId = row.evidenceId,
                kind = row.kind,
                locator = row.locator,
                excerpt = MemoryEvidenceLiteralSearchSupport.excerptAroundMatch(
                    storedExcerpt = row.excerpt,
                    needle = query,
                ),
                sourceId = row.sourceId,
                sourceAssetKey = row.sourceAssetKey,
                assetType = row.assetType,
                label = row.displayLabel,
                openPageNumber = PdfPageEvidenceLocator.parsePageNumber(row.locator.value),
                retrievalPath = MemoryEvidenceRetrievalPath.KEYWORD,
            )
        }

        MemoryEvidenceSearchOutcome.Matches(
            query = query,
            hits = hits,
            limitReached = hits.size >= limit,
        )
    }

    companion object {
        /** Matches [com.memora.app.domain.intelligence.DeterministicMemoryBuilder.ASSEMBLY_SCHEMA]. */
        const val DEFAULT_ASSEMBLY_SCHEMA = "asset-memory-facts-v4"
    }
}

/**
 * Honesty label for how this candidate was generated.
 *
 * Aligns with DRAFT `CANONICAL_RECALL_RESULT_CONTRACT` path vocabulary.
 * MIG-06 emits [KEYWORD] only.
 */
enum class MemoryEvidenceRetrievalPath {
    KEYWORD,
}

/**
 * One literal evidence hit with provenance for future MIG-07 Why + open-original.
 *
 * Carries Memory identity, evidence identity, locator, display excerpt, and
 * cheap asset identity — enough to cite Why and open the original without
 * re-querying extraction tables at Find time.
 */
data class MemoryEvidenceSearchHit(
    val memoryId: MemoryId,
    val revisionId: MemoryRevisionId,
    val evidenceId: MemoryEvidenceId,
    val kind: MemoryEvidenceKind,
    val locator: EvidenceLocator,
    val excerpt: String,
    val sourceId: SourceId,
    val sourceAssetKey: SourceAssetKey,
    val assetType: AssetType,
    val label: String,
    val openPageNumber: Int? = null,
    val retrievalPath: MemoryEvidenceRetrievalPath = MemoryEvidenceRetrievalPath.KEYWORD,
) {
    init {
        require(label.isNotBlank()) { "A memory-evidence search hit needs a label." }
        require(excerpt.isNotBlank()) { "A memory-evidence search hit needs an excerpt." }
        require(openPageNumber == null || openPageNumber > 0) {
            "Open page must be positive when present."
        }
    }
}

sealed interface MemoryEvidenceSearchOutcome {
    data object BlankQuery : MemoryEvidenceSearchOutcome

    /** Normalized query was submitted, but no current READY evidence exists to search. */
    data class NothingSavedToSearch(
        val query: String,
    ) : MemoryEvidenceSearchOutcome {
        init {
            require(query.isNotBlank()) {
                "Nothing-saved outcome needs the submitted search query."
            }
        }
    }

    data class Matches(
        val query: String,
        val hits: List<MemoryEvidenceSearchHit>,
        val limitReached: Boolean,
    ) : MemoryEvidenceSearchOutcome {
        init {
            require(query.isNotBlank()) {
                "A memory-evidence search match list needs the query."
            }
            if (limitReached) {
                require(hits.isNotEmpty()) {
                    "limitReached requires at least one hit."
                }
            }
        }
    }
}
