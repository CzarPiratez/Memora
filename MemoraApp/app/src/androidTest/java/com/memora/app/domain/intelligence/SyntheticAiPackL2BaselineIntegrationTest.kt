package com.memora.app.domain.intelligence

import android.content.Context
import android.net.ConnectivityManager
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
 * Emulator L2: stage synthetic pack, prove integrity edges offline (local crypto),
 * keep support matrix UNSUPPORTED, tear down staging.
 */
@RunWith(AndroidJUnit4::class)
class SyntheticAiPackL2BaselineIntegrationTest {
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
    fun offline_integrity_and_unsupported_matrix_on_emulator_tier() {
        val payload = SyntheticAiPackBaselineCorpus.validPayload()
        val packFile = File(stagingDir, "pack.bin")
        packFile.writeBytes(payload)

        // Network may be present on the AVD; the harness must not depend on it.
        val connectivity = context.getSystemService(ConnectivityManager::class.java)
        val activeNetwork = connectivity?.activeNetwork
        Log.i(
            LOG_TAG,
            "activeNetworkPresent=${activeNetwork != null} " +
                "(integrity path must remain local regardless)",
        )

        val report = MeasureSyntheticAiPackL2Baseline()(
            deviceTierId = SyntheticAiPackBaselineCorpus.DEVICE_TIER_EMULATOR_MEDIUM_PHONE,
            stagedOnDiskBytes = packFile.length(),
        )

        assertEquals(payload.size.toLong(), report.l1.packOnDiskBytes)
        assertTrue(report.offlineCorePathOk)
        assertTrue(report.priorKnownGoodRetainedAfterCorruptUpdate)
        assertTrue(report.truncatedVerification is AiPackVerificationResult.Rejected)
        assertEquals(CapabilityId.entries.size, report.supportMatrixRows.size)
        assertTrue(report.supportMatrixRows.all { !it.mayReportAvailable() })
        assertTrue(report.visionStillUnavailableProxy())
        assertTrue(report.keywordPathStillRequiresDisclosure)

        Log.i(
            LOG_TAG,
            "tier=${report.deviceTierId} corpus=${report.fixtureCorpusId} " +
                "algo=${report.integrityAlgorithm} claims=${report.claims.size} " +
                "matrixRows=${report.supportMatrixRows.size} " +
                "offlineOk=${report.offlineCorePathOk} " +
                "priorKnownGood=${report.priorKnownGoodRetainedAfterCorruptUpdate}",
        )
    }

    private fun SyntheticAiPackL2BaselineReport.visionStillUnavailableProxy(): Boolean =
        l1.visionStillUnavailable

    private fun clearStaging() {
        if (!::stagingDir.isInitialized) return
        stagingDir.listFiles()?.forEach { it.delete() }
        stagingDir.delete()
    }

    companion object {
        private const val STAGING_DIR_NAME = "memora_synthetic_ai_pack_baseline_l2"
        private const val LOG_TAG = "MemoraAiPackBaselineL2"
    }
}
