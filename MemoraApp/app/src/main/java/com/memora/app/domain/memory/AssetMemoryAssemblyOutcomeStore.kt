package com.memora.app.domain.memory

import com.memora.app.domain.asset.AssetFingerprint
import com.memora.app.domain.asset.AssetIdentity

/** Why one Asset could not become a Memory for the current assembly schema. */
enum class MemoryAssemblySkipReason {
    NO_USABLE_EVIDENCE,
    ASSET_MISSING,
    REVISION_CONFLICT,
}

/**
 * Durable per-Asset assembly outcomes so a drain cursor can advance.
 *
 * Not a Memory. Find must never read this table. A skip is keyed by Asset
 * identity + fingerprint + assembly schema and is cleared when new facts land.
 */
interface AssetMemoryAssemblyOutcomeStore {
    suspend fun recordTerminal(
        identity: AssetIdentity,
        fingerprint: AssetFingerprint,
        assemblySchemaVersion: MemoryAssemblySchemaVersion,
        reason: MemoryAssemblySkipReason,
        factsDigest: String,
    )

    suspend fun clearForAsset(
        identity: AssetIdentity,
        fingerprint: AssetFingerprint,
    )
}
