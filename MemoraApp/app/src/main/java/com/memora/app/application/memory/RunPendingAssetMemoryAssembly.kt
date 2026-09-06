package com.memora.app.application.memory

import com.memora.app.domain.asset.Asset
import com.memora.app.domain.memory.AssemblyFactsDigest
import com.memora.app.domain.memory.AssetMemoryAssemblyOutcomeStore
import com.memora.app.domain.memory.AssetMemoryFactSource
import com.memora.app.domain.memory.MemoryAssemblySkipReason
import com.memora.app.domain.memory.MemoryRepository
import javax.inject.Inject

/** Explicit, bounded foreground drain over already-saved deterministic facts. */
class RunPendingAssetMemoryAssembly @Inject constructor(
    private val factSource: AssetMemoryFactSource,
    private val assembler: AssembleAssetMemoryFromExtractionFacts,
    private val memoryRepository: MemoryRepository,
    private val outcomeStore: AssetMemoryAssemblyOutcomeStore,
) {
    suspend operator fun invoke(limit: Int = DEFAULT_BATCH_LIMIT): AssetMemoryDrainResult {
        require(limit in 1..MAX_BATCH_LIMIT)
        var inserted = 0
        var skipped = 0
        var inspected = 0

        while (inspected < limit) {
            val asset = factSource.findNextPendingAsset(
                AssembleAssetMemoryFromExtractionFacts.ASSEMBLY_SCHEMA,
            ) ?: break
            inspected += 1
            when (val result = assembler(asset.identity)) {
                is AssetMemoryAssemblyResult.Persisted -> inserted += 1
                is AssetMemoryAssemblyResult.AlreadyPresent -> Unit
                AssetMemoryAssemblyResult.NoUsableEvidence -> {
                    recordSkip(asset, MemoryAssemblySkipReason.NO_USABLE_EVIDENCE)
                    skipped += 1
                }
                AssetMemoryAssemblyResult.AssetMissing -> {
                    recordSkip(asset, MemoryAssemblySkipReason.ASSET_MISSING)
                    skipped += 1
                }
                AssetMemoryAssemblyResult.RevisionConflict -> {
                    recordSkip(asset, MemoryAssemblySkipReason.REVISION_CONFLICT)
                    skipped += 1
                }
                AssetMemoryAssemblyResult.FailedSafely ->
                    return AssetMemoryDrainResult.FailedSafely(
                        assembledCount = inserted,
                        skippedCount = skipped,
                        currentReadyCount = memoryRepository.countCurrentReady(),
                    )
            }
        }

        val hasMore = factSource.findNextPendingAsset(
            AssembleAssetMemoryFromExtractionFacts.ASSEMBLY_SCHEMA,
        ) != null
        return AssetMemoryDrainResult.Completed(
            assembledCount = inserted,
            skippedCount = skipped,
            currentReadyCount = memoryRepository.countCurrentReady(),
            hasMore = hasMore,
        )
    }

    private suspend fun recordSkip(asset: Asset, reason: MemoryAssemblySkipReason) {
        val digest = when (reason) {
            MemoryAssemblySkipReason.ASSET_MISSING -> AssemblyFactsDigest.NONE
            MemoryAssemblySkipReason.NO_USABLE_EVIDENCE,
            MemoryAssemblySkipReason.REVISION_CONFLICT,
            -> AssemblyFactsDigest.of(factSource.loadCurrentFacts(asset))
        }
        outcomeStore.recordTerminal(
            identity = asset.identity,
            fingerprint = asset.fingerprint,
            assemblySchemaVersion = AssembleAssetMemoryFromExtractionFacts.ASSEMBLY_SCHEMA,
            reason = reason,
            factsDigest = digest,
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
        val skippedCount: Int,
        val currentReadyCount: Int,
        val hasMore: Boolean,
    ) : AssetMemoryDrainResult

    data class FailedSafely(
        val assembledCount: Int,
        val skippedCount: Int,
        val currentReadyCount: Int,
    ) : AssetMemoryDrainResult
}
