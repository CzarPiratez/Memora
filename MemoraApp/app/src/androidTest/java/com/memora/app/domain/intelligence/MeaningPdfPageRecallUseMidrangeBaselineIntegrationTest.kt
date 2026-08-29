package com.memora.app.domain.intelligence

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.memora.app.data.intelligence.MediaPipeEmbeddingEngine
import com.memora.app.data.intelligence.NoBackupOnDeviceEmbeddingModelStore
import java.io.BufferedInputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * M4 midrange_arm64 baseline: score labeled page-recall texts with live
 * Universal Sentence Encoder on a physical midrange host. Prefer the product
 * USE install; else stage from /data/local/tmp or HTTPS into a disposable dir.
 * Does not mark AVAILABLE. Does not change M3 emulator tier.
 */
@RunWith(AndroidJUnit4::class)
class MeaningPdfPageRecallUseMidrangeBaselineIntegrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var stagingRoot: File
    private lateinit var modelStore: MidrangeStagingUseEmbeddingModelStore
    private var engine: MediaPipeEmbeddingEngine? = null

    @Before
    fun setUp() {
        stagingRoot = File(context.noBackupFilesDir, STAGING_DIR_NAME)
        clearStaging()
        stagingRoot.mkdirs()
        modelStore = MidrangeStagingUseEmbeddingModelStore(stagingRoot)
    }

    @After
    fun tearDown() {
        engine?.reset()
        engine = null
        clearStaging()
    }

    @Test
    fun measures_use_page_recall_on_midrange_arm64_tier() {
        stageUseModelForMeasurement()
        assertTrue("USE embedder must be staged for M4", modelStore.isInstalled())

        val liveEngine = MediaPipeEmbeddingEngine(context, modelStore)
        engine = liveEngine
        assertTrue(liveEngine.availability() is CapabilityAvailability.Available)

        val scored = ScoreMeaningPdfPageRecallCorpus.score(liveEngine)
        val report = MeasureOnDeviceMeaningPdfPageRecallBaseline.measure(
            scoreResult = scored,
            deviceTierId = MeaningPdfPageRecallCorpus.DEVICE_TIER_MIDRANGE_ARM64,
        )

        assertEquals(MeaningPdfPageRecallCorpus.CORPUS_ID, report.baseline.fixtureCorpusId)
        assertEquals(3, report.baseline.caseCount)
        assertTrue(report.baseline.claims.all { it.hasMeasuredEvidence })
        assertEquals(
            MeaningPdfPageRecallCorpus.DEVICE_TIER_MIDRANGE_ARM64,
            report.baseline.deviceTierId,
        )
        assertEquals(MediaPipeUniversalSentenceEncoderSpec.MODEL_IDENTITY, report.model)
        assertTrue(report.embedCount >= 3)
        assertTrue(report.embedWallMs >= 0L)
        assertTrue(report.baseline.notes.contains("Does not authorize product AVAILABLE"))

        Log.i(
            LOG_TAG,
            "tier=${report.baseline.deviceTierId} corpus=${report.baseline.fixtureCorpusId} " +
                "model=${report.model.modelId}@${report.model.version} " +
                "cosineHits=${report.baseline.cosineOnlyHitsAt1}/${report.baseline.caseCount} " +
                "boostedHits=${report.baseline.boostedHitsAt1}/${report.baseline.caseCount} " +
                "recommendsE4b=${report.baseline.recommendsE4bForSemanticOnly} " +
                "embeds=${report.embedCount} wallMs=${report.embedWallMs}",
        )
    }

    /**
     * Prefer product USE install (E4b smoke path), then host-pushed tempfile,
     * then HTTPS download into disposable staging.
     */
    private fun stageUseModelForMeasurement() {
        val target = modelStore.modelFile()
        val product = File(
            context.noBackupFilesDir,
            "${NoBackupOnDeviceEmbeddingModelStore.ROOT_DIR_NAME}/" +
                MediaPipeUniversalSentenceEncoderSpec.FILE_NAME,
        )
        val pushed = File(
            "/data/local/tmp/${MediaPipeUniversalSentenceEncoderSpec.FILE_NAME}",
        )
        when {
            product.exists() && product.length() > 0L -> {
                product.copyTo(target, overwrite = true)
            }
            pushed.exists() && pushed.length() > 0L -> {
                pushed.copyTo(target, overwrite = true)
            }
            else -> downloadUseIntoStaging()
        }
    }

    private fun downloadUseIntoStaging() {
        val temp = File(
            stagingRoot,
            "${MediaPipeUniversalSentenceEncoderSpec.FILE_NAME}.tmp",
        )
        val target = modelStore.modelFile()
        temp.delete()
        val connection = (URL(MediaPipeUniversalSentenceEncoderSpec.DOWNLOAD_URL)
            .openConnection() as HttpURLConnection).apply {
            connectTimeout = 30_000
            readTimeout = 300_000
            instanceFollowRedirects = true
            requestMethod = "GET"
        }
        try {
            val code = connection.responseCode
            assertTrue("USE download HTTP $code", code in 200..299)
            BufferedInputStream(connection.inputStream).use { input ->
                temp.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var total = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        total += read
                        assertTrue(
                            "USE exceeded disclosed size limit",
                            total <= MediaPipeUniversalSentenceEncoderSpec.MAX_DOWNLOAD_BYTES,
                        )
                        output.write(buffer, 0, read)
                    }
                }
            }
            assertTrue(temp.exists() && temp.length() > 0L)
            target.delete()
            if (!temp.renameTo(target)) {
                temp.copyTo(target, overwrite = true)
                temp.delete()
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun clearStaging() {
        if (!::stagingRoot.isInitialized) return
        stagingRoot.walkBottomUp().forEach { it.delete() }
    }

    companion object {
        private const val STAGING_DIR_NAME = "memora_m4_use_page_recall_baseline"
        private const val LOG_TAG = "MemoraMeaningPdfM4"
    }
}

private class MidrangeStagingUseEmbeddingModelStore(
    private val rootDir: File,
) : OnDeviceEmbeddingModelStore {
    fun modelFile(): File =
        File(rootDir, MediaPipeUniversalSentenceEncoderSpec.FILE_NAME)

    override fun isInstalled(): Boolean {
        val file = modelFile()
        return file.exists() && file.length() > 0L
    }

    override fun absoluteModelPath(): String? {
        val file = modelFile()
        if (!file.exists() || file.length() <= 0L) return null
        return file.absolutePath
    }

    override fun modelIdentity(): ModelVersionIdentity =
        MediaPipeUniversalSentenceEncoderSpec.MODEL_IDENTITY

    override fun clear() {
        modelFile().delete()
    }
}
