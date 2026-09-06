package com.memora.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.memora.app.application.memory.AssembleAssetMemoryFromExtractionFacts
import com.memora.app.domain.intelligence.ModelVersionIdentity
import com.memora.app.domain.memory.MemoryRevisionId
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regression (D-8): the meaning-index drain selected the newest candidates
 * regardless of whether they were already embedded, so every Build re-offered
 * the same rows, they were all skipped as unchanged, and the indexed count
 * never moved while the rest of the corpus stayed unreachable.
 */
@RunWith(AndroidJUnit4::class)
class RoomMemoryRepositoryMeaningIndexSelectionIntegrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var database: MemoraDatabase
    private lateinit var repository: RoomMemoryRepository

    private val model = ModelVersionIdentity(modelId = "test-embedder", version = "1")
    private val otherModel = ModelVersionIdentity(modelId = "test-embedder", version = "2")

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, MemoraDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoomMemoryRepository(database = { database })
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun selection_excludes_revisions_already_embedded_for_this_model() = runBlocking {
        seedMemory(index = 1)
        seedMemory(index = 2)
        seedMemory(index = 3)
        embed(revisionId = revisionOf(3))

        val selected = repository.listMeaningIndexSummaries(model = model, limit = 25)

        assertEquals(
            setOf(revisionOf(1), revisionOf(2)),
            selected.mapTo(mutableSetOf()) { it.revisionId },
        )
    }

    /**
     * The reported symptom: tapping Build repeatedly must advance through the
     * corpus rather than re-offering the newest page forever.
     */
    @Test
    fun consecutive_batches_select_disjoint_revisions_until_drained() = runBlocking {
        repeat(5) { seedMemory(index = it + 1) }

        val seen = mutableSetOf<MemoryRevisionId>()
        var batches = 0
        while (true) {
            val batch = repository.listMeaningIndexSummaries(model = model, limit = 2)
            if (batch.isEmpty()) break
            batches += 1
            assertTrue(
                "Batch $batches re-offered an already-indexed revision.",
                batch.none { it.revisionId in seen },
            )
            batch.forEach { embed(it.revisionId) }
            seen += batch.map { it.revisionId }
            assertTrue("Drain did not converge.", batches <= 5)
        }

        assertEquals(5, seen.size)
        assertEquals(3, batches)
    }

    /**
     * STALE_REINDEX_REQUIRED means the summary is embedded but evidence
     * embeddings are still owed (MIG-05). Excluding it on "already embedded"
     * would deadlock the evidence drain permanently.
     */
    @Test
    fun stale_reindex_revisions_stay_selectable_after_their_summary_is_indexed() = runBlocking {
        seedMemory(index = 1, integrityState = "STALE_REINDEX_REQUIRED")
        embed(revisionId = revisionOf(1))

        val selected = repository.listMeaningIndexSummaries(model = model, limit = 25)

        assertEquals(listOf(revisionOf(1)), selected.map { it.revisionId })
    }

    @Test
    fun never_indexed_revisions_are_offered_before_evidence_gaps() = runBlocking {
        seedMemory(index = 1, integrityState = "STALE_REINDEX_REQUIRED")
        embed(revisionId = revisionOf(1))
        seedMemory(index = 2)

        val selected = repository.listMeaningIndexSummaries(model = model, limit = 1)

        assertEquals(listOf(revisionOf(2)), selected.map { it.revisionId })
    }

    @Test
    fun a_different_model_identity_owes_the_whole_corpus_again() = runBlocking {
        seedMemory(index = 1)
        seedMemory(index = 2)
        embed(revisionOf(1))
        embed(revisionOf(2))

        assertTrue(repository.listMeaningIndexSummaries(model = model, limit = 25).isEmpty())
        assertEquals(
            2,
            repository.listMeaningIndexSummaries(model = otherModel, limit = 25).size,
        )
    }

    @Test
    fun pending_count_agrees_with_what_the_next_batch_would_select() = runBlocking {
        repeat(4) { seedMemory(index = it + 1) }
        embed(revisionOf(1))

        assertEquals(3, repository.countMeaningIndexPending(model))
        assertEquals(3, repository.listMeaningIndexSummaries(model = model, limit = 25).size)

        repository.listMeaningIndexSummaries(model = model, limit = 25)
            .forEach { embed(it.revisionId) }

        assertEquals(0, repository.countMeaningIndexPending(model))
        assertTrue(repository.listMeaningIndexSummaries(model = model, limit = 25).isEmpty())
    }

    @Test
    fun blank_summaries_are_never_offered_or_counted() = runBlocking {
        seedMemory(index = 1, summaryText = "")

        assertEquals(0, repository.countMeaningIndexPending(model))
        assertTrue(repository.listMeaningIndexSummaries(model = model, limit = 25).isEmpty())
    }

    private fun revisionOf(index: Int) = MemoryRevisionId("rev-$index")

    private suspend fun seedMemory(
        index: Int,
        integrityState: String = "READY",
        summaryText: String = "Memory summary $index",
    ) {
        val sourceAssetKey = "tree:asset-$index.pdf"
        val fingerprint = "fp-$index"
        database.assetDao().upsert(
            AssetEntity(
                sourceId = SOURCE_ID,
                sourceAssetKey = sourceAssetKey,
                assetType = "PDF",
                location = "content://test/$index",
                fingerprint = fingerprint,
                discoveredAtEpochMillis = BASE_EPOCH_MS,
                displayName = "asset-$index.pdf",
                sourceModifiedAtEpochMillis = null,
                indexingStatus = "INDEXED",
                indexingAttemptCount = 0,
                failureCode = null,
                failureMessage = null,
            ),
        )
        database.memoryDao().insertHeader(
            MemoryEntity(
                revisionId = revisionOf(index).value,
                memoryId = "mem-$index",
                sourceId = SOURCE_ID,
                sourceAssetKey = sourceAssetKey,
                fingerprint = fingerprint,
                assemblySchemaVersion = ASSEMBLY_SCHEMA_VERSION,
                integrityState = integrityState,
                summaryText = summaryText,
                createdAtEpochMillis = BASE_EPOCH_MS,
                // Ascending so the newest rows are the highest index, matching
                // the ordering that made the original defect reproducible.
                updatedAtEpochMillis = BASE_EPOCH_MS + index,
            ),
        )
    }

    private suspend fun embed(
        revisionId: MemoryRevisionId,
        forModel: ModelVersionIdentity = model,
    ) {
        database.memoryEmbeddingDao().upsert(
            MemoryEmbeddingEntity(
                revisionId = revisionId.value,
                memoryId = revisionId.value.replace("rev-", "mem-"),
                modelId = forModel.modelId,
                modelVersion = forModel.version,
                dimensions = 1,
                vectorBlob = ByteArray(FLOAT_BYTES),
                sourceTextFingerprint = "fp-summary-${revisionId.value}",
                createdAtEpochMs = BASE_EPOCH_MS,
            ),
        )
    }

    private companion object {
        const val SOURCE_ID = "saf-pdf-test"
        const val BASE_EPOCH_MS = 1_720_000_000_000L
        const val FLOAT_BYTES = 4

        val ASSEMBLY_SCHEMA_VERSION: String =
            AssembleAssetMemoryFromExtractionFacts.ASSEMBLY_SCHEMA.value
    }
}
