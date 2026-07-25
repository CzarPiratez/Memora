package com.memora.app.work

import android.content.Context
import android.os.CancellationSignal
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
import com.memora.app.application.documents.PendingPdfLocalReader
import com.memora.app.application.documents.PendingPdfLocalReadingOutcome
import com.memora.app.domain.asset.SourceId
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * WorkManager testing APIs for SAF PDF extract drain (scripted reading outcomes).
 */
@RunWith(AndroidJUnit4::class)
class SafPdfExtractWorkerAndroidTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
    }

    @Test
    fun drainsPendingPdfsUntilCompleteUsingUniqueWorkAndTestDriver() {
        val sourceId = SourceId("android-saf-document-tree:wm-extract")
        val reader = ScriptedPendingReader(
            listOf(
                PendingPdfLocalReadingOutcome.Persisted("a", hasMorePending = true),
                PendingPdfLocalReadingOutcome.Persisted("b", hasMorePending = false),
            ),
        )
        val workManager = initializeTestWorkManager(reader)
        val scheduler = DefaultSafPdfExtractWorkScheduler(workManager)

        scheduler.enqueueDrain(sourceId)
        drainUntilFinished(workManager, sourceId)

        val infos = workManager
            .getWorkInfosForUniqueWork(DefaultSafPdfExtractWorkScheduler.uniqueWorkName(sourceId))
            .get()
        assertTrue(infos.isNotEmpty())
        assertTrue(infos.all { it.state.isFinished })
        assertFalse(infos.any { it.state == WorkInfo.State.FAILED })
        assertEquals(2, reader.invocationCount)
        assertTrue(
            infos.any { info ->
                info.state == WorkInfo.State.SUCCEEDED &&
                    !info.outputData.getBoolean(SafPdfExtractWorker.KEY_HAS_MORE, true)
            },
        )
    }

    @Test
    fun accessStoppedFailsUniqueWorkWithoutContinuation() {
        val sourceId = SourceId("android-saf-document-tree:wm-extract-revoke")
        val reader = ScriptedPendingReader(
            listOf(PendingPdfLocalReadingOutcome.AccessStopped),
        )
        val workManager = initializeTestWorkManager(reader)
        val scheduler = DefaultSafPdfExtractWorkScheduler(workManager)

        scheduler.enqueueDrain(sourceId)
        drainUntilFinished(workManager, sourceId)

        val infos = workManager
            .getWorkInfosForUniqueWork(DefaultSafPdfExtractWorkScheduler.uniqueWorkName(sourceId))
            .get()
        assertEquals(1, reader.invocationCount)
        assertTrue(infos.any { it.state == WorkInfo.State.FAILED })
        assertEquals(
            SafPdfExtractWorker.REASON_ACCESS_STOPPED,
            infos.first { it.state == WorkInfo.State.FAILED }
                .outputData
                .getString(SafPdfExtractWorker.KEY_FAILURE_REASON),
        )
    }

    private fun initializeTestWorkManager(reader: PendingPdfLocalReader): WorkManager {
        val factory = object : WorkerFactory() {
            override fun createWorker(
                appContext: Context,
                workerClassName: String,
                workerParameters: WorkerParameters,
            ): ListenableWorker? {
                if (workerClassName != SafPdfExtractWorker::class.java.name) return null
                val scheduler = DefaultSafPdfExtractWorkScheduler(WorkManager.getInstance(appContext))
                return SafPdfExtractWorker(appContext, workerParameters, reader, scheduler)
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
        val testDriver = requireNotNull(WorkManagerTestInitHelper.getTestDriver(context))
        val uniqueName = DefaultSafPdfExtractWorkScheduler.uniqueWorkName(sourceId)
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
        error("Extract drain did not finish for $uniqueName")
    }

    private class ScriptedPendingReader(
        initial: List<PendingPdfLocalReadingOutcome>,
    ) : PendingPdfLocalReader {
        private val outcomes = ArrayDeque(initial)
        var invocationCount: Int = 0
            private set

        override suspend fun invoke(
            sourceId: SourceId,
            afterSourceAssetKey: String?,
            cancellationSignal: CancellationSignal,
        ): PendingPdfLocalReadingOutcome {
            invocationCount += 1
            return outcomes.removeFirstOrNull() ?: PendingPdfLocalReadingOutcome.NoPending
        }
    }
}
