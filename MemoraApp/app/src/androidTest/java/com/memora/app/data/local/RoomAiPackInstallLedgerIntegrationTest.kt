package com.memora.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.memora.app.domain.intelligence.AiPackDisclosureSnapshot
import com.memora.app.domain.intelligence.AiPackInstallState
import com.memora.app.domain.intelligence.AiPackManifest
import com.memora.app.domain.intelligence.AiPackVerificationResult
import com.memora.app.domain.intelligence.EmbeddingFirstAiPackTrack
import com.memora.app.domain.intelligence.LedgerBackedAiPackManager
import com.memora.app.domain.intelligence.ModelVersionIdentity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomAiPackInstallLedgerIntegrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var database: MemoraDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, MemoraDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun persists_active_pack_across_ledger_instances() {
        val first = RoomAiPackInstallLedger { database.aiPackInstallLedgerDao() }
        first.acknowledgeDisclosure(sampleDisclosure())
        first.beginVerification(EmbeddingFirstAiPackTrack.PLANNED_PACK_ID, atEpochMs = 2L)
        first.recordVerifiedActive(sampleActiveManifest(), atEpochMs = 3L)

        val second = RoomAiPackInstallLedger { database.aiPackInstallLedgerDao() }
        val manager = LedgerBackedAiPackManager(second)
        assertEquals(
            AiPackInstallState.ACTIVE,
            manager.installationState(EmbeddingFirstAiPackTrack.PLANNED_PACK_ID),
        )
        assertTrue(
            manager.verifiedManifest(EmbeddingFirstAiPackTrack.PLANNED_PACK_ID)
                is AiPackVerificationResult.Verified,
        )
        assertEquals(1, runBlocking { database.aiPackInstallLedgerDao().count() })
    }

    @Test
    fun empty_table_never_reports_active() {
        val ledger = RoomAiPackInstallLedger { database.aiPackInstallLedgerDao() }
        val manager = LedgerBackedAiPackManager(ledger)
        assertEquals(
            AiPackInstallState.NOT_INSTALLED,
            manager.installationState(EmbeddingFirstAiPackTrack.PLANNED_PACK_ID),
        )
        assertTrue(
            manager.verifiedManifest(EmbeddingFirstAiPackTrack.PLANNED_PACK_ID)
                is AiPackVerificationResult.Rejected,
        )
    }

    private fun sampleDisclosure(): AiPackDisclosureSnapshot = AiPackDisclosureSnapshot(
        packId = EmbeddingFirstAiPackTrack.PLANNED_PACK_ID,
        capability = EmbeddingFirstAiPackTrack.capability,
        model = ModelVersionIdentity(modelId = "embedding-tbd", version = "0.0.0-planned"),
        downloadSizeBytes = 10_000_000L,
        storageRequirementBytes = 12_000_000L,
        license = "TBD-pack-license",
        disclosedAtEpochMs = 1L,
    )

    private fun sampleActiveManifest(): AiPackManifest = AiPackManifest(
        packId = EmbeddingFirstAiPackTrack.PLANNED_PACK_ID,
        capability = EmbeddingFirstAiPackTrack.capability,
        model = ModelVersionIdentity(modelId = "embedding-tbd", version = "0.0.0-planned"),
        compatibleAppVersions = "1.0.0+",
        compatibleSchemaVersions = "memory-schema-1",
        downloadSizeBytes = 10_000_000L,
        storageRequirementBytes = 12_000_000L,
        integrityHash = "sha256:good",
        license = "TBD-pack-license",
        installationState = AiPackInstallState.ACTIVE,
    )
}
