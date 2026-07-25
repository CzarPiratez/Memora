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
import com.memora.app.application.discovery.MediaStoreImageIndexer
import com.memora.app.application.discovery.MediaStoreIndexingOutcome
import com.memora.app.domain.discovery.ImageLibraryAccessScope
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * WorkManager testing APIs for MediaStore discovery drain.
 *
 * Discovery metadata only: the scripted indexer never opens image bytes.
 */
@RunWith(AndroidJUnit4::class)
class MediaStoreDiscoveryWorkerAndroidTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
    }

    @Test
    fun drainsPagesUntilCompleteUsingUniqueWorkAndTestDriver() {
        val indexer = ScriptedIndexer(
            listOf(
                MediaStoreIndexingOutcome.Indexed(
                    discoveredAssetCount = 1,
                    hasMore = true,
                    accessScope = ImageLibraryAccessScope.FULL_LIBRARY,
                ),
                MediaStoreIndexingOutcome.Indexed(
                    discoveredAssetCount = 1,
                    hasMore = true,
                    accessScope = ImageLibraryAccessScope.FULL_LIBRARY,
                ),
                MediaStoreIndexingOutcome.Indexed(
                    discoveredAssetCount = 1,
                    hasMore = false,
                    accessScope = ImageLibraryAccessScope.FULL_LIBRARY,
                ),
            ),
        )
        val workManager = initializeTestWorkManager(indexer)
        val scheduler = DefaultMediaStoreDiscoveryWorkScheduler(workManager)

        scheduler.enqueueDrain()
        drainUntilFinished(workManager)

        val infos = workManager
            .getWorkInfosForUniqueWork(DefaultMediaStoreDiscoveryWorkScheduler.UNIQUE_WORK_NAME)
            .get()
        assertTrue(infos.isNotEmpty())
        assertTrue(infos.all { it.state.isFinished })
        assertFalse(infos.any { it.state == WorkInfo.State.FAILED })
        assertEquals(3, indexer.invocationCount)
        val successes = infos.filter { it.state == WorkInfo.State.SUCCEEDED }
        assertEquals(3, successes.size)
        assertTrue(
            successes.any { info ->
                !info.outputData.getBoolean(MediaStoreDiscoveryWorker.KEY_HAS_MORE, true)
            },
        )
    }

    @Test
    fun accessStoppedFailsUniqueWorkWithoutContinuation() {
        val indexer = ScriptedIndexer(
            listOf(MediaStoreIndexingOutcome.AccessRevoked),
        )
        val workManager = initializeTestWorkManager(indexer)
        val scheduler = DefaultMediaStoreDiscoveryWorkScheduler(workManager)

        scheduler.enqueueDrain()
        drainUntilFinished(workManager)

        val infos = workManager
            .getWorkInfosForUniqueWork(DefaultMediaStoreDiscoveryWorkScheduler.UNIQUE_WORK_NAME)
            .get()
        assertEquals(1, indexer.invocationCount)
        assertTrue(infos.any { it.state == WorkInfo.State.FAILED })
        assertEquals(
            MediaStoreDiscoveryWorker.REASON_ACCESS_STOPPED,
            infos.first { it.state == WorkInfo.State.FAILED }
                .outputData
                .getString(MediaStoreDiscoveryWorker.KEY_FAILURE_REASON),
        )
    }

    private fun initializeTestWorkManager(indexer: MediaStoreImageIndexer): WorkManager {
        val factory = object : WorkerFactory() {
            override fun createWorker(
                appContext: Context,
                workerClassName: String,
                workerParameters: WorkerParameters,
            ): ListenableWorker? {
                if (workerClassName != MediaStoreDiscoveryWorker::class.java.name) return null
                val scheduler = DefaultMediaStoreDiscoveryWorkScheduler(
                    WorkManager.getInstance(appContext),
                )
                return MediaStoreDiscoveryWorker(
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

    private fun drainUntilFinished(workManager: WorkManager) {
        val testDriver = requireNotNull(WorkManagerTestInitHelper.getTestDriver(context))
        val uniqueName = DefaultMediaStoreDiscoveryWorkScheduler.UNIQUE_WORK_NAME
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
        error("MediaStore discovery drain did not finish for $uniqueName")
    }

    private class ScriptedIndexer(
        initial: List<MediaStoreIndexingOutcome>,
    ) : MediaStoreImageIndexer {
        private val outcomes = ArrayDeque(initial)
        var invocationCount: Int = 0
            private set

        override suspend fun invoke(): MediaStoreIndexingOutcome {
            invocationCount += 1
            return outcomes.removeFirstOrNull()
                ?: MediaStoreIndexingOutcome.Indexed(
                    discoveredAssetCount = 0,
                    hasMore = false,
                    accessScope = ImageLibraryAccessScope.FULL_LIBRARY,
                )
        }
    }
}
