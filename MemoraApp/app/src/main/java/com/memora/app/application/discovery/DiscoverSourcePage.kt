package com.memora.app.application.discovery

import com.memora.app.domain.discovery.AssetDiscoverySource
import com.memora.app.domain.discovery.DiscoveryCheckpointRepository
import com.memora.app.domain.discovery.DiscoveryFailure
import com.memora.app.domain.discovery.DiscoveryRequest
import com.memora.app.domain.discovery.DiscoveryResult
import com.memora.app.domain.discovery.SourceAccessState
import javax.inject.Inject

/**
 * Requests one bounded, source-owned discovery page and coordinates its safe storage.
 * This use case is inert until a caller explicitly supplies an approved source.
 */
class DiscoverSourcePage @Inject constructor(
    private val checkpointRepository: DiscoveryCheckpointRepository,
    private val processDiscoveryResult: ProcessDiscoveryResult,
) {
    suspend operator fun invoke(
        source: AssetDiscoverySource,
        batchSize: Int = DiscoveryRequest.DEFAULT_BATCH_SIZE,
    ): DiscoveryResult {
        val sourceId = source.capability.sourceId
        val discoveryResult = try {
            when (source.accessState()) {
                SourceAccessState.GRANTED -> {
                    val cursor = checkpointRepository.find(sourceId)
                    source.discover(DiscoveryRequest(cursor = cursor, batchSize = batchSize))
                }
                SourceAccessState.ACCESS_REQUIRED -> DiscoveryResult.AccessRequired
                SourceAccessState.ACCESS_REVOKED -> DiscoveryResult.AccessRevoked
                SourceAccessState.UNAVAILABLE -> unavailableSourceResult()
            }
        } catch (_: Exception) {
            unavailableSourceResult()
        }

        val verifiedResult = discoveryResult.takeUnless { result ->
            result is DiscoveryResult.Page && result.value.sourceId != sourceId
        } ?: DiscoveryResult.Failed(
            DiscoveryFailure(
                code = SOURCE_ID_MISMATCH,
                message = "Memora rejected a discovery page from an unexpected source.",
            ),
        )

        return processDiscoveryResult(verifiedResult)
    }

    private fun unavailableSourceResult() = DiscoveryResult.Failed(
        DiscoveryFailure(
            code = SOURCE_UNAVAILABLE,
            message = "Memora could not read this source. Please try again.",
        ),
    )

    private companion object {
        const val SOURCE_UNAVAILABLE = "discovery_source_unavailable"
        const val SOURCE_ID_MISMATCH = "discovery_source_id_mismatch"
    }
}
