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
 * Keyword / substring **candidate generation** into [CanonicalRecall] (ADR-049).
 * Product-facing Find ViewModels call [CanonicalRecall], not this use case
 * directly. MIG-07 PDF + screenshot + photo + note keyword Finds are cut over
 * (AssetType filters). Includes READY and STALE_REINDEX_REQUIRED revisions —
 * MIG-05 STALE means evidence *embeddings* need reindex; excerpts stay searchable.
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

        val tokens = MemoryEvidenceLiteralSearchSupport.queryTokens(query)
        val rows = when {
            tokens.isEmpty() -> {
                excerptSearch.searchByExcerpt(
                    escapedNeedle = MemoryEvidenceLiteralSearchSupport.escapeForLike(query),
                    assemblySchemaVersion = assemblySchemaVersion,
                    limit = limit,
                    assetType = assetType,
                )
            }
            tokens.size == 1 -> {
                excerptSearch.searchByExcerpt(
                    escapedNeedle = MemoryEvidenceLiteralSearchSupport.escapeForLike(tokens.single()),
                    assemblySchemaVersion = assemblySchemaVersion,
                    limit = limit,
                    assetType = assetType,
                )
            }
            else -> {
                searchRowsMatchingAllTokens(
                    tokens = tokens,
                    assemblySchemaVersion = assemblySchemaVersion,
                    limit = limit,
                    assetType = assetType,
                )
            }
        }

        val hits = rows.map { row ->
            val excerptNeedle = bestExcerptNeedle(tokens, row.excerpt) ?: query
            MemoryEvidenceSearchHit(
                memoryId = row.memoryId,
                revisionId = row.revisionId,
                evidenceId = row.evidenceId,
                kind = row.kind,
                locator = row.locator,
                excerpt = MemoryEvidenceLiteralSearchSupport.excerptAroundMatch(
                    storedExcerpt = row.excerpt,
                    needle = excerptNeedle,
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

    private suspend fun searchRowsMatchingAllTokens(
        tokens: List<String>,
        assemblySchemaVersion: String,
        limit: Int,
        assetType: AssetType?,
    ): List<MemoryEvidenceExcerptMatch> {
        val poolLimit = (limit * MemoryEvidenceLiteralSearchSupport.MULTI_TOKEN_SEARCH_POOL_MULTIPLIER)
            .coerceAtLeast(limit)
        val rowsByAsset = linkedMapOf<String, MutableList<MemoryEvidenceExcerptMatch>>()
        val tokensMatchedByAsset = linkedMapOf<String, MutableSet<String>>()

        for (token in tokens) {
            val tokenRows = excerptSearch.searchByExcerpt(
                escapedNeedle = MemoryEvidenceLiteralSearchSupport.escapeForLike(token),
                assemblySchemaVersion = assemblySchemaVersion,
                limit = poolLimit,
                assetType = assetType,
            )
            for (row in tokenRows) {
                if (!MemoryEvidenceLiteralSearchSupport.excerptContainsToken(row.excerpt, token)) {
                    continue
                }
                val assetKey = MemoryEvidenceLiteralSearchSupport.assetKey(
                    sourceId = row.sourceId,
                    sourceAssetKey = row.sourceAssetKey,
                )
                rowsByAsset.getOrPut(assetKey) { mutableListOf() }.add(row)
                tokensMatchedByAsset
                    .getOrPut(assetKey) { mutableSetOf() }
                    .add(token.lowercase())
            }
        }

        val requiredTokens = tokens.map { it.lowercase() }.toSet()
        return rowsByAsset
            .filter { (assetKey, _) ->
                tokensMatchedByAsset[assetKey]?.containsAll(requiredTokens) == true
            }
            .map { (assetKey, assetRows) ->
                assetRows
                    .distinctBy { it.evidenceId }
                    .maxWithOrNull(
                        compareBy<MemoryEvidenceExcerptMatch> { row ->
                            tokens.count { token ->
                                MemoryEvidenceLiteralSearchSupport.excerptContainsToken(
                                    row.excerpt,
                                    token,
                                )
                            }
                        }.thenBy { row ->
                            tokens.firstOrNull { token ->
                                MemoryEvidenceLiteralSearchSupport.excerptContainsToken(
                                    row.excerpt,
                                    token,
                                )
                            }?.let { token ->
                                row.excerpt.indexOf(token, ignoreCase = true)
                            } ?: Int.MAX_VALUE
                        },
                    )
                    ?: error("Asset $assetKey qualified for AND search but had no rows.")
            }
            .take(limit)
    }

    private fun bestExcerptNeedle(tokens: List<String>, excerpt: String): String? {
        if (tokens.size <= 1) return tokens.singleOrNull()
        return tokens.firstOrNull { token ->
            MemoryEvidenceLiteralSearchSupport.excerptContainsToken(excerpt, token)
        }
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
