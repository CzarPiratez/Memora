package com.memora.app.data.local

import com.memora.app.domain.asset.AssetFingerprint
import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.memory.AssetMemoryAssemblyOutcomeStore
import com.memora.app.domain.memory.MemoryAssemblySchemaVersion
import com.memora.app.domain.memory.MemoryAssemblySkipReason

class RoomAssetMemoryAssemblyOutcomeStore(
    private val database: () -> MemoraDatabase,
) : AssetMemoryAssemblyOutcomeStore {
    override suspend fun recordTerminal(
        identity: AssetIdentity,
        fingerprint: AssetFingerprint,
        assemblySchemaVersion: MemoryAssemblySchemaVersion,
        reason: MemoryAssemblySkipReason,
        factsDigest: String,
    ) {
        require(factsDigest.isNotBlank())
        database().memoryAssemblySkipDao().upsert(
            MemoryAssemblySkipEntity(
                sourceId = identity.sourceId.value,
                sourceAssetKey = identity.sourceAssetKey.value,
                fingerprint = fingerprint.value,
                assemblySchemaVersion = assemblySchemaVersion.value,
                reason = reason.name,
                factsDigest = factsDigest,
                recordedAtEpochMillis = System.currentTimeMillis(),
            ),
        )
    }

    override suspend fun clearForAsset(
        identity: AssetIdentity,
        fingerprint: AssetFingerprint,
    ) {
        database().memoryAssemblySkipDao().deleteForAsset(
            sourceId = identity.sourceId.value,
            sourceAssetKey = identity.sourceAssetKey.value,
            fingerprint = fingerprint.value,
        )
    }
}

internal suspend fun MemoraDatabase.clearMemoryAssemblySkips(
    sourceId: String,
    sourceAssetKey: String,
    fingerprint: String,
) {
    memoryAssemblySkipDao().deleteForAsset(sourceId, sourceAssetKey, fingerprint)
}
