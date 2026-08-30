package com.memora.app.ui.search

import com.memora.app.domain.memory.CorpusCompletenessBlocked
import com.memora.app.domain.memory.CorpusCompletenessCounts
import com.memora.app.domain.memory.CorpusCompletenessSnapshot

/**
 * Plain-language indexed / pending / blocked copy for meaning-search corpus (FC-04).
 */
object CorpusHonestyCopy {
    fun summaryBody(snapshot: CorpusCompletenessSnapshot): String {
        val counts = snapshot.counts
        val indexedLine = indexedLine(counts)
        val pendingLine = pendingLine(counts)
        val blockedLine = snapshot.blocked?.let { blockedLine(it) }
        return buildList {
            add(indexedLine)
            if (pendingLine != null) add(pendingLine)
            if (blockedLine != null) add(blockedLine)
        }.joinToString(" ")
    }

    fun assetMemoryReadiness(counts: CorpusCompletenessCounts): String {
        val ready = plural(counts.memoriesReady, "Asset Memory", "Asset Memories")
        val pending = counts.memoriesPendingAssembly
        return if (pending > 0) {
            "$ready saved. $pending ${assetLabel(pending)} still need memory build " +
                "from saved facts on this phone."
        } else {
            if (counts.memoriesReady == 1) {
                "1 current evidence-backed Asset Memory is saved."
            } else {
                "${counts.memoriesReady} current evidence-backed Asset Memories are saved."
            }
        }
    }

    fun aiPackCorpusLine(snapshot: CorpusCompletenessSnapshot): String =
        summaryBody(snapshot) +
            " Tap Build to index the next batch. This is candidate meaning search — " +
            "not a measured AVAILABLE claim."

    private fun indexedLine(counts: CorpusCompletenessCounts): String {
        val memoryPart = plural(counts.meaningSummaryIndexed, "memory summary", "memory summaries")
        val evidencePart = plural(
            counts.meaningEvidenceIndexed,
            "evidence vector",
            "evidence vectors",
        )
        return "Indexed on this phone: $memoryPart and $evidencePart for meaning search."
    }

    private fun pendingLine(counts: CorpusCompletenessCounts): String? {
        val parts = buildList {
            if (counts.meaningIndexPending > 0) {
                add(
                    "${counts.meaningIndexPending} ${memoryLabel(counts.meaningIndexPending)} " +
                        "${if (counts.meaningIndexPending == 1) "awaits" else "await"} meaning indexing",
                )
            }
            if (counts.memoriesPendingAssembly > 0) {
                add(
                    "${counts.memoriesPendingAssembly} ${assetLabel(counts.memoriesPendingAssembly)} " +
                        "${if (counts.memoriesPendingAssembly == 1) "awaits" else "await"} memory build",
                )
            }
        }
        if (parts.isEmpty()) return null
        return "Pending: ${parts.joinToString("; ")}."
    }

    private fun blockedLine(blocked: CorpusCompletenessBlocked): String = when (blocked) {
        CorpusCompletenessBlocked.BuildMemoriesFirst ->
            "Blocked: build Asset Memories from saved facts before meaning search can help."
        CorpusCompletenessBlocked.BuildMeaningIndex ->
            "Blocked: build the meaning index from About on-device meaning search."
    }

    private fun plural(count: Int, singular: String, plural: String): String =
        if (count == 1) "1 $singular" else "$count $plural"

    private fun memoryLabel(count: Int): String =
        if (count == 1) "memory" else "memories"

    private fun assetLabel(count: Int): String =
        if (count == 1) "asset" else "assets"
}
