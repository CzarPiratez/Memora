package com.memora.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.memora.app.domain.intelligence.EmbeddingVector
import com.memora.app.domain.intelligence.MemoryEmbeddingRecord
import com.memora.app.domain.intelligence.ModelVersionIdentity
import com.memora.app.domain.memory.MemoryId
import com.memora.app.domain.memory.MemoryRevisionId
import java.nio.ByteBuffer
import java.nio.ByteOrder

@Entity(
    tableName = "memory_embeddings",
    primaryKeys = ["revision_id", "model_id", "model_version"],
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
data class MemoryEmbeddingEntity(
    @ColumnInfo(name = "revision_id") val revisionId: String,
    @ColumnInfo(name = "memory_id") val memoryId: String,
    @ColumnInfo(name = "model_id") val modelId: String,
    @ColumnInfo(name = "model_version") val modelVersion: String,
    @ColumnInfo(name = "dimensions") val dimensions: Int,
    @ColumnInfo(name = "vector_blob", typeAffinity = ColumnInfo.BLOB) val vectorBlob: ByteArray,
    @ColumnInfo(name = "source_text_fingerprint") val sourceTextFingerprint: String,
    @ColumnInfo(name = "created_at_epoch_ms") val createdAtEpochMs: Long,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MemoryEmbeddingEntity) return false
        return revisionId == other.revisionId &&
            modelId == other.modelId &&
            modelVersion == other.modelVersion &&
            dimensions == other.dimensions &&
            vectorBlob.contentEquals(other.vectorBlob) &&
            sourceTextFingerprint == other.sourceTextFingerprint &&
            createdAtEpochMs == other.createdAtEpochMs &&
            memoryId == other.memoryId
    }

    override fun hashCode(): Int {
        var result = revisionId.hashCode()
        result = 31 * result + memoryId.hashCode()
        result = 31 * result + modelId.hashCode()
        result = 31 * result + modelVersion.hashCode()
        result = 31 * result + dimensions
        result = 31 * result + vectorBlob.contentHashCode()
        result = 31 * result + sourceTextFingerprint.hashCode()
        result = 31 * result + createdAtEpochMs.hashCode()
        return result
    }
}

internal object EmbeddingVectorBlobCodec {
    fun encode(vector: EmbeddingVector): ByteArray {
        val buffer = ByteBuffer.allocate(vector.dimensions * 4).order(ByteOrder.LITTLE_ENDIAN)
        vector.values.forEach { buffer.putFloat(it) }
        return buffer.array()
    }

    fun decode(blob: ByteArray, dimensions: Int): EmbeddingVector {
        require(blob.size == dimensions * 4) {
            "Embedding blob size does not match dimensions."
        }
        val buffer = ByteBuffer.wrap(blob).order(ByteOrder.LITTLE_ENDIAN)
        val values = FloatArray(dimensions) { buffer.float }
        return EmbeddingVector(values)
    }
}

internal fun MemoryEmbeddingRecord.toEntity(): MemoryEmbeddingEntity =
    MemoryEmbeddingEntity(
        revisionId = revisionId.value,
        memoryId = memoryId.value,
        modelId = model.modelId,
        modelVersion = model.version,
        dimensions = vector.dimensions,
        vectorBlob = EmbeddingVectorBlobCodec.encode(vector),
        sourceTextFingerprint = sourceTextFingerprint,
        createdAtEpochMs = createdAtEpochMs,
    )

internal fun MemoryEmbeddingEntity.toDomain(): MemoryEmbeddingRecord =
    MemoryEmbeddingRecord(
        revisionId = MemoryRevisionId(revisionId),
        memoryId = MemoryId(memoryId),
        model = ModelVersionIdentity(modelId = modelId, version = modelVersion),
        vector = EmbeddingVectorBlobCodec.decode(vectorBlob, dimensions),
        sourceTextFingerprint = sourceTextFingerprint,
        createdAtEpochMs = createdAtEpochMs,
    )
