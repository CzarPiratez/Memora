package com.memora.app.domain.asset

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Instant

class SourceAvailabilityObservationTest {
    @Test
    fun reachable_requires_open_succeeded() {
        val observation = SourceAvailabilityObservation(
            sourceId = SourceId("src"),
            sourceAssetKey = SourceAssetKey("asset"),
            status = SourceAvailabilityStatus.REACHABLE,
            cause = SourceAvailabilityCause.OPEN_SUCCEEDED,
            observedAt = Instant.parse("2026-09-20T12:00:00Z"),
        )
        assertEquals(SourceAvailabilityStatus.REACHABLE, observation.status)
    }

    @Test
    fun unknown_cannot_be_stored() {
        assertThrows(IllegalArgumentException::class.java) {
            SourceAvailabilityObservation(
                sourceId = SourceId("src"),
                sourceAssetKey = SourceAssetKey("asset"),
                status = SourceAvailabilityStatus.UNKNOWN,
                cause = SourceAvailabilityCause.OPEN_FAILED_UNREACHABLE,
                observedAt = Instant.parse("2026-09-20T12:00:00Z"),
            )
        }
    }

    @Test
    fun reachable_cannot_claim_open_failed() {
        assertThrows(IllegalArgumentException::class.java) {
            SourceAvailabilityObservation(
                sourceId = SourceId("src"),
                sourceAssetKey = SourceAssetKey("asset"),
                status = SourceAvailabilityStatus.REACHABLE,
                cause = SourceAvailabilityCause.OPEN_FAILED_UNREACHABLE,
                observedAt = Instant.parse("2026-09-20T12:00:00Z"),
            )
        }
    }
}
