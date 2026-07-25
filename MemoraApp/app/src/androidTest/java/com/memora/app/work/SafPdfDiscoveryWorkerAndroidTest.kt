package com.memora.app.work

import android.content.Context
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import androidx.work.workDataOf
import com.memora.app.application.documents.SafPdfFolderIndexer
import com.memora.app.application.documents.SafPdfFolderIndexingOutcome
import com.memora.app.domain.asset.SourceId
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * WorkManager testing APIs for SAF PDF discovery drain.
 *
 * Discovery metadata only: the scripted indexer never opens PDF bytes.
 */
@RunWith(AndroidJUnit4::class)
class SafPdfDiscoveryWorkerAndroidTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
    }

    @Test
    fun drainsPagesUntilCompleteUsingUniqueWorkAndTestDriver() {
        val sourceId = SourceId("android-saf-document-tree:wm-drain")
        val indexer = ScriptedIndexer(
            listOf(
                SafPdfFolderIndexingOutcome.Indexed(
                    sourceId = sourceId,
                    discoveredAssetCount = 1,
                    hasMore = true,
                ),
                SafPdfFolderIndexingOutcome.Indexed(
                    sourceId = sourceId,
                    discoveredAssetCount = 1,
                    hasMore = true,
                ),
                SafPdfFolderIndexingOutcome.Indexed(
                    sourceId = sourceId,
                    discoveredAssetCount = 1,
                    hasMore = false,
                ),
            ),
        )
        val workManager = initializeTestWorkManager(indexer)
        val scheduler = DefaultSafPdfDiscoveryWorkScheduler(workManager)

        scheduler.enqueueDrain(sourceId)
        drainUntilFinished(workManager, sourceId)

        val infos = workManager
            .getWorkInfosForUniqueWork(DefaultSafPdfDiscoveryWorkScheduler.uniqueWorkName(sourceId))
            .get()
        assertTrue(infos.isNotEmpty())
        assertTrue(infos.all { it.state.isFinished })
        assertFalse(infos.any { it.state == WorkInfo.State.FAILED })
        assertEquals(3, indexer.invocationCount)
        val successes = infos.filter { it.state == WorkInfo.State.SUCCEEDED }
        assertEquals(3, successes.size)
        assertTrue(
            successes.any { info ->
                !info.outputData.getBoolean(SafPdfDiscoveryWorker.KEY_HAS_MORE, true)
            },
        )
        assertEquals(
            2,
            successes.count { info ->
                info.outputData.getBoolean(SafPdfDiscoveryWorker.KEY_HAS_MORE, false)
            },
        )
    }

    @Test
    fun resumesDrainAfterPartialProgressWithoutExtraIndexerCallsBeyondRemainingPages() {
        val sourceId = SourceId("android-saf-document-tree:wm-resume")
        val indexer = ScriptedIndexer(
            listOf(
                SafPdfFolderIndexingOutcome.Indexed(
                    sourceId = sourceId,
                    discoveredAssetCount = 1,
                    hasMore = true,
                ),
                SafPdfFolderIndexingOutcome.Indexed(
                    sourceId = sourceId,
                    discoveredAssetCount = 1,
                    hasMore = false,
                ),
            ),
        )
        val workManager = initializeTestWorkManager(indexer)
        val scheduler = DefaultSafPdfDiscoveryWorkScheduler(workManager)

        scheduler.enqueueDrain(sourceId)
        drainUntilFinished(workManager, sourceId)
        assertEquals(2, indexer.invocationCount)

        // Explicit Index again after completion (REPLACE) must not invent extra pages.
        indexer.append(
            SafPdfFolderIndexingOutcome.Indexed(
                sourceId = sourceId,
                discoveredAssetCount = 0,
                hasMore = false,
            ),
        )
        scheduler.enqueueDrain(sourceId)
        drainUntilFinished(workManager, sourceId)
        assertEquals(3, indexer.invocationCount)
    }

    @Test
    fun accessStoppedFailsUniqueWorkWithoutContinuation() {
        val sourceId = SourceId("android-saf-document-tree:wm-revoke")
        val indexer = ScriptedIndexer(
            listOf(SafPdfFolderIndexingOutcome.AccessRevoked),
        )
        val workManager = initializeTestWorkManager(indexer)
        val scheduler = DefaultSafPdfDiscoveryWorkScheduler(workManager)

        scheduler.enqueueDrain(sourceId)
        drainUntilFinished(workManager, sourceId)

        val infos = workManager
            .getWorkInfosForUniqueWork(DefaultSafPdfDiscoveryWorkScheduler.uniqueWorkName(sourceId))
            .get()
        assertEquals(1, indexer.invocationCount)
        assertTrue(infos.any { it.state == WorkInfo.State.FAILED })
        assertEquals(
            SafPdfDiscoveryWorker.REASON_ACCESS_STOPPED,
            infos.first { it.state == WorkInfo.State.FAILED }
                .outputData
                .getString(SafPdfDiscoveryWorker.KEY_FAILURE_REASON),
        )
    }

    private fun initializeTestWorkManager(indexer: SafPdfFolderIndexer): WorkManager {
        val factory = object : WorkerFactory() {
            override fun createWorker(
                appContext: Context,
                workerClassName: String,
                workerParameters: WorkerParameters,
            ): ListenableWorker? {
                if (workerClassName != SafPdfDiscoveryWorker::class.java.name) return null
                val scheduler = DefaultSafPdfDiscoveryWorkScheduler(WorkManager.getInstance(appContext))
                return SafPdfDiscoveryWorker(
                    appContext,
                    workerParameters,
                    indexer,
                    scheduler,
                )
            }
        }
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .setExecutor(SynchronousExecutor())
            .setWorkerFactory(factory)
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
        return WorkManager.getInstance(context)
    }

    private fun drainUntilFinished(workManager: WorkManager, sourceId: SourceId) {
        val testDriver = requireNotNull(WorkManagerTestInitHelper.getTestDriver(context)) {
            "WorkManager TestDriver must be available after initializeTestWorkManager."
        }
        val uniqueName = DefaultSafPdfDiscoveryWorkScheduler.uniqueWorkName(sourceId)
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        while (System.nanoTime() < deadline) {
            val infos = workManager.getWorkInfosForUniqueWork(uniqueName).get()
            infos.filter { info ->
                info.state == WorkInfo.State.ENQUEUED ||
                    info.state == WorkInfo.State.BLOCKED ||
                    info.state == WorkInfo.State.RUNNING
            }.forEach { info ->
                testDriver.setAllConstraintsMet(info.id)
            }
            if (infos.isNotEmpty() && infos.all { it.state.isFinished }) {
                return
            }
            Thread.sleep(20)
        }
        error("Discovery drain did not finish for $uniqueName")
    }

    private class ScriptedIndexer(
        initial: List<SafPdfFolderIndexingOutcome>,
    ) : SafPdfFolderIndexer {
        private val outcomes = ArrayDeque(initial)
        var invocationCount: Int = 0
            private set

        fun append(outcome: SafPdfFolderIndexingOutcome) {
            outcomes.addLast(outcome)
        }

        override suspend fun invoke(sourceId: SourceId): SafPdfFolderIndexingOutcome {
            invocationCount += 1
            return outcomes.removeFirstOrNull()
                ?: SafPdfFolderIndexingOutcome.Indexed(
                    sourceId = sourceId,
                    discoveredAssetCount = 0,
                    hasMore = false,
                )
        }
    }
}
