package com.memora.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface MemoryDao {
    @Query(
        """
        SELECT * FROM memories
        WHERE source_id = :sourceId
          AND source_asset_key = :sourceAssetKey
          AND fingerprint = :fingerprint
          AND assembly_schema_version = :assemblySchemaVersion
        LIMIT 1
        """,
    )
    suspend fun findHeader(
        sourceId: String,
        sourceAssetKey: String,
        fingerprint: String,
        assemblySchemaVersion: String,
    ): MemoryEntity?

    @Query("SELECT * FROM memory_evidence WHERE revision_id = :revisionId ORDER BY evidence_id")
    suspend fun findEvidence(revisionId: String): List<MemoryEvidenceEntity>

    @Query("SELECT * FROM memory_anchors WHERE revision_id = :revisionId ORDER BY anchor_id")
    suspend fun findAnchors(revisionId: String): List<MemoryAnchorEntity>

    @Query(
        "SELECT * FROM memory_anchor_evidence WHERE revision_id = :revisionId " +
            "ORDER BY anchor_id, evidence_id",
    )
    suspend fun findAnchorEvidence(revisionId: String): List<MemoryAnchorEvidenceEntity>

    @Query(
        "SELECT * FROM memory_summary_evidence WHERE revision_id = :revisionId ORDER BY evidence_id",
    )
    suspend fun findSummaryEvidence(revisionId: String): List<MemorySummaryEvidenceEntity>

    @Query(
        "SELECT * FROM memory_extraction_schemas WHERE revision_id = :revisionId ORDER BY schema_version",
    )
    suspend fun findExtractionSchemas(revisionId: String): List<MemoryExtractionSchemaEntity>

    @Query(
        """
        SELECT COUNT(*) FROM memories AS m
        INNER JOIN assets AS a
          ON a.source_id = m.source_id
         AND a.source_asset_key = m.source_asset_key
         AND a.fingerprint = m.fingerprint
        WHERE m.integrity_state = 'READY'
        """,
    )
    suspend fun countCurrentReady(): Int

    @Query(
        """
        SELECT
            m.revision_id AS revision_id,
            m.memory_id AS memory_id,
            m.summary_text AS summary_text
        FROM memories AS m
        INNER JOIN assets AS a
          ON a.source_id = m.source_id
         AND a.source_asset_key = m.source_asset_key
         AND a.fingerprint = m.fingerprint
        WHERE m.integrity_state = 'READY'
          AND m.summary_text != ''
        ORDER BY m.updated_at_epoch_millis DESC
        LIMIT :limit
        """,
    )
    suspend fun listCurrentReadySummaries(limit: Int): List<MemorySummaryRow>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertHeader(entity: MemoryEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEvidence(entities: List<MemoryEvidenceEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAnchors(entities: List<MemoryAnchorEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAnchorEvidence(entities: List<MemoryAnchorEvidenceEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSummaryEvidence(entities: List<MemorySummaryEvidenceEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertExtractionSchemas(entities: List<MemoryExtractionSchemaEntity>)

    @Transaction
    suspend fun insertAtomic(rows: MemoryRoomRows) {
        insertHeader(rows.header)
        insertExtractionSchemas(rows.extractionSchemas)
        insertEvidence(rows.evidence)
        insertAnchors(rows.anchors)
        insertSummaryEvidence(rows.summaryEvidence)
        insertAnchorEvidence(rows.anchorEvidence)
    }

    @Transaction
    suspend fun findRows(
        sourceId: String,
        sourceAssetKey: String,
        fingerprint: String,
        assemblySchemaVersion: String,
    ): MemoryRoomRows? {
        val header = findHeader(
            sourceId,
            sourceAssetKey,
            fingerprint,
            assemblySchemaVersion,
        ) ?: return null
        val revisionId = header.revisionId
        return MemoryRoomRows(
            header = header,
            extractionSchemas = findExtractionSchemas(revisionId),
            evidence = findEvidence(revisionId),
            anchors = findAnchors(revisionId),
            anchorEvidence = findAnchorEvidence(revisionId),
            summaryEvidence = findSummaryEvidence(revisionId),
        )
    }
}

data class MemoryRoomRows(
    val header: MemoryEntity,
    val extractionSchemas: List<MemoryExtractionSchemaEntity>,
    val evidence: List<MemoryEvidenceEntity>,
    val anchors: List<MemoryAnchorEntity>,
    val anchorEvidence: List<MemoryAnchorEvidenceEntity>,
    val summaryEvidence: List<MemorySummaryEvidenceEntity>,
)

data class MemorySummaryRow(
    @ColumnInfo(name = "revision_id") val revisionId: String,
    @ColumnInfo(name = "memory_id") val memoryId: String,
    @ColumnInfo(name = "summary_text") val summaryText: String,
)
