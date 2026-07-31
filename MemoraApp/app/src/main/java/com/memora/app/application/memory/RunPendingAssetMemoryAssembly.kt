package com.memora.app.application.memory

import com.memora.app.domain.memory.AssetMemoryFactSource
import com.memora.app.domain.memory.MemoryRepository
import javax.inject.Inject

/** Explicit, bounded foreground drain over already-saved deterministic facts. */
class RunPendingAssetMemoryAssembly @Inject constructor(
    private val factSource: AssetMemoryFactSource,
    private val assembler: AssembleAssetMemoryFromExtractionFacts,
    private val memoryRepository: MemoryRepository,
) {
    suspend operator fun invoke(limit: Int = DEFAULT_BATCH_LIMIT): AssetMemoryDrainResult {
        require(limit in 1..MAX_BATCH_LIMIT)
        var inserted = 0
        var inspected = 0

        while (inspected < limit) {
            val asset = factSource.findNextPendingAsset(
                AssembleAssetMemoryFromExtractionFacts.ASSEMBLY_SCHEMA,
            ) ?: break
            inspected += 1
            when (assembler(asset.identity)) {
                is AssetMemoryAssemblyResult.Persisted -> inserted += 1
                is AssetMemoryAssemblyResult.AlreadyPresent -> Unit
                AssetMemoryAssemblyResult.NoUsableEvidence,
                AssetMemoryAssemblyResult.AssetMissing,
                AssetMemoryAssemblyResult.RevisionConflict,
                AssetMemoryAssemblyResult.FailedSafely,
                -> return AssetMemoryDrainResult.FailedSafely(
                    assembledCount = inserted,
                    currentReadyCount = memoryRepository.countCurrentReady(),
                )
            }
        }

        val hasMore = factSource.findNextPendingAsset(
            AssembleAssetMemoryFromExtractionFacts.ASSEMBLY_SCHEMA,
        ) != null
        return AssetMemoryDrainResult.Completed(
            assembledCount = inserted,
            currentReadyCount = memoryRepository.countCurrentReady(),
            hasMore = hasMore,
        )
    }

    companion object {
        const val DEFAULT_BATCH_LIMIT = 25
        const val MAX_BATCH_LIMIT = 100
    }
}

sealed interface AssetMemoryDrainResult {
    data class Completed(
        val assembledCount: Int,
        val currentReadyCount: Int,
        val hasMore: Boolean,
    ) : AssetMemoryDrainResult

    data class FailedSafely(
        val assembledCount: Int,
        val currentReadyCount: Int,
    ) : AssetMemoryDrainResult
}
