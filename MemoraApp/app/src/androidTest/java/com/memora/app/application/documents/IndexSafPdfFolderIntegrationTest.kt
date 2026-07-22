package com.memora.app.application.documents

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.memora.app.application.discovery.DiscoverSourcePage
import com.memora.app.application.discovery.PersistDiscoveryPage
import com.memora.app.application.discovery.ProcessDiscoveryResult
import com.memora.app.data.local.MemoraDatabase
import com.memora.app.data.local.MemoraDatabaseMigrations
import com.memora.app.data.local.RoomDiscoveryCheckpointRepository
import com.memora.app.data.local.RoomDiscoveryPageStore
import com.memora.app.data.local.RoomDocumentTreeApprovalRepository
import com.memora.app.data.saf.ContentResolverDocumentTreeAccessValidator
import com.memora.app.data.saf.ContentResolverSafDocumentTreeCatalog
import com.memora.app.data.saf.SafPdfDiscoverySourceFactory
import com.memora.app.domain.asset.IndexingStatus
import com.memora.app.domain.discovery.DiscoveryPage
import com.memora.app.domain.discovery.DiscoveryPageStore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises one existing user-approved PDF folder through SAF metadata discovery and
 * atomic placeholder/checkpoint persistence. The output database is in-memory; this
 * test never creates, opens, changes, copies, extracts, or deletes a source document.
 */
@RunWith(AndroidJUnit4::class)
class IndexSafPdfFolderIntegrationTest {
    private lateinit var applicationDatabase: MemoraDatabase
    private lateinit var outputDatabase: MemoraDatabase
    private lateinit var store: RecordingRoomPageStore

    @Before
    fun openDependencies() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        applicationDatabase = Room.databaseBuilder(context, MemoraDatabase::class.java, "memora.db")
            .addMigrations(
                MemoraDatabaseMigrations.MIGRATION_1_2,
                MemoraDatabaseMigrations.MIGRATION_2_3,
            )
            .build()
        outputDatabase = Room.inMemoryDatabaseBuilder(context, MemoraDatabase::class.java).build()
        store = RecordingRoomPageStore(RoomDiscoveryPageStore(outputDatabase))
    }

    @After
    fun closeDatabases() {
        outputDatabase.close()
        applicationDatabase.close()
    }

    @Test
    fun persistsOneLiveBoundedPdfPageAndCheckpointInAnIsolatedDatabase() = runBlocking {
        val approvalRepository = RoomDocumentTreeApprovalRepository(
            applicationDatabase.documentTreeApprovalDao(),
        )
        val approval = requireNotNull(approvalRepository.findAll().firstOrNull()) {
            "Connect a PDF folder in Memora before running this emulator integration test."
        }
        val indexer = IndexSafPdfFolder(
            approvalRepository = approvalRepository,
            sourceFactory = SafPdfDiscoverySourceFactory(
                accessValidator = ContentResolverDocumentTreeAccessValidator(
                    InstrumentationRegistry.getInstrumentation().targetContext,
                ),
                catalog = ContentResolverSafDocumentTreeCatalog(
                    InstrumentationRegistry.getInstrumentation().targetContext,
                ),
            ),
            discoverSourcePage = DiscoverSourcePage(
                checkpointRepository = RoomDiscoveryCheckpointRepository(
                    outputDatabase.discoveryCheckpointDao(),
                ),
                processDiscoveryResult = ProcessDiscoveryResult(PersistDiscoveryPage(store)),
            ),
        )

        when (val outcome = indexer(approval.sourceId, batchSize = 2)) {
            is SafPdfFolderIndexingOutcome.Indexed -> {
                val savedPage = requireNotNull(store.savedPage) {
                    "A successful PDF indexing outcome must persist exactly one discovery page."
                }
                assertEquals(approval.sourceId, outcome.sourceId)
                assertTrue(savedPage.assets.size <= 2)
                assertEquals(savedPage.assets.size, outcome.discoveredAssetCount)
                assertEquals(savedPage.hasMore, outcome.hasMore)
                assertEquals(
                    savedPage.checkpoint.value,
                    outputDatabase.discoveryCheckpointDao()
                        .find(savedPage.sourceId.value)
                        ?.cursorValue,
                )
                savedPage.assets.forEach { asset ->
                    val stored = outputDatabase.assetDao().find(
                        asset.identity.sourceId.value,
                        asset.identity.sourceAssetKey.value,
                    )
                    assertNotNull(stored)
                    assertEquals(IndexingStatus.DISCOVERED.name, stored?.indexingStatus)
                }
            }

            SafPdfFolderIndexingOutcome.SourceNotConnected -> fail(
                "The user-approved PDF folder was not found in Memora's private database.",
            )
            SafPdfFolderIndexingOutcome.AccessRequired -> fail(
                "The approved PDF folder unexpectedly requires Android read access.",
            )
            SafPdfFolderIndexingOutcome.AccessRevoked -> fail(
                "Android no longer grants Memora read access to the connected PDF folder.",
            )
            is SafPdfFolderIndexingOutcome.Failed -> fail(
                "The approved PDF folder could not be indexed: ${outcome.failure.code}",
            )
        }
    }

    private class RecordingRoomPageStore(
        private val delegate: RoomDiscoveryPageStore,
    ) : DiscoveryPageStore {
        var savedPage: DiscoveryPage? = null

        override suspend fun save(page: DiscoveryPage) {
            delegate.save(page)
            savedPage = page
        }
    }
}
