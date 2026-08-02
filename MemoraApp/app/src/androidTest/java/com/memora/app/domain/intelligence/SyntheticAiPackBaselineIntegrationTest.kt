package com.memora.app.domain.intelligence

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Emulator-tier L1 baseline: stage synthetic pack bytes under no-backup private
 * storage, measure on-disk size, verify integrity success/failure, then delete.
 *
 * Logs aggregate metrics only (no user content). Does not mark Vision AVAILABLE.
 */
@RunWith(AndroidJUnit4::class)
class SyntheticAiPackBaselineIntegrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var stagingDir: File

    @Before
    fun setUp() {
        stagingDir = File(context.noBackupFilesDir, STAGING_DIR_NAME)
        clearStaging()
        stagingDir.mkdirs()
    }

    @After
    fun tearDown() {
        clearStaging()
    }

    @Test
    fun stages_verifies_and_rejects_corrupt_pack_on_emulator_tier() {
        val payload = SyntheticAiPackBaselineCorpus.validPayload()
        val packFile = File(stagingDir, "pack.bin")
        packFile.writeBytes(payload)
        assertTrue(packFile.exists())

        val report = MeasureSyntheticAiPackBaseline()(
            deviceTierId = SyntheticAiPackBaselineCorpus.DEVICE_TIER_EMULATOR_MEDIUM_PHONE,
            stagedOnDiskBytes = packFile.length(),
        )

        assertEquals(payload.size.toLong(), report.packOnDiskBytes)
        assertTrue(report.validVerification is AiPackVerificationResult.Verified)
        assertTrue(report.corruptedVerification is AiPackVerificationResult.Rejected)
        assertTrue(report.visionStillUnavailable)
        assertTrue(report.claims.all { it.hasMeasuredEvidence })

        Log.i(
            LOG_TAG,
            "tier=${report.deviceTierId} corpus=${report.fixtureCorpusId} " +
                "algo=${report.integrityAlgorithm} onDiskBytes=${report.packOnDiskBytes} " +
                "downloadBytes=${report.disclosedDownloadBytes} " +
                "storageReqBytes=${report.disclosedStorageRequirementBytes} " +
                "claims=${report.claims.size} visionUnavailable=${report.visionStillUnavailable}",
        )
    }

    private fun clearStaging() {
        if (!::stagingDir.isInitialized) return
        stagingDir.listFiles()?.forEach { it.delete() }
        stagingDir.delete()
    }

    companion object {
        private const val STAGING_DIR_NAME = "memora_synthetic_ai_pack_baseline"
        private const val LOG_TAG = "MemoraAiPackBaseline"
    }
}
