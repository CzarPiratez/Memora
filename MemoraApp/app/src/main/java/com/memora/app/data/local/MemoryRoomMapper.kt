package com.memora.app.data.local

import com.memora.app.domain.asset.AssetFingerprint
import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.memory.EvidenceLocator
import com.memora.app.domain.memory.Memory
import com.memora.app.domain.memory.MemoryAnchor
import com.memora.app.domain.memory.MemoryAnchorId
import com.memora.app.domain.memory.MemoryAnchorKind
import com.memora.app.domain.memory.MemoryAssemblySchemaVersion
import com.memora.app.domain.memory.MemoryEvidence
import com.memora.app.domain.memory.MemoryEvidenceClass
import com.memora.app.domain.memory.MemoryEvidenceId
import com.memora.app.domain.memory.MemoryEvidenceKind
import com.memora.app.domain.memory.MemoryId
import com.memora.app.domain.memory.MemoryIntegrityState
import com.memora.app.domain.memory.MemoryRevisionId
import com.memora.app.domain.memory.MemorySignature
import com.memora.app.domain.memory.MemorySummary
import com.memora.app.domain.memory.MemoryText
import java.time.Instant

internal object MemoryRoomMapper {
    fun toRows(memory: Memory): MemoryRoomRows {
        val revisionId = memory.revisionId.value
        return MemoryRoomRows(
            header = MemoryEntity(
                revisionId = revisionId,
                memoryId = memory.id.value,
                sourceId = memory.assetIdentity.sourceId.value,
                sourceAssetKey = memory.assetIdentity.sourceAssetKey.value,
                fingerprint = memory.assetFingerprint.value,
                assemblySchemaVersion = memory.assemblySchemaVersion.value,
                integrityState = memory.integrityState.name,
                summaryText = memory.signature.summary.text.value,
                createdAtEpochMillis = memory.createdAt.toEpochMilli(),
                updatedAtEpochMillis = memory.updatedAt.toEpochMilli(),
            ),
            extractionSchemas = memory.extractionSchemaVersions.sorted().map {
                MemoryExtractionSchemaEntity(revisionId, it)
            },
            evidence = memory.evidence.map {
                MemoryEvidenceEntity(
                    revisionId = revisionId,
                    evidenceId = it.id.value,
                    evidenceKind = it.kind.name,
                    evidenceClass = it.evidenceClass.name,
                    locator = it.locator.value,
                    excerpt = it.excerpt.value,
                )
            },
            anchors = memory.signature.anchors.map {
                MemoryAnchorEntity(
                    revisionId = revisionId,
                    anchorId = it.id.value,
                    anchorKind = it.kind.name,
                    anchorText = it.text.value,
                )
            },
            anchorEvidence = memory.signature.anchors.flatMap { anchor ->
                anchor.evidenceIds.map { evidenceId ->
                    MemoryAnchorEvidenceEntity(revisionId, anchor.id.value, evidenceId.value)
                }
            },
            summaryEvidence = memory.signature.summary.evidenceIds.map {
                MemorySummaryEvidenceEntity(revisionId, it.value)
            },
        )
    }

    fun toDomain(rows: MemoryRoomRows): Memory {
        val anchorEvidence = rows.anchorEvidence.groupBy { it.anchorId }
        return Memory(
            id = MemoryId(rows.header.memoryId),
            revisionId = MemoryRevisionId(rows.header.revisionId),
            assetIdentity = AssetIdentity(
                SourceId(rows.header.sourceId),
                SourceAssetKey(rows.header.sourceAssetKey),
            ),
            assetFingerprint = AssetFingerprint(rows.header.fingerprint),
            assemblySchemaVersion = MemoryAssemblySchemaVersion(
                rows.header.assemblySchemaVersion,
            ),
            extractionSchemaVersions = rows.extractionSchemas.mapTo(linkedSetOf()) {
                it.schemaVersion
            },
            integrityState = MemoryIntegrityState.valueOf(rows.header.integrityState),
            signature = MemorySignature(
                summary = MemorySummary(
                    text = MemoryText(rows.header.summaryText),
                    evidenceIds = rows.summaryEvidence.mapTo(linkedSetOf()) {
                        MemoryEvidenceId(it.evidenceId)
                    },
                ),
                anchors = rows.anchors.map { anchor ->
                    MemoryAnchor(
                        id = MemoryAnchorId(anchor.anchorId),
                        kind = MemoryAnchorKind.valueOf(anchor.anchorKind),
                        text = MemoryText(anchor.anchorText),
                        evidenceIds = anchorEvidence[anchor.anchorId]
                            .orEmpty()
                            .mapTo(linkedSetOf()) { MemoryEvidenceId(it.evidenceId) },
                    )
                },
            ),
            evidence = rows.evidence.map {
                MemoryEvidence(
                    id = MemoryEvidenceId(it.evidenceId),
                    kind = MemoryEvidenceKind.valueOf(it.evidenceKind),
                    evidenceClass = MemoryEvidenceClass.valueOf(it.evidenceClass),
                    locator = EvidenceLocator(it.locator),
                    excerpt = MemoryText(it.excerpt),
                )
            },
            createdAt = Instant.ofEpochMilli(rows.header.createdAtEpochMillis),
            updatedAt = Instant.ofEpochMilli(rows.header.updatedAtEpochMillis),
        )
    }
}
