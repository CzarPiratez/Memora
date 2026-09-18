package com.memora.app.application.intelligence

import android.util.Log
import com.memora.app.BuildConfig
import com.memora.app.domain.intelligence.MeaningRecallCue
import com.memora.app.domain.intelligence.RecallPrecision
import java.util.Locale

/**
 * Phase 0 live-path diagnostic for D-20. Counts and short labels only — no
 * stored excerpts, no Room row, no upload.
 *
 * Does not change ranking. [emit] is a no-op in release builds.
 */
object MeaningSearchTrace {
    const val LOG_TAG = "MeaningSearchTrace"

    private const val MAX_DROPPED_LABELS = 8
    private const val MAX_LABEL_CHARS = 40

    data class Snapshot(
        val outcomeKind: String,
        val rawQuery: String,
        val contentTokens: List<String>,
        val vectorsScanned: Int,
        val survivedFloor: Int,
        val assetsAfterCollapse: Int,
        val admitted: Int,
        val poolTruncated: Boolean,
        val admittedTopCosine: Float?,
        val tier: String,
        val droppedByTier: Int,
        val droppedByTierLabels: List<String>,
        val shown: Int,
        val shownTopCosine: Float?,
        val capTruncated: Boolean,
        val latencyMs: Long,
    )

    fun shortLabel(label: String): String =
        label.trim().replace(WHITESPACE, " ").take(MAX_LABEL_CHARS)

    fun droppedLabels(hits: List<MeaningSearchHit>): List<String> =
        hits.take(MAX_DROPPED_LABELS).map { shortLabel(it.label) }

    fun toLogLine(snapshot: Snapshot): String = buildString {
        append("q=")
        append(snapshot.rawQuery)
        append(" tokens=")
        append(snapshot.contentTokens.joinToString(",").ifEmpty { "-" })
        append(" scanned=")
        append(snapshot.vectorsScanned)
        append(" floor=")
        append(snapshot.survivedFloor)
        append(" collapse=")
        append(snapshot.assetsAfterCollapse)
        append(" admitted=")
        append(snapshot.admitted)
        append(" poolTruncated=")
        append(snapshot.poolTruncated)
        append(" admittedTopCosine=")
        append(formatCosine(snapshot.admittedTopCosine))
        append(" tier=")
        append(snapshot.tier)
        append(" droppedByTier=")
        append(snapshot.droppedByTier)
        append(" dropped=[")
        append(snapshot.droppedByTierLabels.joinToString(";"))
        append("] shown=")
        append(snapshot.shown)
        append(" shownTopCosine=")
        append(formatCosine(snapshot.shownTopCosine))
        append(" capTruncated=")
        append(snapshot.capTruncated)
        append(" latencyMs=")
        append(snapshot.latencyMs)
        append(" outcome=")
        append(snapshot.outcomeKind)
    }

    fun emit(snapshot: Snapshot) {
        if (!BuildConfig.DEBUG) return
        Log.i(LOG_TAG, toLogLine(snapshot))
    }

    fun fromLivePath(
        rawQuery: String,
        candidate: MeaningSearchOutcome,
        ranked: MeaningSearchOutcome,
        shown: MeaningSearchOutcome,
        latencyMs: Long,
    ): Snapshot {
        val tokens = MeaningRecallCue.contentTokens(rawQuery)
        val pool = (candidate as? MeaningSearchOutcome.Matches)?.debugTrace
        val rankedMatches = ranked as? MeaningSearchOutcome.Matches
        val shownMatches = shown as? MeaningSearchOutcome.Matches
        return Snapshot(
            outcomeKind = outcomeKind(shown),
            rawQuery = MeaningRecallCue.displayQuery(rawQuery).ifEmpty { rawQuery.trim() },
            contentTokens = tokens,
            vectorsScanned = pool?.vectorsScanned ?: 0,
            survivedFloor = pool?.survivedFloor ?: 0,
            assetsAfterCollapse = pool?.assetsAfterCollapse ?: 0,
            admitted = pool?.admitted ?: 0,
            poolTruncated = pool?.poolTruncated ?: false,
            admittedTopCosine = pool?.admittedTopCosine,
            tier = tierName(shownMatches?.precision ?: rankedMatches?.precision),
            droppedByTier = rankedMatches?.debugTrace?.droppedByTier ?: 0,
            droppedByTierLabels = rankedMatches?.debugTrace?.droppedByTierLabels.orEmpty(),
            shown = shownMatches?.hits?.size ?: 0,
            shownTopCosine = shownMatches?.hits?.maxOfOrNull { it.cosine },
            capTruncated = shownMatches?.limitReached ?: false,
            latencyMs = latencyMs.coerceAtLeast(0L),
        )
    }

    fun withPool(
        vectorsScanned: Int,
        survivedFloor: Int,
        assetsAfterCollapse: Int,
        admittedHits: List<MeaningSearchHit>,
        poolTruncated: Boolean,
    ): MeaningSearchDebugTrace = MeaningSearchDebugTrace(
        vectorsScanned = vectorsScanned,
        survivedFloor = survivedFloor,
        assetsAfterCollapse = assetsAfterCollapse,
        admitted = admittedHits.size,
        poolTruncated = poolTruncated,
        admittedTopCosine = admittedHits.maxOfOrNull { it.cosine },
    )

    fun withTierDrops(
        current: MeaningSearchDebugTrace?,
        before: List<MeaningSearchHit>,
        after: List<MeaningSearchHit>,
    ): MeaningSearchDebugTrace {
        val dropped = before.filter { hit ->
            after.none { kept -> kept.revisionId == hit.revisionId }
        }
        return (current ?: MeaningSearchDebugTrace()).copy(
            droppedByTier = dropped.size,
            droppedByTierLabels = droppedLabels(dropped),
        )
    }

    private fun outcomeKind(outcome: MeaningSearchOutcome): String = when (outcome) {
        MeaningSearchOutcome.BlankQuery -> "BlankQuery"
        is MeaningSearchOutcome.EngineUnavailable -> "EngineUnavailable"
        is MeaningSearchOutcome.NothingIndexed -> "NothingIndexed"
        is MeaningSearchOutcome.Failed -> "Failed"
        is MeaningSearchOutcome.Matches -> "Matches"
    }

    private fun tierName(precision: RecallPrecision?): String = when (precision) {
        null -> "-"
        RecallPrecision.Exact -> "Exact"
        is RecallPrecision.Partial -> "Partial"
        is RecallPrecision.MeaningOnly -> "MeaningOnly"
    }

    private fun formatCosine(value: Float?): String =
        if (value == null) "-" else String.format(Locale.US, "%.3f", value)

    private val WHITESPACE = Regex("""\s+""")
}

/**
 * Counts collected on the live meaning path. Never holds excerpts.
 */
data class MeaningSearchDebugTrace(
    val vectorsScanned: Int = 0,
    val survivedFloor: Int = 0,
    val assetsAfterCollapse: Int = 0,
    val admitted: Int = 0,
    val poolTruncated: Boolean = false,
    val admittedTopCosine: Float? = null,
    val droppedByTier: Int = 0,
    val droppedByTierLabels: List<String> = emptyList(),
)
