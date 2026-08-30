package com.memora.app.ui.search

import com.memora.app.application.memory.CanonicalRecallResult
import com.memora.app.application.memory.CanonicalRecallRetrievalPath
import com.memora.app.domain.asset.AssetType

/**
 * Unified Why copy for all Canonical Recall Find hits (contract §3).
 *
 * Path-labeled per ADR-024; cites stored excerpt only; no Grounded Answers prose.
 */
object CanonicalRecallWhyCopy {
    const val WHY_THIS_RESULT_LABEL = "Why this result?"

    fun whyThisResult(result: CanonicalRecallResult, query: String): String {
        require(query.isNotBlank())
        val excerpt = result.excerpt.trim().replace(Regex("\\s+"), " ")
        val clipped = if (excerpt.length <= 160) excerpt else excerpt.take(157) + "…"
        val pathLine = when (result.retrievalPath) {
            CanonicalRecallRetrievalPath.KEYWORD ->
                "This is a keyword match on saved evidence text on this phone — " +
                    "not meaning-based recall."
            CanonicalRecallRetrievalPath.MEANING ->
                "This is candidate meaning ranking on this phone — not a measured " +
                    "AVAILABLE claim."
        }
        val pageLine = when {
            result.openPageNumber != null && result.assetType == AssetType.PDF ->
                " It matched PDF page ${result.openPageNumber}."
            else -> ""
        }
        val locatorLine = when {
            result.locator != null && result.openPageNumber == null ->
                " Locator: ${result.locator.value}."
            else -> ""
        }
        val boostLine = if (result.evidenceTokenBoosted) {
            " Rank also rose because your cue appears in this saved evidence text " +
                "(disclosed assist — still candidate meaning, not keyword Find alone)."
        } else {
            ""
        }
        val scoreLine = when (result.retrievalPath) {
            CanonicalRecallRetrievalPath.MEANING ->
                " Score is candidate cosine similarity on this phone — not a guarantee " +
                    "of full meaning match."
            CanonicalRecallRetrievalPath.KEYWORD -> ""
        }
        return "$WHY_THIS_RESULT_LABEL Your cue \"$query\" matched this saved " +
            "${assetTypeLabel(result.assetType)} \"${result.label}\": \"$clipped\"." +
            "$pageLine$locatorLine $pathLine$boostLine$scoreLine"
    }

    private fun assetTypeLabel(type: AssetType): String = when (type) {
        AssetType.PDF -> "PDF memory"
        AssetType.SCREENSHOT -> "screenshot memory"
        AssetType.PHOTO -> "photo memory"
        AssetType.NOTE -> "note memory"
    }
}
