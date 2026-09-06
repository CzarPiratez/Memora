package com.memora.app.data.saf

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.memora.app.data.local.MemoraDatabase
import com.memora.app.data.local.MemoraDatabaseMigrations
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.discovery.DiscoveryRequest
import com.memora.app.domain.discovery.DiscoveryResult
import com.memora.app.domain.discovery.DocumentTreeApproval
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Reads one metadata-only page from a folder that the user has already connected in
 * the target app. It uses the existing private approval record and never creates,
 * opens, changes, copies, or deletes a source document.
 */
@RunWith(AndroidJUnit4::class)
class SafPdfDiscoverySourceIntegrationTest {
    private lateinit var database: MemoraDatabase

    @Before
    fun openExistingApplicationDatabase() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.databaseBuilder(targetContext, MemoraDatabase::class.java, "memora.db")
            .addMigrations(
                MemoraDatabaseMigrations.MIGRATION_1_2,
                MemoraDatabaseMigrations.MIGRATION_2_3,
                MemoraDatabaseMigrations.MIGRATION_3_4,
                MemoraDatabaseMigrations.MIGRATION_12_13,
                MemoraDatabaseMigrations.MIGRATION_13_14,
                MemoraDatabaseMigrations.MIGRATION_14_15,
                MemoraDatabaseMigrations.MIGRATION_15_16,
            ).build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun readsOneBoundedMetadataPageFromAnAlreadyApprovedFolder() = runBlocking {
        val approvalEntity = requireNotNull(database.documentTreeApprovalDao().findAll().firstOrNull()) {
            "Connect a PDF folder in Memora before running this emulator integration test."
        }
        val approval = DocumentTreeApproval(
            sourceId = SourceId(approvalEntity.sourceId),
            treeUri = approvalEntity.treeUri,
            approvedAt = Instant.ofEpochMilli(approvalEntity.approvedAtEpochMillis),
        )
        val source = SafPdfDiscoverySource(
            approval = approval,
            accessValidator = ContentResolverDocumentTreeAccessValidator(
                InstrumentationRegistry.getInstrumentation().targetContext,
            ),
            catalog = ContentResolverSafDocumentTreeCatalog(
                InstrumentationRegistry.getInstrumentation().targetContext,
            ),
        )

        when (val result = source.discover(DiscoveryRequest(batchSize = 2))) {
            is DiscoveryResult.Page -> {
                assertTrue(result.value.assets.size <= 2)
                assertEquals(approval.sourceId, result.value.sourceId)
                assertEquals(approval.sourceId, result.value.checkpoint.sourceId)
            }

            DiscoveryResult.AccessRevoked -> throw AssertionError(
                "Android no longer grants Memora read access to the connected PDF folder.",
            )
            DiscoveryResult.AccessRequired -> throw AssertionError(
                "The connected PDF folder needs Android read access before this test can run.",
            )
            is DiscoveryResult.Failed -> throw AssertionError(
                "The folder metadata query failed: ${result.failure.code}",
            )
        }
    }
}
