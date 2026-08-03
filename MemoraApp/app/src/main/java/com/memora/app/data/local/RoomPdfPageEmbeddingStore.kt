package com.memora.app.data.local

import com.memora.app.domain.intelligence.ModelVersionIdentity
import com.memora.app.domain.intelligence.PdfPageEmbeddingRecord
import com.memora.app.domain.intelligence.PdfPageEmbeddingStore
import com.memora.app.domain.memory.MemoryRevisionId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class RoomPdfPageEmbeddingStore(
    private val dao: () -> PdfPageEmbeddingDao,
) : PdfPageEmbeddingStore {
    override fun find(
        revisionId: MemoryRevisionId,
        pageNumber: Int,
        model: ModelVersionIdentity,
    ): PdfPageEmbeddingRecord? = io {
        dao().find(
            revisionId = revisionId.value,
            pageNumber = pageNumber,
            modelId = model.modelId,
            modelVersion = model.version,
        )?.toDomain()
    }

    override fun upsert(record: PdfPageEmbeddingRecord) {
        io { dao().upsert(record.toEntity()) }
    }

    override fun countForModel(model: ModelVersionIdentity): Int = io {
        dao().countForModel(modelId = model.modelId, modelVersion = model.version)
    }

    override fun listForModel(model: ModelVersionIdentity): List<PdfPageEmbeddingRecord> = io {
        dao().listForModel(modelId = model.modelId, modelVersion = model.version)
            .map { it.toDomain() }
    }

    private fun <T> io(block: suspend () -> T): T =
        runBlocking(Dispatchers.IO) { block() }
}
