package com.memora.app.domain.memory

import com.memora.app.domain.asset.AssetFingerprint
import com.memora.app.domain.asset.AssetIdentity

/**
 * Revision-safe persistence for Asset Memories.
 *
 * Implementations insert immutable revisions keyed by Asset identity, fingerprint,
 * and assembly schema. Existing history is never updated in place.
 */
interface MemoryRepository {
    suspend fun find(
        assetIdentity: AssetIdentity,
        assetFingerprint: AssetFingerprint,
        assemblySchemaVersion: MemoryAssemblySchemaVersion,
    ): Memory?

    suspend fun insert(memory: Memory): MemoryInsertResult

    /** Number of READY revisions matching their Asset's current fingerprint. */
    suspend fun countCurrentReady(): Int
}

sealed interface MemoryInsertResult {
    data object Inserted : MemoryInsertResult

    /** The exact immutable revision was already present; the write is idempotent. */
    data object AlreadyExists : MemoryInsertResult

    /** The key existed with different content, so history was left untouched. */
    data object RevisionConflict : MemoryInsertResult

    data object FailedSafely : MemoryInsertResult
}
