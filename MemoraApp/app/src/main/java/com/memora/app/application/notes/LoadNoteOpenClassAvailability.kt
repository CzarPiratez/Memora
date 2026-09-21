package com.memora.app.application.notes

import com.memora.app.application.asset.RecordOpenSourceAvailability
import com.memora.app.domain.asset.Asset
import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.asset.AssetRepository
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAvailabilityStatus
import com.memora.app.domain.notes.NotePageOpenTargetRepository
import com.memora.app.domain.notes.NotesProviderSession
import com.memora.app.domain.notes.NotesProviderTokenVault
import javax.inject.Inject

/**
 * Fills UNKNOWN note availability from local UNFYND rows and the vaulted
 * grant. Never calls Graph or [OneNoteInteractiveAuth.ensureSession].
 */
class LoadNoteOpenClassAvailability(
    private val findAsset: suspend (AssetIdentity) -> Asset?,
    private val hasOpenTarget: suspend (AssetIdentity) -> Boolean,
    private val vaultedGrantPresent: () -> Boolean,
    private val persistUnreachable: suspend (AssetIdentity) -> Unit,
) {
    @Inject
    constructor(
        assets: AssetRepository,
        openTargets: NotePageOpenTargetRepository,
        tokenVault: NotesProviderTokenVault,
        recordOpenSourceAvailability: RecordOpenSourceAvailability,
    ) : this(
        findAsset = { assets.find(it)?.asset },
        hasOpenTarget = { identity ->
            openTargets.find(identity.sourceId.value, identity.sourceAssetKey.value) != null
        },
        vaultedGrantPresent = { tokenVault.readSession() != null },
        persistUnreachable = { identity ->
            recordOpenSourceAvailability.unreachable(
                identity.sourceId.value,
                identity.sourceAssetKey.value,
            )
        },
    )

    suspend fun enrich(
        identities: Collection<AssetIdentity>,
        stored: Map<AssetIdentity, SourceAvailabilityStatus>,
    ): Map<AssetIdentity, SourceAvailabilityStatus> {
        if (identities.isEmpty()) return stored
        val grant = vaultedGrantPresent()
        val next = stored.toMutableMap()
        for (identity in identities) {
            val current = next[identity] ?: SourceAvailabilityStatus.UNKNOWN
            if (current != SourceAvailabilityStatus.UNKNOWN) continue
            val asset = findAsset(identity)
            if (!isNoteIdentity(identity, asset)) continue
            val learned = NoteOpenClassPolicy.learn(
                assetPresent = asset != null,
                openTargetPresent = hasOpenTarget(identity),
                vaultedGrantPresent = grant,
            ) ?: continue
            next[identity] = learned.status
            if (learned.persistUnreachable) {
                persistUnreachable(identity)
            }
        }
        return next
    }

    private fun isNoteIdentity(identity: AssetIdentity, asset: Asset?): Boolean =
        asset?.type == AssetType.NOTE ||
            identity.sourceId.value == NotesProviderSession.PROVIDER_MICROSOFT_ONENOTE
}
