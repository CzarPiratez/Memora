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
import com.memora.app.domain.memory.AssetMemoryFact
import com.memora.app.domain.memory.AssetMemoryFactSource
import com.memora.app.domain.memory.Memory
import com.memora.app.domain.memory.MemoryAnchorKind
import com.memora.app.domain.memory.MemoryAssemblySchemaVersion
import com.memora.app.domain.memory.MemoryEmbeddingSummary
import com.memora.app.domain.memory.MemoryEvidenceClass
import com.memora.app.domain.memory.MemoryEvidenceId
import com.memora.app.domain.memory.MemoryEvidenceKind
import com.memora.app.domain.memory.MemoryInsertResult
import com.memora.app.domain.memory.MemoryMeaningLookup
import com.memora.app.domain.memory.MemoryEvidenceSearchRow
import com.memora.app.domain.memory.MemoryRepository
import com.memora.app.domain.memory.MemoryRevisionId
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AssembleAssetMemoryFromExtractionFactsTest {
    private fun assembler(
        asset: Asset,
        facts: List<AssetMemoryFact>,
        repository: FakeMemoryRepository = FakeMemoryRepository(),
        clock: Clock = Clock.systemUTC(),
    ) = AssembleAssetMemoryFromExtractionFacts(
        assetRepository = FakeAssetRepository(asset),
        factSource = FakeFactSource(facts),
        memoryRepository = repository,
        memoryBuilder = DeterministicMemoryBuilder(),
        clock = clock,
    )

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
        val assembler = assembler(
            asset = asset,
            facts = facts,
            repository = repository,
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
        assertTrue(memory.evidence.all { it.evidenceClass == MemoryEvidenceClass.DIRECT })
        assertEquals("Project Atlas launch notes", memory.signature.summary.text.value)
        assertEquals(
            memory.evidence.first { it.kind == MemoryEvidenceKind.DOCUMENT_TEXT }.id,
            memory.signature.summary.evidenceIds.single(),
        )
        val textAnchor = memory.signature.anchors.single { it.kind == MemoryAnchorKind.TEXT }
        assertEquals(
            memory.evidence.first { it.kind == MemoryEvidenceKind.DOCUMENT_TEXT }.id,
            textAnchor.evidenceIds.single(),
        )
        val timeAnchor = memory.signature.anchors.single { it.kind == MemoryAnchorKind.TIME }
        val exifEvidence = memory.evidence.single {
            it.kind == MemoryEvidenceKind.SOURCE_METADATA && it.locator.value == "exif:fields"
        }
        assertEquals(exifEvidence.id, timeAnchor.evidenceIds.single())
        assertEquals("Date taken: 2026:07:31 10:30:00", timeAnchor.text.value)
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
        val result = assembler(
            asset = noteAsset,
            facts = facts,
            repository = repository,
            clock = Clock.fixed(Instant.parse("2026-08-02T10:00:00Z"), ZoneOffset.UTC),
        )(noteAsset.identity)

        assertTrue(result is AssetMemoryAssemblyResult.Persisted)
        val memory = (result as AssetMemoryAssemblyResult.Persisted).memory
        assertEquals(
            listOf(MemoryEvidenceKind.NOTE_TEXT, MemoryEvidenceKind.SOURCE_METADATA),
            memory.evidence.map { it.kind },
        )
        assertTrue(memory.evidence.all { it.evidenceClass == MemoryEvidenceClass.DIRECT })
        assertEquals("Meeting notes for Project Atlas", memory.signature.summary.text.value)
        val topicAnchor = memory.signature.anchors.single { it.kind == MemoryAnchorKind.TOPIC }
        val titleEvidence = memory.evidence.single {
            it.kind == MemoryEvidenceKind.SOURCE_METADATA && it.locator.value == "note:title"
        }
        assertEquals(titleEvidence.id, topicAnchor.evidenceIds.single())
        assertEquals("Title: Project Atlas", topicAnchor.text.value)
        assertTrue(memory.signature.anchors.any { it.kind == MemoryAnchorKind.TEXT })
        assertNull(memory.signature.anchors.firstOrNull { it.kind == MemoryAnchorKind.TIME })
        assertEquals(setOf("onenote-page-text-v1"), memory.extractionSchemaVersions)
    }

    @Test
    fun `emits TOPIC anchor from pdf title fact citing that evidence`() = runTest {
        val asset = asset()
        val facts = listOf(
            AssetMemoryFact(
                MemoryEvidenceKind.DOCUMENT_TEXT,
                "pdf:page:1",
                "Body of the quarterly plan",
                "pdf-extraction-v1",
            ),
            AssetMemoryFact(
                MemoryEvidenceKind.SOURCE_METADATA,
                "pdf:title",
                "Title: Quarterly Plan",
                "pdf-extraction-v1",
            ),
        )
        val memory = (
            assembler(asset, facts)(asset.identity) as AssetMemoryAssemblyResult.Persisted
            ).memory

        val topicAnchor = memory.signature.anchors.single { it.kind == MemoryAnchorKind.TOPIC }
        val titleEvidence = memory.evidence.single { it.locator.value == "pdf:title" }
        assertEquals(titleEvidence.id, topicAnchor.evidenceIds.single())
        assertEquals("Title: Quarterly Plan", topicAnchor.text.value)
        assertTrue(memory.signature.anchors.any { it.kind == MemoryAnchorKind.TEXT })
        assertNull(memory.signature.anchors.firstOrNull { it.kind == MemoryAnchorKind.TIME })
        assertTrue(memory.evidence.all { it.evidenceClass == MemoryEvidenceClass.DIRECT })
    }

    @Test
    fun `does not fabricate TIME or TOPIC when those facts are absent`() = runTest {
        val asset = asset()
        val facts = listOf(
            AssetMemoryFact(
                MemoryEvidenceKind.DOCUMENT_TEXT,
                "pdf:page:1",
                "Only page text remains",
                "pdf-extraction-v1",
            ),
        )
        val memory = (
            assembler(asset, facts)(asset.identity) as AssetMemoryAssemblyResult.Persisted
            ).memory

        assertEquals(listOf(MemoryAnchorKind.TEXT), memory.signature.anchors.map { it.kind })
        assertEquals("Only page text remains", memory.signature.anchors.single().text.value)
        assertTrue(memory.evidence.all { it.evidenceClass == MemoryEvidenceClass.DIRECT })
    }

    @Test
    fun `does not invent TIME when EXIF has camera but no date taken`() = runTest {
        val asset = asset(type = AssetType.PHOTO)
        val facts = listOf(
            AssetMemoryFact(
                MemoryEvidenceKind.OCR_TEXT,
                "image:whole",
                "Cafe menu board",
                "photo-ocr-v1",
            ),
            AssetMemoryFact(
                MemoryEvidenceKind.SOURCE_METADATA,
                "exif:fields",
                "Camera: Pixel 8; Dimensions: 640 × 480",
                "image-exif-v1",
            ),
        )
        val memory = (
            assembler(asset, facts)(asset.identity) as AssetMemoryAssemblyResult.Persisted
            ).memory

        assertNull(memory.signature.anchors.firstOrNull { it.kind == MemoryAnchorKind.TIME })
        assertTrue(memory.signature.anchors.any { it.kind == MemoryAnchorKind.TEXT })
        assertTrue(memory.evidence.any { it.locator.value == "exif:fields" })
    }

    @Test
    fun `skips persistence when extracts contain no usable evidence`() = runTest {
        val asset = asset()
        val repository = FakeMemoryRepository()
        val result = assembler(asset, emptyList(), repository)(asset.identity)

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
        val result = assembler(asset, facts)(asset.identity)

        val memory = (result as AssetMemoryAssemblyResult.Persisted).memory
        assertEquals("Cafe receipt total 12.50", memory.signature.summary.text.value)
        assertTrue(memory.evidence.all { it.evidenceClass == MemoryEvidenceClass.DIRECT })
        assertEquals(
            MemoryEvidenceKind.OCR_TEXT,
            memory.evidence.first { it.id == memory.signature.summary.evidenceIds.single() }.kind,
        )
    }

    @Test
    fun `rejects dimension-only EXIF noise as unusable evidence`() = runTest {
        val asset = asset(type = AssetType.PHOTO)
        val result = assembler(
            asset,
            listOf(
                AssetMemoryFact(
                    MemoryEvidenceKind.SOURCE_METADATA,
                    "exif:fields",
                    "Dimensions: 640 × 480; Orientation: 1",
                    "image-exif-v1",
                ),
            ),
        )(asset.identity)

        assertEquals(AssetMemoryAssemblyResult.NoUsableEvidence, result)
    }

    @Test
    fun `persists every usable fact when more than eight are available`() = runTest {
        val asset = asset()
        val facts = (1..12).map { page ->
            AssetMemoryFact(
                MemoryEvidenceKind.DOCUMENT_TEXT,
                "pdf:page:$page",
                "Page $page body for Atlas launch notes",
                "pdf-extraction-v1",
            )
        }
        val result = assembler(asset, facts)(asset.identity)

        val memory = (result as AssetMemoryAssemblyResult.Persisted).memory
        assertEquals(12, memory.evidence.size)
        assertTrue(memory.evidence.size > 8)
        assertTrue(
            memory.evidence.all {
                it.excerpt.value.length <=
                    AssembleAssetMemoryFromExtractionFacts.MAX_EVIDENCE_CHARS_PER_ITEM
            },
        )
        assertTrue(memory.evidence.all { it.evidenceClass == MemoryEvidenceClass.DIRECT })
        assertEquals(
            AssembleAssetMemoryFromExtractionFacts.ASSEMBLY_SCHEMA,
            memory.assemblySchemaVersion,
        )
        assertEquals("asset-memory-facts-v4", memory.assemblySchemaVersion.value)
    }

    @Test
    fun `summary stays within display bound regardless of evidence volume`() = runTest {
        val asset = asset()
        val longPage = "Atlas ".repeat(80) // well over MAX_SUMMARY_CHARS
        val facts = (1..10).map { page ->
            AssetMemoryFact(
                MemoryEvidenceKind.DOCUMENT_TEXT,
                "pdf:page:$page",
                if (page == 1) longPage else "Secondary page $page",
                "pdf-extraction-v1",
            )
        }
        val memory = (
            assembler(asset, facts)(asset.identity) as AssetMemoryAssemblyResult.Persisted
            ).memory

        assertEquals(10, memory.evidence.size)
        assertTrue(
            memory.signature.summary.text.value.length <=
                AssembleAssetMemoryFromExtractionFacts.MAX_SUMMARY_CHARS,
        )
        assertEquals(
            AssembleAssetMemoryFromExtractionFacts.MAX_SUMMARY_CHARS,
            memory.signature.summary.text.value.length,
        )
    }

    @Test
    fun `truncates a single pathological fact to the per-item character bound`() = runTest {
        val asset = asset()
        val oversized = "x".repeat(
            AssembleAssetMemoryFromExtractionFacts.MAX_EVIDENCE_CHARS_PER_ITEM + 250,
        )
        val memory = (
            assembler(
                asset,
                listOf(
                    AssetMemoryFact(
                        MemoryEvidenceKind.DOCUMENT_TEXT,
                        "pdf:page:1",
                        oversized,
                        "pdf-extraction-v1",
                    ),
                ),
            )(asset.identity) as AssetMemoryAssemblyResult.Persisted
            ).memory

        assertEquals(1, memory.evidence.size)
        assertEquals(
            AssembleAssetMemoryFromExtractionFacts.MAX_EVIDENCE_CHARS_PER_ITEM,
            memory.evidence.single().excerpt.value.length,
        )
    }

    @Test
    fun `changed fingerprint creates a new revision without rewriting prior history`() = runTest {
        val first = asset(fingerprint = "fp-1")
        val second = asset(fingerprint = "fp-2")
        val repository = FakeMemoryRepository()
        val facts = listOf(
            AssetMemoryFact(MemoryEvidenceKind.OCR_TEXT, "image:whole", "Receipt 42", "ocr-v1"),
        )
        val firstMemory = (assembler(first, facts, repository)(first.identity)
            as AssetMemoryAssemblyResult.Persisted).memory
        val secondMemory = (assembler(second, facts, repository)(second.identity)
            as AssetMemoryAssemblyResult.Persisted).memory

        assertEquals(firstMemory.id, secondMemory.id)
        assertTrue(firstMemory.revisionId != secondMemory.revisionId)
        assertTrue(firstMemory.evidence.all { it.evidenceClass == MemoryEvidenceClass.DIRECT })
        assertTrue(secondMemory.evidence.all { it.evidenceClass == MemoryEvidenceClass.DIRECT })
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

    override suspend fun countMeaningIndexCandidates(): Int = history.size

    override suspend fun listCurrentReadySummaries(limit: Int) = emptyList<MemoryEmbeddingSummary>()

    override suspend fun listMeaningIndexSummaries(limit: Int) = emptyList<MemoryEmbeddingSummary>()

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

    override suspend fun findPdfPageEvidenceIds(
        revisionIds: Collection<MemoryRevisionId>,
    ) = emptyMap<MemoryRevisionId, Map<Int, MemoryEvidenceId>>()

    override suspend fun findEvidenceSearchRows(
        revisionIds: Collection<MemoryRevisionId>,
    ) = emptyMap<MemoryRevisionId, Map<MemoryEvidenceId, MemoryEvidenceSearchRow>>()

    override suspend fun findOcrTextEvidenceForEmbedding(
        revisionIds: Collection<MemoryRevisionId>,
    ) = emptyMap<MemoryRevisionId, List<MemoryEvidenceSearchRow>>()

    override suspend fun findSignatureAnchors(
        revisionIds: Collection<MemoryRevisionId>,
    ) = emptyMap<MemoryRevisionId, List<com.memora.app.domain.memory.MemoryAnchor>>()
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
