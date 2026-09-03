package com.memora.app.data.intelligence

import com.memora.app.domain.intelligence.CapabilityAvailability
import com.memora.app.domain.intelligence.CapabilityLimits
import com.memora.app.domain.intelligence.IdentityRecallRanker
import com.memora.app.domain.intelligence.ModelVersionIdentity
import com.memora.app.domain.intelligence.RecallRankCandidate
import com.memora.app.domain.intelligence.RecallRankResult
import com.memora.app.domain.intelligence.RecallRanker
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stage A product [RecallRanker]: ONNX when pack present, otherwise identity order.
 * Also falls back to identity when ONNX returns Unavailable (budget / load errors).
 */
@Singleton
class StageARecallRanker @Inject constructor(
    private val onnx: OnnxCrossEncoderRecallRanker,
) : RecallRanker {
    override fun availability(): CapabilityAvailability =
        when (val onnxAvailability = onnx.availability()) {
            is CapabilityAvailability.Available -> onnxAvailability
            is CapabilityAvailability.Unavailable ->
                CapabilityAvailability.Available(
                    ModelVersionIdentity(
                        modelId = "identity-recall-ranker",
                        version = "1.0.0-stage-a-fallback",
                    ),
                )
        }

    override fun limits(): CapabilityLimits? = onnx.limits() ?: IdentityRecallRanker.limits()

    override fun rank(query: String, candidates: List<RecallRankCandidate>): RecallRankResult {
        return when (val result = onnx.rank(query, candidates)) {
            is RecallRankResult.Ranked -> result
            is RecallRankResult.Unavailable -> IdentityRecallRanker.rank(query, candidates)
        }
    }
}
