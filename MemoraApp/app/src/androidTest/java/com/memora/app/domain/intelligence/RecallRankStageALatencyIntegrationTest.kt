package com.memora.app.domain.intelligence

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.memora.app.data.intelligence.AndroidRecallRankDeviceSignals
import java.io.BufferedInputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * FC-02 Stage A S3 — midrange aggregate rerank wallMs (real tokenizer + ONNX).
 *
 * Bar: pool-40 ≤ 800 ms **or** record DEGRADED_EXPLICIT / identity fallback via
 * [RecallRankLatencyPolicy]. Does not wire Canonical Recall. Not AVAILABLE.
 */
@RunWith(AndroidJUnit4::class)
class RecallRankStageALatencyIntegrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun measures_midrange_rerank_wall_ms_and_records_disposition() {
        val snapshot = AndroidRecallRankDeviceSignals.snapshot(context)
        val executionTier = RecallRankDevicePolicy.executionTier(snapshot)
        assumeTrue(
            "Skip S3 on identity-only hosts",
            executionTier != RecallRankExecutionTier.IDENTITY_ONLY,
        )

        val modelFile = stageCrossEncoderModel()
        assumeTrue("Stage ONNX model for S3", modelFile != null)

        val tokenizer = loadTokenizerFromAssets()
        val passages = latencyPassages()
        val query = "invoice boarding mira"
        val (env, session) = OnnxCrossEncoderSpikeSupport.openSession(checkNotNull(modelFile))
        try {
            data class Config(val pool: Int, val maxLen: Int, val label: String)

            val configs = listOf(
                Config(40, 128, "full40_len128"),
                Config(20, 128, "reduced20_len128"),
                Config(20, 96, "reduced20_len96"),
            )
            val walls = LinkedHashMap<String, Long>()
            configs.forEach { config ->
                val scorer = OnnxCrossEncoderPairScorer(env, session, tokenizer, config.maxLen)
                val slice = passages.take(config.pool)
                val started = System.nanoTime()
                slice.forEach { passage -> scorer.score(query, passage) }
                val wallMs = (System.nanoTime() - started) / 1_000_000L
                walls[config.label] = wallMs
                Log.i(
                    LOG_TAG,
                    "latency label=${config.label} pool=${config.pool} maxLen=${config.maxLen} " +
                        "wallMs=$wallMs targetMs=${RecallRankLatencyPolicy.TARGET_AGGREGATE_WALL_MS}",
                )
            }

            val fullWall = walls.getValue("full40_len128")
            val reducedWall = walls.getValue("reduced20_len96")
            val disposition = RecallRankLatencyPolicy.dispositionForMeasuredWallMs(
                fullPoolWallMs = fullWall,
                reducedPoolWallMs = reducedWall,
            )
            val effectivePool = RecallRankLatencyPolicy.effectivePoolSize(
                executionTier,
                disposition,
            )
            Log.i(
                LOG_TAG,
                "disposition=$disposition effectivePool=$effectivePool " +
                    "fullWallMs=$fullWall reduced20len96WallMs=$reducedWall " +
                    "tier=$executionTier meets800full=${fullWall <= RecallRankLatencyPolicy.TARGET_AGGREGATE_WALL_MS}",
            )

            assertNotNull(disposition)
            assertTrue(fullWall > 0L)
            assertTrue(reducedWall > 0L)
            // S3 does not fail solely for missing the 800 ms bar — disposition must be recorded.
            assertTrue(
                disposition == RecallRankLatencyPolicy.MidrangeLatencyDisposition.WITHIN_BUDGET ||
                    disposition == RecallRankLatencyPolicy.MidrangeLatencyDisposition.DEGRADED_EXPLICIT ||
                    disposition == RecallRankLatencyPolicy.MidrangeLatencyDisposition.IDENTITY_FALLBACK,
            )
        } finally {
            session.close()
        }
    }

    private fun latencyPassages(): List<String> {
        val seed = MeaningPdfPageRecallCorpus.stageALabeledCases()
            .flatMap { labeled -> labeled.candidates.map { it.evidenceText } }
            .distinct()
        require(seed.isNotEmpty())
        return List(40) { index -> seed[index % seed.size] + " context-$index" }
    }

    private fun loadTokenizerFromAssets(): BertWordPieceTokenizer {
        val instrumentationContext = InstrumentationRegistry.getInstrumentation().context
        instrumentationContext.assets.open(VOCAB_ASSET).bufferedReader().use { reader ->
            return BertWordPieceTokenizer.loadFromReader(reader)
        }
    }

    private fun stageCrossEncoderModel(): File? {
        val stagingRoot = File(context.noBackupFilesDir, STAGING_DIR)
        stagingRoot.mkdirs()
        val target = File(stagingRoot, OnnxMsMarcoMiniLmCrossEncoderSpec.FILE_NAME)
        val pushed = File("/data/local/tmp/${OnnxMsMarcoMiniLmCrossEncoderSpec.FILE_NAME}")
        when {
            pushed.exists() && pushed.length() > 0L -> pushed.copyTo(target, overwrite = true)
            target.exists() && target.length() > 0L -> Unit
            else -> downloadCrossEncoderInto(target)
        }
        return target.takeIf { it.exists() && it.length() > 0L }
    }

    private fun downloadCrossEncoderInto(target: File): Boolean {
        val temp = File(target.parentFile, "${target.name}.tmp")
        temp.delete()
        val connection = (URL(OnnxMsMarcoMiniLmCrossEncoderSpec.DOWNLOAD_URL)
            .openConnection() as HttpURLConnection).apply {
            connectTimeout = 60_000
            readTimeout = 180_000
            instanceFollowRedirects = true
            requestMethod = "GET"
            setRequestProperty("User-Agent", "UNFYND-S3-latency/1.0")
            setRequestProperty("Accept", "*/*")
        }
        return try {
            connection.connect()
            if (connection.responseCode !in 200..299) return false
            BufferedInputStream(connection.inputStream).use { input ->
                temp.outputStream().use { output -> input.copyTo(output) }
            }
            if (temp.length() <= 0L || temp.length() > RecallRankAiPackTrack.MAX_DOWNLOAD_BYTES) {
                temp.delete()
                false
            } else {
                temp.copyTo(target, overwrite = true)
                temp.delete()
                true
            }
        } catch (_: Exception) {
            temp.delete()
            false
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val LOG_TAG = "MemoraRecallRankS3"
        const val STAGING_DIR = "recall_rank_spike_staging"
        const val VOCAB_ASSET = "recall_rank/bert_vocab.txt"
    }
}
