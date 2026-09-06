package com.memora.app.application.memory

import com.memora.app.domain.asset.Asset
import com.memora.app.domain.asset.AssetFingerprint
import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.asset.AssetIndexRecord
import com.memora.app.domain.asset.AssetLocation
import com.memora.app.domain.asset.AssetRepository
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.IndexingState
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.intelligence.DeterministicMemoryBuilder
import com.memora.app.domain.memory.AssetMemoryAssemblyOutcomeStore
import com.memora.app.domain.memory.AssetMemoryFact
import com.memora.app.domain.memory.AssetMemoryFactSource
import com.memora.app.domain.memory.Memory
import com.memora.app.domain.memory.MemoryAssemblySchemaVersion
import com.memora.app.domain.memory.MemoryAssemblySkipReason
import com.memora.app.domain.memory.MemoryEmbeddingSummary
import com.memora.app.domain.memory.MemoryEvidenceId
import com.memora.app.domain.memory.MemoryEvidenceKind
import com.memora.app.domain.memory.MemoryEvidenceSearchRow
import com.memora.app.domain.memory.MemoryInsertResult
import com.memora.app.domain.memory.MemoryMeaningLookup
import com.memora.app.domain.memory.MemoryRepository
import com.memora.app.domain.memory.MemoryRevisionId
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RunPendingAssetMemoryAssemblyTest {
    @Test
    fun no_usable_evidence_records_a_skip_and_assembles_the_next_asset() = runTest {
        val unusable = asset("unusable")
        val usable = asset("usable")
        val skips = FakeOutcomeStore()
        val memories = FakeDrainMemoryRepository()
        val facts = FakeQueuedFactSource(
            pending = listOf(unusable, usable),
            factsByKey = mapOf(
                unusable.identity to emptyList(),
                usable.identity to listOf(usableFact()),
            ),
            skips = skips,
            memories = memories,
        )
        val drain = drain(
            assets = listOf(unusable, usable),
            facts = facts,
            skips = skips,
            memories = memories,
        )

        val result = drain(limit = 25)

        assertTrue(result is AssetMemoryDrainResult.Completed)
        val completed = result as AssetMemoryDrainResult.Completed
        assertEquals(1, completed.assembledCount)
        assertEquals(1, completed.skippedCount)
        assertFalse(completed.hasMore)
        assertEquals(1, skips.recorded.size)
        assertEquals(unusable.identity, skips.recorded.single().identity)
        assertEquals(MemoryAssemblySkipReason.NO_USABLE_EVIDENCE, skips.recorded.single().reason)
    }

    @Test
    fun a_second_drain_does_not_see_the_skipped_asset_again() = runTest {
        val unusable = asset("unusable")
        val skips = FakeOutcomeStore()
        val memories = FakeDrainMemoryRepository()
        val facts = FakeQueuedFactSource(
            pending = listOf(unusable),
            factsByKey = mapOf(unusable.identity to emptyList()),
            skips = skips,
            memories = memories,
        )
        val drain = drain(
            assets = listOf(unusable),
            facts = facts,
            skips = skips,
            memories = memories,
        )

        val first = drain(limit = 25) as AssetMemoryDrainResult.Completed
        assertEquals(1, first.skippedCount)
        val second = drain(limit = 25) as AssetMemoryDrainResult.Completed
        assertEquals(0, second.skippedCount)
        assertEquals(0, second.assembledCount)
        assertFalse(second.hasMore)
        assertEquals(1, skips.recorded.size)
    }

    @Test
    fun infrastructure_failure_aborts_without_skipping_the_failing_asset() = runTest {
        val good = asset("good")
        val broken = asset("broken")
        val skips = FakeOutcomeStore()
        val memories = FakeDrainMemoryRepository(failInsertFor = broken.fingerprint)
        val facts = FakeQueuedFactSource(
            pending = listOf(good, broken),
            factsByKey = mapOf(
                good.identity to listOf(usableFact()),
                broken.identity to listOf(usableFact()),
            ),
            skips = skips,
            memories = memories,
        )
        val drain = drain(
            assets = listOf(good, broken),
            facts = facts,
            skips = skips,
            memories = memories,
        )

        val result = drain(limit = 25)

        assertTrue(result is AssetMemoryDrainResult.FailedSafely)
        val failed = result as AssetMemoryDrainResult.FailedSafely
        assertEquals(1, failed.assembledCount)
        assertEquals(0, failed.skippedCount)
        assertTrue(skips.recorded.isEmpty())
    }

    @Test
    fun asset_missing_is_skipped_so_the_drain_can_continue() = runTest {
        val missing = asset("missing")
        val usable = asset("usable")
        val skips = FakeOutcomeStore()
        val memories = FakeDrainMemoryRepository()
        val facts = FakeQueuedFactSource(
            pending = listOf(missing, usable),
            factsByKey = mapOf(
                missing.identity to listOf(usableFact()),
                usable.identity to listOf(usableFact()),
            ),
            skips = skips,
            memories = memories,
        )
        val drain = drain(
            assets = listOf(usable),
            facts = facts,
            skips = skips,
            memories = memories,
        )

        val result = drain(limit = 25) as AssetMemoryDrainResult.Completed
        assertEquals(1, result.assembledCount)
        assertEquals(1, result.skippedCount)
        assertEquals(MemoryAssemblySkipReason.ASSET_MISSING, skips.recorded.single().reason)
    }

    private fun drain(
        assets: List<Asset>,
        facts: FakeQueuedFactSource,
        skips: FakeOutcomeStore,
        memories: FakeDrainMemoryRepository = FakeDrainMemoryRepository(),
    ) = RunPendingAssetMemoryAssembly(
        factSource = facts,
        assembler = AssembleAssetMemoryFromExtractionFacts(
            assetRepository = FakeMultiAssetRepository(assets),
            factSource = facts,
            memoryRepository = memories,
            memoryBuilder = DeterministicMemoryBuilder(),
        ),
        memoryRepository = memories,
        outcomeStore = skips,
    )

    private fun asset(key: String) = Asset(
        identity = AssetIdentity(SourceId("source"), SourceAssetKey(key)),
        type = AssetType.PDF,
        location = AssetLocation("opaque"),
        fingerprint = AssetFingerprint("fp-$key"),
        discoveredAt = Instant.parse("2026-07-31T09:00:00Z"),
        displayName = null,
    )

    private fun usableFact() = AssetMemoryFact(
        MemoryEvidenceKind.DOCUMENT_TEXT,
        "pdf:page:1",
        "Grade 2 swimming timetable",
        "pdf-extraction-v1",
    )
}

private data class RecordedSkip(
    val identity: AssetIdentity,
    val fingerprint: AssetFingerprint,
    val reason: MemoryAssemblySkipReason,
    val factsDigest: String,
)

private class FakeOutcomeStore : AssetMemoryAssemblyOutcomeStore {
    val recorded = mutableListOf<RecordedSkip>()

    override suspend fun recordTerminal(
        identity: AssetIdentity,
        fingerprint: AssetFingerprint,
        assemblySchemaVersion: MemoryAssemblySchemaVersion,
        reason: MemoryAssemblySkipReason,
        factsDigest: String,
    ) {
        recorded += RecordedSkip(identity, fingerprint, reason, factsDigest)
    }

    override suspend fun clearForAsset(
        identity: AssetIdentity,
        fingerprint: AssetFingerprint,
    ) {
        recorded.removeAll { it.identity == identity && it.fingerprint == fingerprint }
    }

    fun isSkipped(identity: AssetIdentity, fingerprint: AssetFingerprint): Boolean =
        recorded.any { it.identity == identity && it.fingerprint == fingerprint }
}

private class FakeQueuedFactSource(
    private val pending: List<Asset>,
    private val factsByKey: Map<AssetIdentity, List<AssetMemoryFact>>,
    private val skips: FakeOutcomeStore,
    private val memories: FakeDrainMemoryRepository,
) : AssetMemoryFactSource {
    override suspend fun loadCurrentFacts(asset: Asset): List<AssetMemoryFact> =
        factsByKey[asset.identity].orEmpty()

    override suspend fun findNextPendingAsset(
        assemblySchemaVersion: MemoryAssemblySchemaVersion,
    ): Asset? = pending.firstOrNull { asset ->
        !skips.isSkipped(asset.identity, asset.fingerprint) &&
            memories.find(asset.identity, asset.fingerprint, assemblySchemaVersion) == null
    }

    override suspend fun countPendingAssembly(
        assemblySchemaVersion: MemoryAssemblySchemaVersion,
    ): Int = pending.count { asset ->
        !skips.isSkipped(asset.identity, asset.fingerprint) &&
            memories.find(asset.identity, asset.fingerprint, assemblySchemaVersion) == null
    }
}

private class FakeMultiAssetRepository(
    assets: List<Asset>,
) : AssetRepository {
    private val records = assets.associate {
        it.identity to AssetIndexRecord(it, IndexingState.discovered)
    }.toMutableMap()

    override suspend fun save(record: AssetIndexRecord) {
        records[record.asset.identity] = record
    }

    override suspend fun find(identity: AssetIdentity): AssetIndexRecord? = records[identity]

    override suspend fun findFirstBySourceAndType(sourceId: SourceId, type: AssetType): Asset? = null
    override suspend fun countBySourceAndType(sourceId: SourceId, type: AssetType): Int = 0
    override suspend fun findNextPdfPendingLocalReading(
        sourceId: SourceId,
        schemaVersion: String,
        afterSourceAssetKey: String?,
    ): Asset? = null
    override suspend fun countPdfPendingLocalReading(sourceId: SourceId, schemaVersion: String): Int = 0
    override suspend fun findNextImagePendingExifExtract(
        sourceId: SourceId,
        schemaVersion: String,
        afterSourceAssetKey: String?,
    ): Asset? = null
    override suspend fun findNextScreenshotPendingOcrExtract(
        sourceId: SourceId,
        schemaVersion: String,
        afterSourceAssetKey: String?,
    ): Asset? = null
    override suspend fun findNextPhotoPendingOcrExtract(
        sourceId: SourceId,
        schemaVersion: String,
        afterSourceAssetKey: String?,
    ): Asset? = null
    override suspend fun findNextNotePendingPageExtract(
        sourceId: SourceId,
        schemaVersion: String,
        afterSourceAssetKey: String?,
    ): Asset? = null
}

private class FakeDrainMemoryRepository(
    private val failInsertFor: AssetFingerprint? = null,
) : MemoryRepository {
    private val history = mutableListOf<Memory>()

    override suspend fun find(
        assetIdentity: AssetIdentity,
        assetFingerprint: AssetFingerprint,
        assemblySchemaVersion: MemoryAssemblySchemaVersion,
    ): Memory? = history.firstOrNull {
        it.assetIdentity == assetIdentity &&
            it.assetFingerprint == assetFingerprint &&
            it.assemblySchemaVersion == assemblySchemaVersion
    }

    override suspend fun insert(memory: Memory): MemoryInsertResult {
        if (failInsertFor != null && memory.assetFingerprint == failInsertFor) {
            return MemoryInsertResult.FailedSafely
        }
        val existing = find(
            memory.assetIdentity,
            memory.assetFingerprint,
            memory.assemblySchemaVersion,
        )
        if (existing != null) {
            return if (existing == memory) MemoryInsertResult.AlreadyExists
            else MemoryInsertResult.RevisionConflict
        }
        history += memory
        return MemoryInsertResult.Inserted
    }

    override suspend fun countCurrentReady(): Int = history.size
    override suspend fun countMeaningIndexCandidates(): Int = history.size
    override suspend fun listCurrentReadySummaries(limit: Int) = emptyList<MemoryEmbeddingSummary>()
    override suspend fun countMeaningIndexPending(
        model: com.memora.app.domain.intelligence.ModelVersionIdentity,
    ): Int = 0
    override suspend fun listMeaningIndexSummaries(
        model: com.memora.app.domain.intelligence.ModelVersionIdentity,
        limit: Int,
    ) = emptyList<MemoryEmbeddingSummary>()
    override suspend fun listCurrentReadyRevisionIds() =
        history.mapTo(linkedSetOf()) { it.revisionId }
    override suspend fun listCurrentStaleReindexRevisionIds() = emptySet<MemoryRevisionId>()
    override suspend fun markIntegrityState(
        revisionIds: Collection<MemoryRevisionId>,
        from: com.memora.app.domain.memory.MemoryIntegrityState,
        to: com.memora.app.domain.memory.MemoryIntegrityState,
        nowEpochMs: Long,
    ): Int = 0
    override suspend fun findCurrentReadyMeaningLookups(
        revisionIds: Collection<MemoryRevisionId>,
    ) = emptyMap<MemoryRevisionId, MemoryMeaningLookup>()
    override suspend fun findMeaningIndexLookups(
        revisionIds: Collection<MemoryRevisionId>,
    ) = emptyMap<MemoryRevisionId, MemoryMeaningLookup>()
    override suspend fun findPdfPageEvidenceIds(
        revisionIds: Collection<MemoryRevisionId>,
    ) = emptyMap<MemoryRevisionId, Map<Int, MemoryEvidenceId>>()
    override suspend fun findEvidenceSearchRows(
        revisionIds: Collection<MemoryRevisionId>,
    ) = emptyMap<MemoryRevisionId, Map<MemoryEvidenceId, MemoryEvidenceSearchRow>>()
    override suspend fun findOcrTextEvidenceForEmbedding(
        revisionIds: Collection<MemoryRevisionId>,
    ) = emptyMap<MemoryRevisionId, List<MemoryEvidenceSearchRow>>()
    override suspend fun findNoteTextEvidenceForEmbedding(
        revisionIds: Collection<MemoryRevisionId>,
    ) = emptyMap<MemoryRevisionId, List<MemoryEvidenceSearchRow>>()
    override suspend fun findSignatureAnchors(
        revisionIds: Collection<MemoryRevisionId>,
    ) = emptyMap<MemoryRevisionId, List<com.memora.app.domain.memory.MemoryAnchor>>()
}
