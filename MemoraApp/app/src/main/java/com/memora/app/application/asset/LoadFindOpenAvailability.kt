package com.memora.app.application.asset

import com.memora.app.application.notes.LoadNoteOpenClassAvailability
import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.asset.SourceAvailabilityStatus
import javax.inject.Inject

/**
 * Last-known Open reachability for Find cards: stored observations first,
 * then note Open-class from local rows. Never a ranking input.
 */
class LoadFindOpenAvailability @Inject constructor(
    private val stored: LoadSourceAvailability,
    private val notes: LoadNoteOpenClassAvailability,
) {
    suspend operator fun invoke(
        identities: Collection<AssetIdentity>,
    ): Map<AssetIdentity, SourceAvailabilityStatus> =
        notes.enrich(identities, stored(identities))
}
