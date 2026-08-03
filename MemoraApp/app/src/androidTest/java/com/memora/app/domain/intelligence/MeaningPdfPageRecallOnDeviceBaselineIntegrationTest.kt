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
 * M2 emulator-tier baseline: stage compact MediaPipe model under disposable
 * no-backup storage, score labeled page-recall texts, measure hit@1, delete
 * staging. Does not touch the product model install directory and does not
 * mark Local Intelligence AVAILABLE.
 */
@RunWith(AndroidJUnit4::class)
class MeaningPdfPageRecallOnDeviceBaselineIntegrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var stagingRoot: File
    private lateinit var modelStore: StagingOnDeviceEmbeddingModelStore
    private var engine: MediaPipeEmbeddingEngine? = null

    @Before
    fun setUp() {
        stagingRoot = File(context.noBackupFilesDir, STAGING_DIR_NAME)
        clearStaging()
        stagingRoot.mkdirs()
        modelStore = StagingOnDeviceEmbeddingModelStore(stagingRoot)
    }

    @After
    fun tearDown() {
        engine?.reset()
        engine = null
        clearStaging()
    }

    @Test
    fun measures_compact_mediapipe_page_recall_on_emulator_tier() {
        stageModelForMeasurement()
        assertTrue("Compact embedder must be staged for M2", modelStore.isInstalled())

        val liveEngine = MediaPipeEmbeddingEngine(context, modelStore)
        engine = liveEngine
        assertTrue(liveEngine.availability() is CapabilityAvailability.Available)

        val scored = ScoreMeaningPdfPageRecallCorpus.score(liveEngine)
        val report = MeasureOnDeviceMeaningPdfPageRecallBaseline.measure(
            scoreResult = scored,
            deviceTierId = MeaningPdfPageRecallCorpus.DEVICE_TIER_EMULATOR_MEDIUM_PHONE,
        )

        assertEquals(MeaningPdfPageRecallCorpus.CORPUS_ID, report.baseline.fixtureCorpusId)
        assertEquals(3, report.baseline.caseCount)
        assertTrue(report.baseline.claims.all { it.hasMeasuredEvidence })
        assertEquals(
            MeaningPdfPageRecallCorpus.DEVICE_TIER_EMULATOR_MEDIUM_PHONE,
            report.baseline.deviceTierId,
        )
        assertEquals(MediaPipeAverageWordEmbedderSpec.MODEL_IDENTITY, report.model)
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
     * Prefer a host-pushed tempfile, then the product install (if the user already
     * downloaded the meaning model), then HTTPS download into disposable staging.
     */
    private fun stageModelForMeasurement() {
        val target = modelStore.modelFile()
        val pushed = File("/data/local/tmp/${MediaPipeAverageWordEmbedderSpec.FILE_NAME}")
        val product = File(
            context.noBackupFilesDir,
            "${NoBackupOnDeviceEmbeddingModelStore.ROOT_DIR_NAME}/" +
                MediaPipeAverageWordEmbedderSpec.FILE_NAME,
        )
        when {
            pushed.exists() && pushed.length() > 0L -> {
                pushed.copyTo(target, overwrite = true)
            }
            product.exists() && product.length() > 0L &&
                product.canonicalPath != target.canonicalPath -> {
                product.copyTo(target, overwrite = true)
            }
            else -> downloadModelIntoStaging()
        }
    }

    private fun downloadModelIntoStaging() {
        val temp = File(stagingRoot, "${MediaPipeAverageWordEmbedderSpec.FILE_NAME}.tmp")
        val target = modelStore.modelFile()
        temp.delete()
        val connection = (URL(MediaPipeAverageWordEmbedderSpec.DOWNLOAD_URL)
            .openConnection() as HttpURLConnection).apply {
            connectTimeout = 30_000
            readTimeout = 120_000
            instanceFollowRedirects = true
            requestMethod = "GET"
        }
        try {
            val code = connection.responseCode
            assertTrue("Model download HTTP $code", code in 200..299)
            BufferedInputStream(connection.inputStream).use { input ->
                temp.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var total = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        total += read
                        assertTrue(
                            "Model exceeded disclosed size limit",
                            total <= MediaPipeAverageWordEmbedderSpec.MAX_DOWNLOAD_BYTES,
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
        private const val STAGING_DIR_NAME = "memora_m2_page_recall_baseline"
        private const val LOG_TAG = "MemoraMeaningPdfM2"
    }
}

private class StagingOnDeviceEmbeddingModelStore(
    private val rootDir: File,
) : OnDeviceEmbeddingModelStore {
    fun modelFile(): File = File(rootDir, MediaPipeAverageWordEmbedderSpec.FILE_NAME)

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
        MediaPipeAverageWordEmbedderSpec.MODEL_IDENTITY

    override fun clear() {
        modelFile().delete()
    }
}
