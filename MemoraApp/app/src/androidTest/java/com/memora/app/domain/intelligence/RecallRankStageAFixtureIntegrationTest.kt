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
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * FC-02 Stage A S2 — fixture hit@1 on meaning-pdf-page-recall-v1 with real tokenizer + ONNX.
 *
 * Does not wire Canonical Recall. Does not mark AVAILABLE.
 */
@RunWith(AndroidJUnit4::class)
class RecallRankStageAFixtureIntegrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun cross_encoder_hit_at_1_meets_e5d_boosted_baseline_on_fixture() {
        val snapshot = AndroidRecallRankDeviceSignals.snapshot(context)
        val executionTier = RecallRankDevicePolicy.executionTier(snapshot)
        assumeTrue(
            "Skip S2 ONNX fixture on identity-only hosts",
            executionTier != RecallRankExecutionTier.IDENTITY_ONLY,
        )

        val modelFile = stageCrossEncoderModel()
        assumeTrue(
            "Stage ${OnnxMsMarcoMiniLmCrossEncoderSpec.FILE_NAME} for S2 fixture",
            modelFile != null,
        )

        val tokenizer = loadTokenizerFromAssets()
        val maxLen = RecallRankDevicePolicy.maxSequenceLength(executionTier)
            .coerceAtLeast(32)
            .coerceAtMost(BertWordPieceTokenizer.DEFAULT_MAX_LENGTH)
        val (env, session) = OnnxCrossEncoderSpikeSupport.openSession(checkNotNull(modelFile))
        try {
            val scorer = OnnxCrossEncoderPairScorer(env, session, tokenizer, maxLen)
            val report = ScoreMeaningPdfPageRecallWithCrossEncoder.score(
                scorer = scorer,
                labeledCases = MeaningPdfPageRecallCorpus.stageALabeledCases(),
                deviceTierId = MeaningPdfPageRecallCorpus.DEVICE_TIER_MIDRANGE_ARM64,
            )
            Log.i(
                LOG_TAG,
                "fixture hits@1=${report.crossEncoderHitsAt1}/${report.caseCount} " +
                    "pairs=${report.pairScoreCount} wallMs=${report.scoreWallMs} " +
                    "meetsS2=${report.meetsS2Bar} maxLen=$maxLen",
            )
            report.caseBreakdown.forEach { row ->
                Log.i(
                    LOG_TAG,
                    "case ${row.caseId} hit=${row.hitAt1} " +
                        "top=${row.topAssetFileName}#${row.topPageNumber} score=${row.topScore} " +
                        "expected=${row.expectedAssetFileName}#${row.expectedPageNumber}",
                )
            }
            assertTrue(
                "S2 bar: CE hit@1 must be ${report.e5dBoostedBaselineHitsAt1}/${report.caseCount}. " +
                    report.notes,
                report.meetsS2Bar,
            )
        } finally {
            session.close()
        }
    }

    private fun loadTokenizerFromAssets(): BertWordPieceTokenizer {
        // androidTest assets live on the instrumentation context, not the app under test.
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
            setRequestProperty("User-Agent", "UNFYND-S2-fixture/1.0")
            setRequestProperty("Accept", "*/*")
        }
        return try {
            connection.connect()
            val code = connection.responseCode
            if (code !in 200..299) {
                Log.w(LOG_TAG, "S2 model download HTTP $code")
                return false
            }
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
        } catch (error: Exception) {
            Log.w(LOG_TAG, "S2 model download failed: ${error.message}")
            temp.delete()
            false
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val LOG_TAG = "MemoraRecallRankS2"
        const val STAGING_DIR = "recall_rank_spike_staging"
        const val VOCAB_ASSET = "recall_rank/bert_vocab.txt"
    }
}
