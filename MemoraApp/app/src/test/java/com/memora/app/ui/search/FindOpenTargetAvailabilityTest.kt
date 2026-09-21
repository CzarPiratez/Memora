package com.memora.app.ui.search

import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceAvailabilityStatus
import com.memora.app.domain.asset.SourceId
import org.junit.Assert.assertEquals
import org.junit.Test

class FindOpenTargetAvailabilityTest {
    @Test
    fun missing_observation_is_unknown_not_unreachable() {
        val target = FindOpenTarget(sourceId = "photos", sourceAssetKey = "42")
        assertEquals(
            SourceAvailabilityStatus.UNKNOWN,
            target.availabilityIn(emptyMap()),
        )
        assertEquals(
            SourceAvailabilityStatus.UNREACHABLE,
            target.availabilityIn(
                mapOf(
                    AssetIdentity(SourceId("photos"), SourceAssetKey("42")) to
                        SourceAvailabilityStatus.UNREACHABLE,
                ),
            ),
        )
    }
}
