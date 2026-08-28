package com.memora.app.domain.intelligence

import com.memora.app.domain.asset.AssetFingerprint
import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.memory.AssetMemoryFact
import com.memora.app.domain.memory.MemoryAnchorKind
import com.memora.app.domain.memory.MemoryEvidenceClass
import com.memora.app.domain.memory.MemoryEvidenceKind
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeterministicMemoryBuilderTest {
    private val builder = DeterministicMemoryBuilder()
    private val createdAt = Instant.parse("2026-07-31T10:00:00Z")
    private val identity = AssetIdentity(SourceId("source"), SourceAssetKey("asset"))
    private val fingerprint = AssetFingerprint("fp-1")

    @Test
    fun `assemble with facts produces DIRECT evidence TIME and schema v4`() {
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

        val result = builder.assemble(
            assetIdentity = identity,
            assetFingerprint = fingerprint,
            facts = facts,
            localObservations = emptyList(),
            createdAt = createdAt,
        )

        assertTrue(result is MemoryBuildResult.Success)
        val memory = (result as MemoryBuildResult.Success).memory
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
            MemoryAnchorKind.TIME,
            memory.signature.anchors.single { it.kind == MemoryAnchorKind.TIME }.kind,
        )
        assertEquals("Date taken: 2026:07:31 10:30:00",
            memory.signature.anchors.single { it.kind == MemoryAnchorKind.TIME }.text.value)
        assertEquals(DeterministicMemoryBuilder.ASSEMBLY_SCHEMA, memory.assemblySchemaVersion)
        assertEquals("asset-memory-facts-v4", memory.assemblySchemaVersion.value)
        assertEquals(createdAt, memory.createdAt)
        assertEquals(createdAt, memory.updatedAt)
        assertTrue(builder.availability() is CapabilityAvailability.Available)
    }

    @Test
    fun `empty facts yield NoUsableEvidence`() {
        val result = builder.assemble(
            assetIdentity = identity,
            assetFingerprint = fingerprint,
            facts = emptyList(),
            localObservations = emptyList(),
            createdAt = createdAt,
        )
        assertEquals(MemoryBuildResult.NoUsableEvidence, result)
    }

    @Test
    fun `non-empty localObservations are rejected clearly`() {
        val facts = listOf(
            AssetMemoryFact(
                MemoryEvidenceKind.DOCUMENT_TEXT,
                "pdf:page:1",
                "Body text",
                "pdf-extraction-v1",
            ),
        )
        val result = builder.assemble(
            assetIdentity = identity,
            assetFingerprint = fingerprint,
            facts = facts,
            localObservations = listOf(LocalObservation(observationId = "obs-1")),
            createdAt = createdAt,
        )
        assertTrue(result is MemoryBuildResult.ObservationsUnsupported)
        assertTrue(
            (result as MemoryBuildResult.ObservationsUnsupported).reason.contains("VALIDATED_OBSERVATION"),
        )
    }
}

class UnavailableMemoryBuilderAssembleTest {
    @Test
    fun `unavailable assemble refuses to invent a Memory`() {
        val identity = AssetIdentity(SourceId("source"), SourceAssetKey("asset"))
        val result = UnavailableMemoryBuilder().assemble(
            assetIdentity = identity,
            assetFingerprint = AssetFingerprint("fp-1"),
            facts = listOf(
                AssetMemoryFact(
                    MemoryEvidenceKind.OCR_TEXT,
                    "image:whole",
                    "Cafe receipt",
                    "ocr-v1",
                ),
            ),
            localObservations = emptyList(),
            createdAt = Instant.parse("2026-07-31T10:00:00Z"),
        )
        assertTrue(result is MemoryBuildResult.Unavailable)
        assertTrue(result !is MemoryBuildResult.Success)
    }
}
