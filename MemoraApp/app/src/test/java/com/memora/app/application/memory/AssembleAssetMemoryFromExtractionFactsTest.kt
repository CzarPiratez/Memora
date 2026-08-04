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
import com.memora.app.domain.memory.AssetMemoryFact
import com.memora.app.domain.memory.AssetMemoryFactSource
import com.memora.app.domain.memory.Memory
import com.memora.app.domain.memory.MemoryAssemblySchemaVersion
import com.memora.app.domain.memory.MemoryEmbeddingSummary
import com.memora.app.domain.memory.MemoryEvidenceKind
import com.memora.app.domain.memory.MemoryInsertResult
import com.memora.app.domain.memory.MemoryMeaningLookup
import com.memora.app.domain.memory.MemoryRepository
import com.memora.app.domain.memory.MemoryRevisionId
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AssembleAssetMemoryFromExtractionFactsTest {
    @Test
    fun `assembles PDF OCR and EXIF facts as cited deterministic evidence`() = runTest {
        val asset = asset()
        val facts = listOf(
            AssetMemoryFact(
                MemoryEvidenceKind.DOCUMENT_TEXT,
                "pdf:page:1",
                "  Project   Atlas launch notes  ",
                "pdf-extraction-v1",
            ),
            AssetMemoryFact(
                MemoryEvidenceKind.OCR_TEXT,
                "image:whole",
                "Gate B7",
                "screenshot-ocr-v1",
            ),
            AssetMemoryFact(
                MemoryEvidenceKind.SOURCE_METADATA,
                "exif:fields",
                "Date taken: 2026:07:31 10:30:00",
                "image-exif-v1",
            ),
        )
        val repository = FakeMemoryRepository()
        val assembler = AssembleAssetMemoryFromExtractionFacts(
            assetRepository = FakeAssetRepository(asset),
            factSource = FakeFactSource(facts),
            memoryRepository = repository,
            clock = Clock.fixed(Instant.parse("2026-07-31T10:00:00Z"), ZoneOffset.UTC),
        )

        val result = assembler(asset.identity)

        assertTrue(result is AssetMemoryAssemblyResult.Persisted)
        val memory = (result as AssetMemoryAssemblyResult.Persisted).memory
        assertEquals(
            listOf(
                MemoryEvidenceKind.DOCUMENT_TEXT,
                MemoryEvidenceKind.OCR_TEXT,
                MemoryEvidenceKind.SOURCE_METADATA,
            ),
            memory.evidence.map { it.kind },
        )
        assertEquals("Project Atlas launch notes", memory.signature.summary.text.value)
        assertEquals(
            memory.evidence.first { it.kind == MemoryEvidenceKind.DOCUMENT_TEXT }.id,
            memory.signature.summary.evidenceIds.single(),
        )
        assertEquals(
            memory.evidence.first { it.kind == MemoryEvidenceKind.DOCUMENT_TEXT }.id,
            memory.signature.anchors.single().evidenceIds.single(),
        )
        assertEquals(
            setOf("pdf-extraction-v1", "screenshot-ocr-v1", "image-exif-v1"),
            memory.extractionSchemaVersions,
        )
    }

    @Test
    fun `assembles NOTE_TEXT and note title as cited deterministic evidence`() = runTest {
        val noteAsset = asset(type = AssetType.NOTE)
        val facts = listOf(
            AssetMemoryFact(
                MemoryEvidenceKind.NOTE_TEXT,
                "note:page",
                "  Meeting notes for Project Atlas  ",
                "onenote-page-text-v1",
            ),
            AssetMemoryFact(
                MemoryEvidenceKind.SOURCE_METADATA,
                "note:title",
                "Title: Project Atlas",
                "onenote-page-text-v1",
            ),
        )
        val repository = FakeMemoryRepository()
        val result = AssembleAssetMemoryFromExtractionFacts(
            FakeAssetRepository(noteAsset),
            FakeFactSource(facts),
            repository,
            Clock.fixed(Instant.parse("2026-08-02T10:00:00Z"), ZoneOffset.UTC),
        )(noteAsset.identity)

        assertTrue(result is AssetMemoryAssemblyResult.Persisted)
        val memory = (result as AssetMemoryAssemblyResult.Persisted).memory
        assertEquals(
            listOf(MemoryEvidenceKind.NOTE_TEXT, MemoryEvidenceKind.SOURCE_METADATA),
            memory.evidence.map { it.kind },
        )
        assertEquals("Meeting notes for Project Atlas", memory.signature.summary.text.value)
        assertEquals(setOf("onenote-page-text-v1"), memory.extractionSchemaVersions)
    }

    @Test
    fun `skips persistence when extracts contain no usable evidence`() = runTest {
        val asset = asset()
        val repository = FakeMemoryRepository()
        val result = AssembleAssetMemoryFromExtractionFacts(
            FakeAssetRepository(asset),
            FakeFactSource(emptyList()),
            repository,
            Clock.systemUTC(),
        )(asset.identity)

        assertEquals(AssetMemoryAssemblyResult.NoUsableEvidence, result)
        assertEquals(null, repository.stored)
    }

    @Test
    fun `prefers OCR summary even when EXIF metadata is listed first`() = runTest {
        val asset = asset(type = AssetType.PHOTO)
        val facts = listOf(
            AssetMemoryFact(
                MemoryEvidenceKind.SOURCE_METADATA,
                "exif:fields",
                "Date taken: 2026:07:31 10:30:00; Dimensions: 640 × 480",
                "image-exif-v1",
            ),
            AssetMemoryFact(
                MemoryEvidenceKind.OCR_TEXT,
                "image:whole",
                "Cafe receipt total 12.50",
                "photo-ocr-v1",
            ),
        )
        val result = AssembleAssetMemoryFromExtractionFacts(
            FakeAssetRepository(asset),
            FakeFactSource(facts),
            FakeMemoryRepository(),
            Clock.systemUTC(),
        )(asset.identity)

        val memory = (result as AssetMemoryAssemblyResult.Persisted).memory
        assertEquals("Cafe receipt total 12.50", memory.signature.summary.text.value)
        assertEquals(
            MemoryEvidenceKind.OCR_TEXT,
            memory.evidence.first { it.id == memory.signature.summary.evidenceIds.single() }.kind,
        )
    }

    @Test
    fun `rejects dimension-only EXIF noise as unusable evidence`() = runTest {
        val asset = asset(type = AssetType.PHOTO)
        val result = AssembleAssetMemoryFromExtractionFacts(
            FakeAssetRepository(asset),
            FakeFactSource(
                listOf(
                    AssetMemoryFact(
                        MemoryEvidenceKind.SOURCE_METADATA,
                        "exif:fields",
                        "Dimensions: 640 × 480; Orientation: 1",
                        "image-exif-v1",
                    ),
                ),
            ),
            FakeMemoryRepository(),
            Clock.systemUTC(),
        )(asset.identity)

        assertEquals(AssetMemoryAssemblyResult.NoUsableEvidence, result)
    }

    @Test
    fun `changed fingerprint creates a new revision without rewriting prior history`() = runTest {
        val first = asset(fingerprint = "fp-1")
        val second = asset(fingerprint = "fp-2")
        val repository = FakeMemoryRepository()
        val factSource = FakeFactSource(
            listOf(AssetMemoryFact(MemoryEvidenceKind.OCR_TEXT, "image:whole", "Receipt 42", "ocr-v1")),
        )

        val firstMemory = (AssembleAssetMemoryFromExtractionFacts(
            FakeAssetRepository(first),
            factSource,
            repository,
            Clock.systemUTC(),
        )(first.identity) as AssetMemoryAssemblyResult.Persisted).memory
        val secondMemory = (AssembleAssetMemoryFromExtractionFacts(
            FakeAssetRepository(second),
            factSource,
            repository,
            Clock.systemUTC(),
        )(second.identity) as AssetMemoryAssemblyResult.Persisted).memory

        assertEquals(firstMemory.id, secondMemory.id)
        assertTrue(firstMemory.revisionId != secondMemory.revisionId)
        assertEquals(2, repository.history.size)
    }

    private fun asset(
        fingerprint: String = "fp-1",
        type: AssetType = AssetType.PDF,
    ) = Asset(
        identity = AssetIdentity(SourceId("source"), SourceAssetKey("asset")),
        type = type,
        location = AssetLocation("opaque"),
        fingerprint = AssetFingerprint(fingerprint),
        discoveredAt = Instant.parse("2026-07-31T09:00:00Z"),
        displayName = if (type == AssetType.NOTE) "Project Atlas" else null,
    )
}

private class FakeFactSource(
    private val facts: List<AssetMemoryFact>,
) : AssetMemoryFactSource {
    override suspend fun loadCurrentFacts(asset: Asset): List<AssetMemoryFact> = facts
    override suspend fun findNextPendingAsset(
        assemblySchemaVersion: MemoryAssemblySchemaVersion,
    ): Asset? = null
}

private class FakeMemoryRepository : MemoryRepository {
    val history = mutableListOf<Memory>()
    var stored: Memory? = null

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
        val existing = find(memory.assetIdentity, memory.assetFingerprint, memory.assemblySchemaVersion)
        if (existing != null) {
            return if (existing == memory) MemoryInsertResult.AlreadyExists
            else MemoryInsertResult.RevisionConflict
        }
        history += memory
        stored = memory
        return MemoryInsertResult.Inserted
    }

    override suspend fun countCurrentReady(): Int = history.size

    override suspend fun listCurrentReadySummaries(limit: Int) = emptyList<MemoryEmbeddingSummary>()

    override suspend fun findCurrentReadyMeaningLookups(
        revisionIds: Collection<MemoryRevisionId>,
    ) = emptyMap<MemoryRevisionId, MemoryMeaningLookup>()
}

private class FakeAssetRepository(asset: Asset) : AssetRepository {
    private val records = mutableMapOf(asset.identity to AssetIndexRecord(asset, IndexingState.discovered))
    override suspend fun save(record: AssetIndexRecord) { records[record.asset.identity] = record }
    override suspend fun find(identity: AssetIdentity): AssetIndexRecord? = records[identity]
    override suspend fun findFirstBySourceAndType(sourceId: SourceId, type: AssetType): Asset? = null
    override suspend fun countBySourceAndType(sourceId: SourceId, type: AssetType): Int = 0
    override suspend fun findNextPdfPendingLocalReading(
        sourceId: SourceId,
        schemaVersion: String,
        afterSourceAssetKey: String?,
    ): Asset? = null
    override suspend fun countPdfPendingLocalReading(
        sourceId: SourceId,
        schemaVersion: String,
    ): Int = 0
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
