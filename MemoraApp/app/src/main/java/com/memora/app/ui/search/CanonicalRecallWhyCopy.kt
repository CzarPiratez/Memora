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
        val clipped = if (excerpt.length <= 120) excerpt else excerpt.take(117) + "…"
        val displayLabel = friendlyDisplayLabel(result.label)
        val lines = buildList {
            add(WHY_THIS_RESULT_LABEL)
            add("You searched for \"$query\".")
            add("Matched ${assetTypeLabel(result.assetType)}: $displayLabel.")
            if (result.openPageNumber != null && result.assetType == AssetType.PDF) {
                add("Page ${result.openPageNumber} of the saved document.")
            }
            add("Saved text: \"$clipped\"")
            add(
                when (result.retrievalPath) {
                    CanonicalRecallRetrievalPath.KEYWORD ->
                        "Found by exact words in evidence saved on this phone."
                    CanonicalRecallRetrievalPath.MEANING ->
                        "Ranked by meaning from your saved memories on this phone."
                },
            )
            if (result.evidenceTokenBoosted) {
                add("Your cue words also appear in this saved text.")
            }
        }
        return lines.joinToString("\n")
    }

    internal fun friendlyDisplayLabel(label: String): String {
        val separator = label.indexOf('_')
        if (separator in 33..45 && label.substring(0, separator).all { it.isDigit() || it in 'a'..'f' }) {
            return label.substring(separator + 1)
        }
        return label
    }

    private fun assetTypeLabel(type: AssetType): String = when (type) {
        AssetType.PDF -> "PDF"
        AssetType.SCREENSHOT -> "screenshot"
        AssetType.PHOTO -> "photo"
        AssetType.NOTE -> "note"
    }
}
