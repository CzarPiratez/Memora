package com.memora.app.domain.intelligence

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
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
 * FC-02 Stage A S1 spike: device tier policy (always) + ONNX load/latency when model staged.
 *
 * Does not wire Canonical Recall. Does not mark AVAILABLE.
 *
 * Model staging (one of):
 * - `/data/local/tmp/{OnnxMsMarcoMiniLmCrossEncoderSpec.FILE_NAME}`
 * - HTTPS download into disposable staging (instrumentation network)
 */
@RunWith(AndroidJUnit4::class)
class RecallRankStageASpikeIntegrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun reports_recall_rank_device_tier_on_host() {
        val snapshot = AndroidRecallRankDeviceSignals.snapshot(context)
        val executionTier = RecallRankDevicePolicy.executionTier(snapshot)
        val deviceClass = RecallRankCapabilitySupportPolicy.mapDeviceClass(snapshot)
        val support = RecallRankCapabilitySupportPolicy.decision(deviceClass, executionTier)
        val pool = RecallRankDevicePolicy.maxPoolSize(executionTier)

        Log.i(
            LOG_TAG,
            "abi=${snapshot.primaryAbi} ramMb=${snapshot.totalRamMegabytes} " +
                "emulator=${snapshot.isEmulator} executionTier=$executionTier pool=$pool " +
                "supportTier=${support.tier} smartAutoOnboarding=ADR-052",
        )

        assertNotNull(support.reason)
        assertTrue(pool in 0..RecallRankDevicePolicy.ABSOLUTE_MAX_POOL_SIZE)
    }

    @Test
    fun spikes_quantized_cross_encoder_when_model_is_staged() {
        val snapshot = AndroidRecallRankDeviceSignals.snapshot(context)
        val executionTier = RecallRankDevicePolicy.executionTier(snapshot)
        assumeTrue(
            "Skip ONNX spike on identity-only hosts",
            executionTier != RecallRankExecutionTier.IDENTITY_ONLY,
        )

        val modelFile = stageCrossEncoderModelForSpike()
        if (modelFile == null) {
            Log.w(
                LOG_TAG,
                "onnxSpike skipped: model not staged (HTTPS failed or empty file). " +
                    "url=${OnnxMsMarcoMiniLmCrossEncoderSpec.DOWNLOAD_URL}",
            )
        }
        assumeTrue(
            "Stage ${OnnxMsMarcoMiniLmCrossEncoderSpec.FILE_NAME} under /data/local/tmp or allow HTTPS download",
            modelFile != null,
        )

        val pool = RecallRankDevicePolicy.maxPoolSize(executionTier).coerceAtLeast(1)
        val seqLen = RecallRankDevicePolicy.maxSequenceLength(executionTier).coerceAtLeast(32)
        val result = OnnxCrossEncoderSpikeSupport.runSpike(
            modelFile = checkNotNull(modelFile),
            pairCount = pool,
            sequenceLength = seqLen,
        )

        Log.i(
            LOG_TAG,
            "onnxSpike pairs=${result.pairCount} totalMs=${result.totalWallMs} " +
                "avgPairMs=${result.averagePairMs} inputs=${result.inputNames} " +
                "lastScore=${result.lastScore}",
        )

        assertTrue(result.modelLoaded)
        assertTrue(result.totalWallMs >= 0L)
        assertTrue(result.averagePairMs >= 0.0)
    }

    private fun stageCrossEncoderModelForSpike(): File? {
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
            setRequestProperty("User-Agent", "UNFYND-S1-spike/1.0")
            setRequestProperty("Accept", "*/*")
        }
        return try {
            connection.connect()
            val code = connection.responseCode
            if (code !in 200..299) {
                Log.w(LOG_TAG, "onnxSpike download HTTP $code")
                return false
            }
            val length = connection.contentLengthLong
            if (length > RecallRankAiPackTrack.MAX_DOWNLOAD_BYTES) {
                Log.w(LOG_TAG, "onnxSpike download too large declaredBytes=$length")
                return false
            }
            BufferedInputStream(connection.inputStream).use { input ->
                temp.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            if (temp.length() <= 0L || temp.length() > RecallRankAiPackTrack.MAX_DOWNLOAD_BYTES) {
                Log.w(LOG_TAG, "onnxSpike download empty or over ceiling bytes=${temp.length()}")
                temp.delete()
                false
            } else {
                Log.i(LOG_TAG, "onnxSpike download ok bytes=${temp.length()}")
                temp.copyTo(target, overwrite = true)
                temp.delete()
                true
            }
        } catch (error: Exception) {
            Log.w(LOG_TAG, "onnxSpike download failed: ${error.javaClass.simpleName} ${error.message}")
            temp.delete()
            false
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val LOG_TAG = "MemoraRecallRankS1"
        const val STAGING_DIR = "recall_rank_spike_staging"
    }
}
