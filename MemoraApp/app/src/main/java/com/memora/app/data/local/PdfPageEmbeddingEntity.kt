package com.memora.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.memora.app.domain.intelligence.ModelVersionIdentity
import com.memora.app.domain.intelligence.PdfPageEmbeddingRecord
import com.memora.app.domain.memory.MemoryId
import com.memora.app.domain.memory.MemoryRevisionId

@Entity(
    tableName = "pdf_page_embeddings",
    primaryKeys = ["revision_id", "page_number", "model_id", "model_version"],
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
    ],
)
data class PdfPageEmbeddingEntity(
    @ColumnInfo(name = "revision_id") val revisionId: String,
    @ColumnInfo(name = "memory_id") val memoryId: String,
    @ColumnInfo(name = "page_number") val pageNumber: Int,
    @ColumnInfo(name = "model_id") val modelId: String,
    @ColumnInfo(name = "model_version") val modelVersion: String,
    @ColumnInfo(name = "dimensions") val dimensions: Int,
    @ColumnInfo(name = "vector_blob", typeAffinity = ColumnInfo.BLOB) val vectorBlob: ByteArray,
    @ColumnInfo(name = "source_text_fingerprint") val sourceTextFingerprint: String,
    @ColumnInfo(name = "created_at_epoch_ms") val createdAtEpochMs: Long,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PdfPageEmbeddingEntity) return false
        return revisionId == other.revisionId &&
            memoryId == other.memoryId &&
            pageNumber == other.pageNumber &&
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
        result = 31 * result + pageNumber
        result = 31 * result + modelId.hashCode()
        result = 31 * result + modelVersion.hashCode()
        result = 31 * result + dimensions
        result = 31 * result + vectorBlob.contentHashCode()
        result = 31 * result + sourceTextFingerprint.hashCode()
        result = 31 * result + createdAtEpochMs.hashCode()
        return result
    }
}

internal fun PdfPageEmbeddingRecord.toEntity(): PdfPageEmbeddingEntity =
    PdfPageEmbeddingEntity(
        revisionId = revisionId.value,
        memoryId = memoryId.value,
        pageNumber = pageNumber,
        modelId = model.modelId,
        modelVersion = model.version,
        dimensions = vector.dimensions,
        vectorBlob = EmbeddingVectorBlobCodec.encode(vector),
        sourceTextFingerprint = sourceTextFingerprint,
        createdAtEpochMs = createdAtEpochMs,
    )

internal fun PdfPageEmbeddingEntity.toDomain(): PdfPageEmbeddingRecord =
    PdfPageEmbeddingRecord(
        revisionId = MemoryRevisionId(revisionId),
        memoryId = MemoryId(memoryId),
        pageNumber = pageNumber,
        model = ModelVersionIdentity(modelId = modelId, version = modelVersion),
        vector = EmbeddingVectorBlobCodec.decode(vectorBlob, dimensions),
        sourceTextFingerprint = sourceTextFingerprint,
        createdAtEpochMs = createdAtEpochMs,
    )
