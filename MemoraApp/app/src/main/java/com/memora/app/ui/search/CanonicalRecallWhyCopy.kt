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
        val clipped = if (excerpt.length <= 100) excerpt else excerpt.take(97) + "…"
        val displayLabel = friendlyDisplayLabel(result.label)
        return when (result.retrievalPath) {
            CanonicalRecallRetrievalPath.KEYWORD -> keywordWhy(
                query = query,
                displayLabel = displayLabel,
                result = result,
                clipped = clipped,
            )
            CanonicalRecallRetrievalPath.MEANING -> meaningWhy(
                query = query,
                label = result.label,
                excerpt = result.excerpt,
            )
        }
    }

    private fun keywordWhy(
        query: String,
        displayLabel: String,
        result: CanonicalRecallResult,
        clipped: String,
    ): String = buildList {
        add(WHY_THIS_RESULT_LABEL)
        add("You searched for \"$query\".")
        add("Matched ${assetTypeLabel(result.assetType)}: $displayLabel.")
        if (result.openPageNumber != null && result.assetType == AssetType.PDF) {
            add("Page ${result.openPageNumber} of the saved document.")
        }
        add("Saved text: \"$clipped\"")
        add("Found by exact words in evidence saved on this phone.")
    }.joinToString("\n")

    /**
     * Consumer dialect for meaning (scenario bar U5). The card already shows
     * the file, page, and snippet; this string only names which of [query]'s
     * words this excerpt actually contains (defect D-17). The live meaning
     * screen uses [MeaningWhy] against the whole Memory haystack — this path
     * has only the excerpt on [CanonicalRecallResult].
     */
    private fun meaningWhy(
        query: String,
        label: String,
        excerpt: String,
    ): String {
        val asked = MeaningWhy.askedPhrase(query)
        val fileIs = MeaningWhy.fileIs(label, excerpt)
        return MeaningWhy.relevanceText(asked, fileIs)
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
