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
          AND m.assembly_schema_version = :assemblySchemaVersion
        """,
    )
    suspend fun countCurrentReady(assemblySchemaVersion: String): Int

    @Query(
        """
        SELECT COUNT(*) FROM memories AS m
        INNER JOIN assets AS a
          ON a.source_id = m.source_id
         AND a.source_asset_key = m.source_asset_key
         AND a.fingerprint = m.fingerprint
        WHERE m.integrity_state IN ('READY', 'STALE_REINDEX_REQUIRED')
          AND m.assembly_schema_version = :assemblySchemaVersion
        """,
    )
    suspend fun countMeaningIndexCandidates(assemblySchemaVersion: String): Int

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
          AND m.assembly_schema_version = :assemblySchemaVersion
          AND m.summary_text != ''
        ORDER BY m.updated_at_epoch_millis DESC
        LIMIT :limit
        """,
    )
    suspend fun listCurrentReadySummaries(
        assemblySchemaVersion: String,
        limit: Int,
    ): List<MemorySummaryRow>

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
        WHERE m.integrity_state IN ('READY', 'STALE_REINDEX_REQUIRED')
          AND m.assembly_schema_version = :assemblySchemaVersion
          AND m.summary_text != ''
        ORDER BY m.updated_at_epoch_millis DESC
        LIMIT :limit
        """,
    )
    suspend fun listMeaningIndexSummaries(
        assemblySchemaVersion: String,
        limit: Int,
    ): List<MemorySummaryRow>

    @Query(
        """
        SELECT m.revision_id FROM memories AS m
        INNER JOIN assets AS a
          ON a.source_id = m.source_id
         AND a.source_asset_key = m.source_asset_key
         AND a.fingerprint = m.fingerprint
        WHERE m.integrity_state = :integrityState
          AND m.assembly_schema_version = :assemblySchemaVersion
        """,
    )
    suspend fun listCurrentRevisionIdsByIntegrity(
        assemblySchemaVersion: String,
        integrityState: String,
    ): List<String>

    @Query(
        """
        UPDATE memories
        SET integrity_state = :toState,
            updated_at_epoch_millis = :nowEpochMs
        WHERE revision_id IN (:revisionIds)
          AND integrity_state = :fromState
        """,
    )
    suspend fun markIntegrityState(
        revisionIds: List<String>,
        fromState: String,
        toState: String,
        nowEpochMs: Long,
    ): Int

    @Query(
        """
        SELECT
            revision_id AS revision_id,
            evidence_id AS evidence_id,
            locator AS locator,
            excerpt AS excerpt
        FROM memory_evidence
        WHERE revision_id IN (:revisionIds)
        ORDER BY revision_id, evidence_id
        """,
    )
    suspend fun findEvidenceSearchRows(
        revisionIds: List<String>,
    ): List<MemoryEvidenceSearchRowEntity>

    @Query(
        """
        SELECT
            revision_id AS revision_id,
            evidence_id AS evidence_id,
            locator AS locator,
            excerpt AS excerpt
        FROM memory_evidence
        WHERE revision_id IN (:revisionIds)
          AND evidence_kind = 'OCR_TEXT'
          AND excerpt != ''
        ORDER BY revision_id, evidence_id
        """,
    )
    suspend fun findOcrTextEvidenceForEmbedding(
        revisionIds: List<String>,
    ): List<MemoryEvidenceSearchRowEntity>

    @Query(
        """
        SELECT
            revision_id AS revision_id,
            evidence_id AS evidence_id,
            locator AS locator,
            excerpt AS excerpt
        FROM memory_evidence
        WHERE revision_id IN (:revisionIds)
          AND evidence_kind = 'NOTE_TEXT'
          AND excerpt != ''
        ORDER BY revision_id, evidence_id
        """,
    )
    suspend fun findNoteTextEvidenceForEmbedding(
        revisionIds: List<String>,
    ): List<MemoryEvidenceSearchRowEntity>

    @Query(
        """
        SELECT * FROM memory_anchors
        WHERE revision_id IN (:revisionIds)
        ORDER BY revision_id, anchor_id
        """,
    )
    suspend fun findAnchorsForRevisions(
        revisionIds: List<String>,
    ): List<MemoryAnchorEntity>

    /**
     * Count of evidence rows on current-fingerprint READY Memories (MIG-06/07).
     * Zero means nothing is available for literal evidence search yet.
     * Pass null [assetType] for all types; otherwise filter to that AssetType name.
     */
    @Query(
        """
        SELECT COUNT(*) FROM memory_evidence AS e
        INNER JOIN memories AS m
          ON m.revision_id = e.revision_id
        INNER JOIN assets AS a
          ON a.source_id = m.source_id
         AND a.source_asset_key = m.source_asset_key
         AND a.fingerprint = m.fingerprint
        WHERE m.integrity_state = 'READY'
          AND m.assembly_schema_version = :assemblySchemaVersion
          AND e.excerpt != ''
          AND (:assetType IS NULL OR a.asset_type = :assetType)
        """,
    )
    suspend fun countCurrentReadyEvidence(
        assemblySchemaVersion: String,
        assetType: String?,
    ): Int

    /**
     * Evidence + distinct-document counts for Find readiness (MIG-07).
     * Pass null [assetType] for all types; otherwise filter to that AssetType name.
     */
    @Query(
        """
        SELECT
            COUNT(*) AS evidence_count,
            COUNT(DISTINCT a.source_id || char(31) || a.source_asset_key) AS document_count
        FROM memory_evidence AS e
        INNER JOIN memories AS m
          ON m.revision_id = e.revision_id
        INNER JOIN assets AS a
          ON a.source_id = m.source_id
         AND a.source_asset_key = m.source_asset_key
         AND a.fingerprint = m.fingerprint
        WHERE m.integrity_state = 'READY'
          AND m.assembly_schema_version = :assemblySchemaVersion
          AND e.excerpt != ''
          AND (:assetType IS NULL OR a.asset_type = :assetType)
        """,
    )
    suspend fun countCurrentReadyEvidenceCorpus(
        assemblySchemaVersion: String,
        assetType: String?,
    ): MemoryEvidenceCorpusCountRow

    /**
     * Literal substring search over [memory_evidence.excerpt] for current-fingerprint
     * READY Memories (MIG-06/07). No FTS; no schema bump.
     * Pass null [assetType] for all types; otherwise filter to that AssetType name.
     */
    @Query(
        """
        SELECT
            m.revision_id AS revision_id,
            m.memory_id AS memory_id,
            m.source_id AS source_id,
            m.source_asset_key AS source_asset_key,
            a.asset_type AS asset_type,
            a.display_name AS display_name,
            e.evidence_id AS evidence_id,
            e.evidence_kind AS evidence_kind,
            e.locator AS locator,
            e.excerpt AS excerpt
        FROM memory_evidence AS e
        INNER JOIN memories AS m
          ON m.revision_id = e.revision_id
        INNER JOIN assets AS a
          ON a.source_id = m.source_id
         AND a.source_asset_key = m.source_asset_key
         AND a.fingerprint = m.fingerprint
        WHERE m.integrity_state = 'READY'
          AND m.assembly_schema_version = :assemblySchemaVersion
          AND e.excerpt != ''
          AND (:assetType IS NULL OR a.asset_type = :assetType)
          AND e.excerpt LIKE '%' || :escapedNeedle || '%' ESCAPE '\'
        ORDER BY
            m.updated_at_epoch_millis DESC,
            e.evidence_id ASC
        LIMIT :limit
        """,
    )
    suspend fun searchCurrentReadyEvidenceByExcerpt(
        escapedNeedle: String,
        assemblySchemaVersion: String,
        limit: Int,
        assetType: String?,
    ): List<MemoryEvidenceLiteralSearchRowEntity>

    @Query(
        """
        SELECT
            m.revision_id AS revision_id,
            m.memory_id AS memory_id,
            m.source_id AS source_id,
            m.source_asset_key AS source_asset_key,
            m.summary_text AS summary_text,
            a.asset_type AS asset_type,
            a.display_name AS display_name
        FROM memories AS m
        INNER JOIN assets AS a
          ON a.source_id = m.source_id
         AND a.source_asset_key = m.source_asset_key
         AND a.fingerprint = m.fingerprint
        WHERE m.integrity_state = 'READY'
          AND m.assembly_schema_version = :assemblySchemaVersion
          AND m.summary_text != ''
          AND m.revision_id IN (:revisionIds)
        """,
    )
    suspend fun findCurrentReadyMeaningLookups(
        assemblySchemaVersion: String,
        revisionIds: List<String>,
    ): List<MemoryMeaningLookupRow>

    @Query(
        """
        SELECT
            mse.revision_id AS revision_id,
            me.locator AS locator
        FROM memory_summary_evidence AS mse
        INNER JOIN memory_evidence AS me
          ON me.revision_id = mse.revision_id
         AND me.evidence_id = mse.evidence_id
        WHERE mse.revision_id IN (:revisionIds)
        ORDER BY mse.revision_id, mse.evidence_id
        """,
    )
    suspend fun findSummaryEvidenceLocators(
        revisionIds: List<String>,
    ): List<MemorySummaryEvidenceLocatorRow>

    @Query(
        """
        SELECT revision_id AS revision_id, evidence_id AS evidence_id, locator AS locator
        FROM memory_evidence
        WHERE revision_id IN (:revisionIds)
        ORDER BY revision_id, evidence_id
        """,
    )
    suspend fun findEvidenceLocators(
        revisionIds: List<String>,
    ): List<MemoryEvidenceLocatorRow>

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

data class MemoryMeaningLookupRow(
    @ColumnInfo(name = "revision_id") val revisionId: String,
    @ColumnInfo(name = "memory_id") val memoryId: String,
    @ColumnInfo(name = "source_id") val sourceId: String,
    @ColumnInfo(name = "source_asset_key") val sourceAssetKey: String,
    @ColumnInfo(name = "summary_text") val summaryText: String,
    @ColumnInfo(name = "asset_type") val assetType: String,
    @ColumnInfo(name = "display_name") val displayName: String?,
)

data class MemorySummaryEvidenceLocatorRow(
    @ColumnInfo(name = "revision_id") val revisionId: String,
    @ColumnInfo(name = "locator") val locator: String,
)

data class MemoryEvidenceLocatorRow(
    @ColumnInfo(name = "revision_id") val revisionId: String,
    @ColumnInfo(name = "evidence_id") val evidenceId: String,
    @ColumnInfo(name = "locator") val locator: String,
)

data class MemoryEvidenceSearchRowEntity(
    @ColumnInfo(name = "revision_id") val revisionId: String,
    @ColumnInfo(name = "evidence_id") val evidenceId: String,
    @ColumnInfo(name = "locator") val locator: String,
    @ColumnInfo(name = "excerpt") val excerpt: String,
)

/** MIG-06 literal evidence-search projection (joined Memory + Asset identity). */
data class MemoryEvidenceLiteralSearchRowEntity(
    @ColumnInfo(name = "revision_id") val revisionId: String,
    @ColumnInfo(name = "memory_id") val memoryId: String,
    @ColumnInfo(name = "source_id") val sourceId: String,
    @ColumnInfo(name = "source_asset_key") val sourceAssetKey: String,
    @ColumnInfo(name = "asset_type") val assetType: String,
    @ColumnInfo(name = "display_name") val displayName: String?,
    @ColumnInfo(name = "evidence_id") val evidenceId: String,
    @ColumnInfo(name = "evidence_kind") val evidenceKind: String,
    @ColumnInfo(name = "locator") val locator: String,
    @ColumnInfo(name = "excerpt") val excerpt: String,
)
