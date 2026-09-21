package com.memora.app.application.asset

import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.asset.SourceAvailabilityStatus
import com.memora.app.domain.asset.SourceAvailabilityStore
import javax.inject.Inject

/** Joins last-known Open observations onto already-ranked Find hits. */
class LoadSourceAvailability @Inject constructor(
    private val store: SourceAvailabilityStore,
) {
    suspend operator fun invoke(
        identities: Collection<AssetIdentity>,
    ): Map<AssetIdentity, SourceAvailabilityStatus> {
        if (identities.isEmpty()) return emptyMap()
        val found = store.findAll(identities)
        return identities.associateWith { identity ->
            found[identity]?.status ?: SourceAvailabilityStatus.UNKNOWN
        }
    }
}
