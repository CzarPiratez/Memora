package com.memora.app.domain.intelligence

/**
 * One rerank input — typically one Memory evidence passage or memory-level hit.
 *
 * [id] must be stable within a single [RecallRanker.rank] call.
 */
data class RecallRankCandidate(
    val id: String,
    val score: Float,
    val excerpt: String? = null,
) {
    init {
        require(id.isNotBlank()) { "Recall rank candidate id cannot be blank." }
        require(excerpt == null || excerpt.isNotBlank()) { "Excerpt when present cannot be blank." }
    }
}

sealed interface RecallRankResult {
    data class Ranked(val orderedIds: List<String>) : RecallRankResult {
        init {
            require(orderedIds.isNotEmpty()) { "Ranked result needs at least one id." }
        }
    }

    data class Unavailable(val reason: String) : RecallRankResult {
        init {
            require(reason.isNotBlank()) { "Unavailable reason cannot be blank." }
        }
    }
}
