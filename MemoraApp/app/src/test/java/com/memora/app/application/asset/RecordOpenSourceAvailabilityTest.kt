package com.memora.app.application.asset

import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceAvailabilityObservation
import com.memora.app.domain.asset.SourceAvailabilityStatus
import com.memora.app.domain.asset.SourceAvailabilityStore
import com.memora.app.domain.asset.SourceId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class RecordOpenSourceAvailabilityTest {
    private val clock = Clock.fixed(Instant.parse("2026-09-20T16:00:00Z"), ZoneOffset.UTC)
    private val store = InMemorySourceAvailabilityStore()
    private val record = RecordOpenSourceAvailability(store, clock)

    @Test
    fun failed_open_is_unreachable_and_successful_open_resurrects() = runTest {
        val source = SourceId("photos")
        val key = SourceAssetKey("42")

        record.unreachable(source.value, key.value)
        assertEquals(
            SourceAvailabilityStatus.UNREACHABLE,
            store.find(source, key)?.status,
        )

        record.reachable(source.value, key.value)
        assertEquals(
            SourceAvailabilityStatus.REACHABLE,
            store.find(source, key)?.status,
        )
        assertEquals(clock.instant(), store.find(source, key)?.observedAt)
    }

    @Test
    fun lookup_returns_unknown_when_nothing_was_observed() = runTest {
        val load = LoadSourceAvailability(store)
        val identity = AssetIdentity(SourceId("photos"), SourceAssetKey("missing"))
        assertEquals(
            SourceAvailabilityStatus.UNKNOWN,
            load(listOf(identity))[identity],
        )
        assertNull(store.find(identity.sourceId, identity.sourceAssetKey))
    }
}

internal class InMemorySourceAvailabilityStore : SourceAvailabilityStore {
    private val rows = linkedMapOf<AssetIdentity, SourceAvailabilityObservation>()

    override suspend fun find(
        sourceId: SourceId,
        sourceAssetKey: SourceAssetKey,
    ): SourceAvailabilityObservation? = rows[AssetIdentity(sourceId, sourceAssetKey)]

    override suspend fun findAll(
        identities: Collection<AssetIdentity>,
    ): Map<AssetIdentity, SourceAvailabilityObservation> =
        identities.mapNotNull { identity -> rows[identity]?.let { identity to it } }.toMap()

    override suspend fun save(observation: SourceAvailabilityObservation) {
        rows[AssetIdentity(observation.sourceId, observation.sourceAssetKey)] = observation
    }
}
