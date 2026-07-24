package com.memora.app.data.security.poc

import android.content.Context
import android.os.BatteryManager
import android.os.SystemClock
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.memora.app.data.local.AssetEntity
import com.memora.app.data.local.DiscoveryCheckpointEntity
import com.memora.app.data.local.DocumentTreeApprovalEntity
import com.memora.app.data.security.ConversionElapsedBuckets
import com.memora.app.data.security.DatabaseEncryptionConversionJournal
import com.memora.app.data.security.KeystoreDatabasePassphraseStore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Content-free conversion timing/battery buckets for schema-v3 sizes.
 * Disposable benchmark identities only; never leaves production memora.db residue.
 */
@RunWith(AndroidJUnit4::class)
class ConversionPerformanceBenchmarkIntegrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var passphraseStore: KeystoreDatabasePassphraseStore
    private lateinit var journal: DatabaseEncryptionConversionJournal
    private lateinit var harness: PlaintextToEncryptedConversionHarness

    @Before
    fun setUp() {
        passphraseStore = KeystoreDatabasePassphraseStore(
            context = context,
            keyAlias = BENCH_KEY_ALIAS,
            wrapperFileName = BENCH_WRAPPER_FILE,
        )
        journal = DatabaseEncryptionConversionJournal(
            context = context,
            journalFileName = BENCH_JOURNAL_FILE,
        )
        harness = PlaintextToEncryptedConversionHarness(
            context = context,
            passphraseStore = passphraseStore,
            journal = journal,
            plaintextDatabaseName = BENCH_PLAINTEXT_DB,
            encryptedDatabaseName = BENCH_ENCRYPTED_DB,
        )
        clearBenchmarkState()
    }

    @After
    fun tearDown() {
        clearBenchmarkState()
    }

    @Test
    fun empty_bucket_converts_within_budget() = runBlocking {
        measureBucket(
            bucketId = "empty",
            assetCount = 0,
            checkpointCount = 0,
            approvalCount = 0,
            ceilingMillis = EMPTY_CEILING_MS,
        )
    }

    @Test
    fun small_100_bucket_converts_within_budget() = runBlocking {
        measureBucket(
            bucketId = "small_100",
            assetCount = 100,
            checkpointCount = 5,
            approvalCount = 5,
            ceilingMillis = SMALL_CEILING_MS,
        )
    }

    @Test
    fun medium_1000_bucket_converts_within_budget() = runBlocking {
        measureBucket(
            bucketId = "medium_1000",
            assetCount = 1_000,
            checkpointCount = 20,
            approvalCount = 20,
            ceilingMillis = MEDIUM_CEILING_MS,
        )
    }

    @Test
    fun large_5000_bucket_converts_within_budget() = runBlocking {
        measureBucket(
            bucketId = "large_5000",
            assetCount = 5_000,
            checkpointCount = 50,
            approvalCount = 50,
            ceilingMillis = LARGE_CEILING_MS,
        )
    }

    private suspend fun measureBucket(
        bucketId: String,
        assetCount: Int,
        checkpointCount: Int,
        approvalCount: Int,
        ceilingMillis: Long,
    ) {
        seedPlaintext(assetCount, checkpointCount, approvalCount)
        val plaintextBytes = context.getDatabasePath(BENCH_PLAINTEXT_DB).length()
        val chargeBefore = readChargeCounterUah()

        val startedAt = SystemClock.elapsedRealtime()
        val pending = harness.convertCopyAndValidate()
        assertTrue(
            "Bucket $bucketId must reach switch-pending.",
            pending is ConversionHarnessResult.ValidatedSwitchPending,
        )
        val completed = harness.finalizeAfterValidatedSwitch()
        assertTrue(
            "Bucket $bucketId must complete finalize.",
            completed is ConversionHarnessResult.Completed,
        )
        val elapsedMillis = SystemClock.elapsedRealtime() - startedAt
        val chargeAfter = readChargeCounterUah()
        val chargeDelta = when {
            chargeBefore == null || chargeAfter == null -> null
            else -> chargeBefore - chargeAfter
        }

        Log.i(
            LOG_TAG,
            "bucket=$bucketId; assets=$assetCount; checkpoints=$checkpointCount; " +
                "approvals=$approvalCount; plaintext_bytes=$plaintextBytes; " +
                "elapsed_ms=$elapsedMillis; elapsed_bucket=${ConversionElapsedBuckets.forMillis(elapsedMillis)}; " +
                "charge_counter_delta_uah=${chargeDelta?.toString() ?: "unavailable"}",
        )

        assertTrue(
            "Bucket $bucketId elapsed ${elapsedMillis}ms exceeds ceiling ${ceilingMillis}ms.",
            elapsedMillis <= ceilingMillis,
        )
    }

    private suspend fun seedPlaintext(
        assetCount: Int,
        checkpointCount: Int,
        approvalCount: Int,
    ) {
        val plaintext = harness.openPlaintext()
        try {
            repeat(assetCount) { index ->
                plaintext.assetDao().upsert(
                    AssetEntity(
                        sourceId = "bench-source",
                        sourceAssetKey = "asset-$index",
                        assetType = "PHOTO",
                        location = "content://com.memora.bench/item/$index",
                        fingerprint = "BENCH_FP_$index",
                        discoveredAtEpochMillis = 1_700_000_000_000L + index,
                        displayName = "bench-$index.jpg",
                        sourceModifiedAtEpochMillis = 1_700_000_000_000L + index,
                        indexingStatus = "DISCOVERED",
                        indexingAttemptCount = 0,
                        failureCode = null,
                        failureMessage = null,
                    ),
                )
            }
            repeat(checkpointCount) { index ->
                plaintext.discoveryCheckpointDao().upsert(
                    DiscoveryCheckpointEntity(
                        sourceId = "bench-checkpoint-$index",
                        cursorValue = "cursor-$index",
                        savedAtEpochMillis = 1_700_000_000_000L + index,
                    ),
                )
            }
            repeat(approvalCount) { index ->
                plaintext.documentTreeApprovalDao().upsert(
                    DocumentTreeApprovalEntity(
                        sourceId = "bench-tree-$index",
                        treeUri = "content://com.memora.bench/tree/$index",
                        approvedAtEpochMillis = 1_700_000_000_000L + index,
                    ),
                )
            }
        } finally {
            plaintext.close()
        }
    }

    private fun clearBenchmarkState() {
        harness.deleteAllHarnessFiles()
        passphraseStore.clearForTest()
        journal.clearForTest()
        File(context.noBackupFilesDir, BENCH_WRAPPER_FILE).delete()
        File(context.noBackupFilesDir, BENCH_JOURNAL_FILE).delete()
    }

    private fun readChargeCounterUah(): Long? {
        val batteryManager = context.getSystemService(BatteryManager::class.java) ?: return null
        val value = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
        return value.takeIf { it >= 0L }
    }

    companion object {
        private const val LOG_TAG = "MemoraConversionBenchmark"
        private const val BENCH_PLAINTEXT_DB = "memora_conversion_bench.db"
        private const val BENCH_ENCRYPTED_DB = "memora_conversion_bench.encrypted_candidate"
        private const val BENCH_KEY_ALIAS = "memora.conversion.bench.wrap.v1"
        private const val BENCH_WRAPPER_FILE = "memora_conversion_bench_wrap_v1.bin"
        private const val BENCH_JOURNAL_FILE = "memora_conversion_bench_v1.journal"

        const val EMPTY_CEILING_MS = 5_000L
        const val SMALL_CEILING_MS = 15_000L
        const val MEDIUM_CEILING_MS = 60_000L
        const val LARGE_CEILING_MS = 180_000L

        @JvmStatic
        @BeforeClass
        fun warmUpSqlCipherOnce() {
            val context = ApplicationProvider.getApplicationContext<Context>()
            System.loadLibrary("sqlcipher")
            val passphraseStore = KeystoreDatabasePassphraseStore(
                context = context,
                keyAlias = BENCH_KEY_ALIAS,
                wrapperFileName = BENCH_WRAPPER_FILE,
            )
            val journal = DatabaseEncryptionConversionJournal(
                context = context,
                journalFileName = BENCH_JOURNAL_FILE,
            )
            val harness = PlaintextToEncryptedConversionHarness(
                context = context,
                passphraseStore = passphraseStore,
                journal = journal,
                plaintextDatabaseName = BENCH_PLAINTEXT_DB,
                encryptedDatabaseName = BENCH_ENCRYPTED_DB,
            )
            runBlocking {
                harness.deleteAllHarnessFiles()
                passphraseStore.clearForTest()
                journal.clearForTest()
                harness.openPlaintext().close()
                val pending = harness.convertCopyAndValidate()
                if (pending is ConversionHarnessResult.ValidatedSwitchPending) {
                    harness.finalizeAfterValidatedSwitch()
                }
                harness.deleteAllHarnessFiles()
                passphraseStore.clearForTest()
                journal.clearForTest()
            }
        }
    }
}
