package com.memora.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.memora.app.domain.intelligence.MemoryEvidenceEmbeddingRecord
import com.memora.app.domain.intelligence.ModelVersionIdentity
import com.memora.app.domain.memory.MemoryEvidenceId
import com.memora.app.domain.memory.MemoryId
import com.memora.app.domain.memory.MemoryRevisionId

@Entity(
    tableName = "memory_evidence_embeddings",
    primaryKeys = ["revision_id", "evidence_id", "model_id", "model_version"],
    foreignKeys = [
        ForeignKey(
            entity = MemoryEntity::class,
            parentColumns = ["revision_id"],
            childColumns = ["revision_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["memory_id"]),
        Index(value = ["model_id", "model_version"]),
        Index(value = ["revision_id", "evidence_id"]),
    ],
)
data class MemoryEvidenceEmbeddingEntity(
    @ColumnInfo(name = "revision_id") val revisionId: String,
    @ColumnInfo(name = "memory_id") val memoryId: String,
    @ColumnInfo(name = "evidence_id") val evidenceId: String,
    @ColumnInfo(name = "model_id") val modelId: String,
    @ColumnInfo(name = "model_version") val modelVersion: String,
    @ColumnInfo(name = "dimensions") val dimensions: Int,
    @ColumnInfo(name = "vector_blob", typeAffinity = ColumnInfo.BLOB) val vectorBlob: ByteArray,
    @ColumnInfo(name = "source_text_fingerprint") val sourceTextFingerprint: String,
    @ColumnInfo(name = "created_at_epoch_ms") val createdAtEpochMs: Long,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MemoryEvidenceEmbeddingEntity) return false
        return revisionId == other.revisionId &&
            memoryId == other.memoryId &&
            evidenceId == other.evidenceId &&
            modelId == other.modelId &&
            modelVersion == other.modelVersion &&
            dimensions == other.dimensions &&
            vectorBlob.contentEquals(other.vectorBlob) &&
            sourceTextFingerprint == other.sourceTextFingerprint &&
            createdAtEpochMs == other.createdAtEpochMs
    }

    override fun hashCode(): Int {
        var result = revisionId.hashCode()
        result = 31 * result + memoryId.hashCode()
        result = 31 * result + evidenceId.hashCode()
        result = 31 * result + modelId.hashCode()
        result = 31 * result + modelVersion.hashCode()
        result = 31 * result + dimensions
        result = 31 * result + vectorBlob.contentHashCode()
        result = 31 * result + sourceTextFingerprint.hashCode()
        result = 31 * result + createdAtEpochMs.hashCode()
        return result
    }
}

internal fun MemoryEvidenceEmbeddingRecord.toEntity(): MemoryEvidenceEmbeddingEntity =
    MemoryEvidenceEmbeddingEntity(
        revisionId = revisionId.value,
        memoryId = memoryId.value,
        evidenceId = evidenceId.value,
        modelId = model.modelId,
        modelVersion = model.version,
        dimensions = vector.dimensions,
        vectorBlob = EmbeddingVectorBlobCodec.encode(vector),
        sourceTextFingerprint = sourceTextFingerprint,
        createdAtEpochMs = createdAtEpochMs,
    )

internal fun MemoryEvidenceEmbeddingEntity.toDomain(): MemoryEvidenceEmbeddingRecord =
    MemoryEvidenceEmbeddingRecord(
        revisionId = MemoryRevisionId(revisionId),
        memoryId = MemoryId(memoryId),
        evidenceId = MemoryEvidenceId(evidenceId),
        model = ModelVersionIdentity(modelId = modelId, version = modelVersion),
        vector = EmbeddingVectorBlobCodec.decode(vectorBlob, dimensions),
        sourceTextFingerprint = sourceTextFingerprint,
        createdAtEpochMs = createdAtEpochMs,
    )
