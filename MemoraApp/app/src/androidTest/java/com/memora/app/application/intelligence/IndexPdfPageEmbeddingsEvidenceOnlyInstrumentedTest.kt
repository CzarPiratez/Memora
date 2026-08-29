package com.memora.app.application.intelligence

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.memora.app.data.local.MemoraDatabase
import com.memora.app.data.local.MemoryEntity
import com.memora.app.data.local.MemoryEvidenceEntity
import com.memora.app.data.local.RoomMemoryEvidenceEmbeddingStore
import com.memora.app.domain.intelligence.CapabilityAvailability
import com.memora.app.domain.intelligence.CapabilityLimits
import com.memora.app.domain.intelligence.EmbeddingEncodeResult
import com.memora.app.domain.intelligence.EmbeddingEngine
import com.memora.app.domain.intelligence.EmbeddingVector
import com.memora.app.domain.intelligence.MemoryEvidenceEmbeddingStore
import com.memora.app.domain.intelligence.ModelVersionIdentity
import com.memora.app.domain.memory.MemoryEvidenceId
import com.memora.app.domain.memory.MemoryId
import com.memora.app.domain.memory.MemoryRevisionId
import com.memora.app.domain.memory.PdfPageEvidenceLocator
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * MIG-05 step 4 evidence-only index proof: [IndexPdfPageEmbeddings] writes
 * [MemoryEvidenceEmbeddingStore] with a real evidence id (`e{n}`). No page
 * embedding DAO / store remains after PdfPageEmbedding* retirement.
 */
@RunWith(AndroidJUnit4::class)
class IndexPdfPageEmbeddingsEvidenceOnlyInstrumentedTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var database: MemoraDatabase
    private lateinit var evidenceStore: MemoryEvidenceEmbeddingStore

    private val revisionId = MemoryRevisionId("rev-evidence-only-e2e")
    private val memoryId = MemoryId("mem-evidence-only-e2e")
    private val evidenceId = MemoryEvidenceId("e3")
    private val pageNumber = 2
    private val pageText = "invoice total due on page two"
    private val fixedVector = EmbeddingVector(floatArrayOf(0.1f, 0.2f, 0.3f, 0.4f))
    private val model = ModelVersionIdentity(modelId = "evidence-only-test-embed", version = "0.0.1")

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, MemoraDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        assertEquals(15, database.openHelper.readableDatabase.version)
        evidenceStore = RoomMemoryEvidenceEmbeddingStore(
            dao = { database.memoryEvidenceEmbeddingDao() },
        )
        assertFalse(tableExists("pdf_page_embeddings"))
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun evidence_only_write_persists_evidence_row_with_real_evidence_id() = runBlocking {
        seedMemoryWithPdfPageEvidence(evidenceId = evidenceId.value, locatorPage = pageNumber)

        val engine = FixedAvailableEmbeddingEngine(model = model, vector = fixedVector)
        val useCase = IndexPdfPageEmbeddings(engine, evidenceStore)

        val result = useCase(
            candidates = listOf(
                PdfPageEmbeddingCandidate(
                    revisionId = revisionId,
                    memoryId = memoryId,
                    pageNumber = pageNumber,
                    pageText = pageText,
                    evidenceId = evidenceId,
                ),
            ),
            nowEpochMs = 1_720_000_000_000L,
        )

        assertTrue(result is IndexPdfPageEmbeddingsResult.Completed)
        val completed = result as IndexPdfPageEmbeddingsResult.Completed
        assertEquals(1, completed.indexed)
        assertEquals(0, completed.unresolvedEvidence)
        assertEquals(0, completed.failed)

        val evidence = evidenceStore.find(revisionId, evidenceId, model)
        assertNotNull(evidence)
        assertEquals(evidenceId, evidence!!.evidenceId)
        assertEquals("e3", evidence.evidenceId.value)
        assertNull(PdfPageEvidenceLocator.parsePageNumber(evidence.evidenceId.value))
        assertEquals(fixedVector.values.toList(), evidence.vector.values.toList())
        assertEquals(model, evidence.model)

        assertEquals(
            1,
            database.memoryEvidenceEmbeddingDao().countForModel(model.modelId, model.version),
        )
        val evidenceEntity = database.memoryEvidenceEmbeddingDao().find(
            revisionId = revisionId.value,
            evidenceId = evidenceId.value,
            modelId = model.modelId,
            modelVersion = model.version,
        )
        assertNotNull(evidenceEntity)
        assertEquals("e3", evidenceEntity!!.evidenceId)
        assertFalse(evidenceEntity.evidenceId.startsWith("pdf:page:"))
        assertFalse(tableExists("pdf_page_embeddings"))
    }

    @Test
    fun unresolved_evidence_id_writes_nothing() = runBlocking {
        seedMemoryWithPdfPageEvidence(evidenceId = "e9", locatorPage = 9)

        val engine = FixedAvailableEmbeddingEngine(model = model, vector = fixedVector)
        val useCase = IndexPdfPageEmbeddings(engine, evidenceStore)

        val result = useCase(
            candidates = listOf(
                PdfPageEmbeddingCandidate(
                    revisionId = revisionId,
                    memoryId = memoryId,
                    pageNumber = 9,
                    pageText = "orphan page without resolved id",
                    evidenceId = null,
                ),
            ),
            nowEpochMs = 1_720_000_000_100L,
        )

        assertTrue(result is IndexPdfPageEmbeddingsResult.Completed)
        val completed = result as IndexPdfPageEmbeddingsResult.Completed
        assertEquals(0, completed.indexed)
        assertEquals(1, completed.unresolvedEvidence)
        assertEquals(1, completed.failed)
        assertEquals(0, evidenceStore.countForModel(model))
        assertNull(evidenceStore.find(revisionId, MemoryEvidenceId("e9"), model))
        assertEquals(
            0,
            database.memoryEvidenceEmbeddingDao().countForModel(model.modelId, model.version),
        )
    }

    @Test
    fun search_by_meaning_constructor_depends_on_evidence_store_not_page_store_type() {
        val constructors = SearchAssetMemoriesByMeaning::class.java.declaredConstructors
        assertTrue(constructors.isNotEmpty())
        val parameterTypes = constructors.flatMap { it.parameterTypes.toList() }.toSet()
        assertTrue(
            "SearchAssetMemoriesByMeaning must take MemoryEvidenceEmbeddingStore",
            parameterTypes.any { MemoryEvidenceEmbeddingStore::class.java.isAssignableFrom(it) },
        )
        assertFalse(
            "PdfPageEmbeddingStore type must not exist on Search constructors",
            parameterTypes.any { it.simpleName.contains("PdfPageEmbedding") },
        )
        assertFalse(
            "SearchAssetMemoriesByMeaning must not take SavedPdfPageTextSource",
            parameterTypes.any {
                it.name.contains("SavedPdfPageTextSource")
            },
        )
    }

    private fun tableExists(tableName: String): Boolean {
        database.openHelper.readableDatabase.query(
            "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
            arrayOf(tableName),
        ).use { cursor ->
            return cursor.moveToFirst()
        }
    }

    private suspend fun seedMemoryWithPdfPageEvidence(
        evidenceId: String,
        locatorPage: Int,
    ) {
        database.memoryDao().insertHeader(
            MemoryEntity(
                revisionId = revisionId.value,
                memoryId = memoryId.value,
                sourceId = "saf-pdf-test",
                sourceAssetKey = "tree:evidence-only.pdf",
                fingerprint = "fp-evidence-only-1",
                assemblySchemaVersion = "asset-memory-facts-v4",
                integrityState = "READY",
                summaryText = "Evidence-only device fixture",
                createdAtEpochMillis = 1_720_000_000_000L,
                updatedAtEpochMillis = 1_720_000_000_000L,
            ),
        )
        database.memoryDao().insertEvidence(
            listOf(
                MemoryEvidenceEntity(
                    revisionId = revisionId.value,
                    evidenceId = evidenceId,
                    evidenceKind = "DOCUMENT_TEXT",
                    evidenceClass = "DIRECT",
                    locator = PdfPageEvidenceLocator.formatLocator(locatorPage),
                    excerpt = pageText,
                ),
            ),
        )
        val seeded = database.memoryDao().findEvidence(revisionId.value).single()
        assertEquals(evidenceId, seeded.evidenceId)
        assertEquals(PdfPageEvidenceLocator.formatLocator(locatorPage), seeded.locator)
        assertNull(PdfPageEvidenceLocator.parsePageNumber(seeded.evidenceId))
    }

    private class FixedAvailableEmbeddingEngine(
        private val model: ModelVersionIdentity,
        private val vector: EmbeddingVector,
    ) : EmbeddingEngine {
        override fun availability(): CapabilityAvailability =
            CapabilityAvailability.Available(model)

        override fun limits(): CapabilityLimits? = null

        override fun embedText(text: String): EmbeddingEncodeResult {
            require(text.isNotBlank())
            return EmbeddingEncodeResult.Success(vector, model)
        }
    }
}
