package com.memora.app.application.memory

import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.asset.AssetRepository
import com.memora.app.domain.intelligence.DeterministicMemoryBuilder
import com.memora.app.domain.intelligence.MemoryBuildResult
import com.memora.app.domain.intelligence.MemoryBuilder
import com.memora.app.domain.memory.AssetMemoryFactSource
import com.memora.app.domain.memory.Memory
import com.memora.app.domain.memory.MemoryInsertResult
import com.memora.app.domain.memory.MemoryRepository
import java.time.Clock
import javax.inject.Inject

/**
 * Orchestrates Asset Memory assembly: asset lookup, fact load, already-present
 * check, and persistence. Pure construction is [MemoryBuilder] (MIG-04).
 *
 * Production path: drain → this use case → [DeterministicMemoryBuilder].
 */
class AssembleAssetMemoryFromExtractionFacts internal constructor(
    private val assetRepository: AssetRepository,
    private val factSource: AssetMemoryFactSource,
    private val memoryRepository: MemoryRepository,
    private val memoryBuilder: MemoryBuilder,
    private val clock: Clock,
) {
    @Inject
    constructor(
        assetRepository: AssetRepository,
        factSource: AssetMemoryFactSource,
        memoryRepository: MemoryRepository,
        memoryBuilder: MemoryBuilder,
    ) : this(
        assetRepository,
        factSource,
        memoryRepository,
        memoryBuilder,
        Clock.systemUTC(),
    )

    suspend operator fun invoke(identity: AssetIdentity): AssetMemoryAssemblyResult {
        val record = assetRepository.find(identity) ?: return AssetMemoryAssemblyResult.AssetMissing
        val asset = record.asset
        val existing = memoryRepository.find(asset.identity, asset.fingerprint, ASSEMBLY_SCHEMA)
        if (existing != null) return AssetMemoryAssemblyResult.AlreadyPresent(existing)

        val facts = factSource.loadCurrentFacts(asset)
        return when (
            val built = memoryBuilder.assemble(
                assetIdentity = asset.identity,
                assetFingerprint = asset.fingerprint,
                facts = facts,
                localObservations = emptyList(),
                createdAt = clock.instant(),
            )
        ) {
            is MemoryBuildResult.Success -> persist(built.memory)
            MemoryBuildResult.NoUsableEvidence -> AssetMemoryAssemblyResult.NoUsableEvidence
            is MemoryBuildResult.ObservationsUnsupported,
            is MemoryBuildResult.Unavailable,
            -> AssetMemoryAssemblyResult.FailedSafely
        }
    }

    private suspend fun persist(memory: Memory): AssetMemoryAssemblyResult =
        when (memoryRepository.insert(memory)) {
            MemoryInsertResult.Inserted -> AssetMemoryAssemblyResult.Persisted(memory)
            MemoryInsertResult.AlreadyExists -> AssetMemoryAssemblyResult.AlreadyPresent(
                memoryRepository.find(memory.assetIdentity, memory.assetFingerprint, ASSEMBLY_SCHEMA)
                    ?: memory,
            )
            MemoryInsertResult.RevisionConflict -> AssetMemoryAssemblyResult.RevisionConflict
            MemoryInsertResult.FailedSafely -> AssetMemoryAssemblyResult.FailedSafely
        }

    companion object {
        /** Re-export of the deterministic builder schema for drain / repository callers. */
        val ASSEMBLY_SCHEMA = DeterministicMemoryBuilder.ASSEMBLY_SCHEMA

        const val MAX_EVIDENCE_CHARS_PER_ITEM =
            DeterministicMemoryBuilder.MAX_EVIDENCE_CHARS_PER_ITEM
        const val MAX_SUMMARY_CHARS = DeterministicMemoryBuilder.MAX_SUMMARY_CHARS
        const val MAX_ANCHOR_CHARS = DeterministicMemoryBuilder.MAX_ANCHOR_CHARS
        const val MAX_LOCATOR_CHARS = DeterministicMemoryBuilder.MAX_LOCATOR_CHARS
    }
}

sealed interface AssetMemoryAssemblyResult {
    data class Persisted(val memory: Memory) : AssetMemoryAssemblyResult
    data class AlreadyPresent(val memory: Memory) : AssetMemoryAssemblyResult
    data object AssetMissing : AssetMemoryAssemblyResult
    data object NoUsableEvidence : AssetMemoryAssemblyResult
    data object RevisionConflict : AssetMemoryAssemblyResult
    data object FailedSafely : AssetMemoryAssemblyResult
}
