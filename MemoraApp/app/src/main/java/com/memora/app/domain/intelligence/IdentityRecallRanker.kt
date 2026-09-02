package com.memora.app.domain.intelligence

/**
 * Pass-through ranker — stable descending sort by [RecallRankCandidate.score].
 *
 * FC-02 slice 1 baseline before cross-encoder adapter lands.
 */
object IdentityRecallRanker : RecallRanker {
    override fun availability(): CapabilityAvailability =
        CapabilityAvailability.Available(
            ModelVersionIdentity(
                modelId = "identity-recall-ranker",
                version = "1.0.0",
            ),
        )

    override fun limits(): CapabilityLimits? = null

    override fun rank(query: String, candidates: List<RecallRankCandidate>): RecallRankResult {
        require(query.isNotBlank()) { "Recall rank query cannot be blank." }
        if (candidates.isEmpty()) {
            return RecallRankResult.Unavailable("No candidates to rank.")
        }
        val ordered = candidates
            .sortedByDescending { it.score }
            .map { it.id }
        return RecallRankResult.Ranked(ordered)
    }
}
