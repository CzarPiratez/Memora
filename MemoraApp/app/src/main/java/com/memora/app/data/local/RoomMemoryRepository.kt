package com.memora.app.data.local

import com.memora.app.domain.asset.AssetFingerprint
import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.memory.Memory
import com.memora.app.domain.memory.MemoryAssemblySchemaVersion
import com.memora.app.domain.memory.MemoryInsertResult
import com.memora.app.domain.memory.MemoryRepository

class RoomMemoryRepository(
    private val database: () -> MemoraDatabase,
) : MemoryRepository {
    override suspend fun find(
        assetIdentity: AssetIdentity,
        assetFingerprint: AssetFingerprint,
        assemblySchemaVersion: MemoryAssemblySchemaVersion,
    ): Memory? = database().memoryDao().findRows(
        sourceId = assetIdentity.sourceId.value,
        sourceAssetKey = assetIdentity.sourceAssetKey.value,
        fingerprint = assetFingerprint.value,
        assemblySchemaVersion = assemblySchemaVersion.value,
    )?.let(MemoryRoomMapper::toDomain)

    override suspend fun insert(memory: Memory): MemoryInsertResult {
        val dao = database().memoryDao()
        return try {
            val existing = find(
                memory.assetIdentity,
                memory.assetFingerprint,
                memory.assemblySchemaVersion,
            )
            if (existing != null) {
                return if (existing == memory) {
                    MemoryInsertResult.AlreadyExists
                } else {
                    MemoryInsertResult.RevisionConflict
                }
            }

            dao.insertAtomic(MemoryRoomMapper.toRows(memory))
            MemoryInsertResult.Inserted
        } catch (_: Exception) {
            val raced = runCatching {
                find(memory.assetIdentity, memory.assetFingerprint, memory.assemblySchemaVersion)
            }.getOrNull()
            if (raced == memory) MemoryInsertResult.AlreadyExists else MemoryInsertResult.FailedSafely
        }
    }

    override suspend fun countCurrentReady(): Int = database().memoryDao().countCurrentReady()
}
