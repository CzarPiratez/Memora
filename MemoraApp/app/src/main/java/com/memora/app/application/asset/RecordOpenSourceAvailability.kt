package com.memora.app.application.asset

import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceAvailabilityCause
import com.memora.app.domain.asset.SourceAvailabilityObservation
import com.memora.app.domain.asset.SourceAvailabilityStatus
import com.memora.app.domain.asset.SourceAvailabilityStore
import com.memora.app.domain.asset.SourceId
import java.time.Clock
import javax.inject.Inject

/**
 * Persists a confirmed Open outcome as source availability.
 *
 * Retryable CouldNotOpen is not recorded — a renderer flake must not mark a
 * live original unreachable. Successful Open resurrects a previous miss.
 */
class RecordOpenSourceAvailability(
    private val store: SourceAvailabilityStore,
    private val clock: Clock = Clock.systemUTC(),
) {
    @Inject
    constructor(store: SourceAvailabilityStore) : this(store, Clock.systemUTC())

    suspend fun reachable(sourceId: String, sourceAssetKey: String) {
        store.save(
            SourceAvailabilityObservation(
                sourceId = SourceId(sourceId),
                sourceAssetKey = SourceAssetKey(sourceAssetKey),
                status = SourceAvailabilityStatus.REACHABLE,
                cause = SourceAvailabilityCause.OPEN_SUCCEEDED,
                observedAt = clock.instant(),
            ),
        )
    }

    suspend fun unreachable(sourceId: String, sourceAssetKey: String) {
        store.save(
            SourceAvailabilityObservation(
                sourceId = SourceId(sourceId),
                sourceAssetKey = SourceAssetKey(sourceAssetKey),
                status = SourceAvailabilityStatus.UNREACHABLE,
                cause = SourceAvailabilityCause.OPEN_FAILED_UNREACHABLE,
                observedAt = clock.instant(),
            ),
        )
    }
}
