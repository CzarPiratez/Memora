package com.memora.app.ui.setup

import androidx.work.Data
import androidx.work.WorkInfo
import com.memora.app.domain.memory.CorpusCompletenessCounts
import com.memora.app.domain.memory.CorpusCompletenessSnapshot
import com.memora.app.work.AssetMemoryAssemblyWorkScheduler
import com.memora.app.work.AssetMemoryAssemblyWorker
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AssetMemorySetupViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun build_enqueues_once_and_shows_building() = runTest {
        val scheduler = RecordingScheduler()
        val viewModel = viewModel(scheduler)
        advanceUntilIdle()

        viewModel.onBuildRequested()
        advanceUntilIdle()
        viewModel.onBuildRequested()
        advanceUntilIdle()

        assertEquals(1, scheduler.drainCount)
        assertTrue(viewModel.uiState.value is AssetMemorySetupState.Building)
    }

    @Test
    fun completed_chain_becomes_ready_with_totals() = runTest {
        val scheduler = RecordingScheduler()
        val viewModel = viewModel(scheduler, pending = 0, ready = 28)
        advanceUntilIdle()

        viewModel.onBuildRequested()
        advanceUntilIdle()
        scheduler.emit(
            listOf(
                workInfo(
                    WorkInfo.State.SUCCEEDED,
                    assembled = 25,
                    skipped = 0,
                    ready = 25,
                    hasMore = true,
                ),
                workInfo(
                    WorkInfo.State.SUCCEEDED,
                    assembled = 3,
                    skipped = 1,
                    ready = 28,
                    hasMore = false,
                ),
            ),
        )
        advanceUntilIdle()

        val ready = viewModel.uiState.value as AssetMemorySetupState.Ready
        assertEquals(28, ready.currentReadyCount)
        assertEquals(28, ready.assembledInLastRun)
        assertEquals(1, ready.skippedInLastRun)
        assertEquals(false, ready.hasMore)
    }

    @Test
    fun stop_cancels_and_cancelled_work_is_ready_not_failed() = runTest {
        val scheduler = RecordingScheduler()
        val viewModel = viewModel(scheduler, pending = 15, ready = 25)
        advanceUntilIdle()

        viewModel.onBuildRequested()
        advanceUntilIdle()
        scheduler.emit(listOf(workInfo(WorkInfo.State.RUNNING)))
        advanceUntilIdle()
        viewModel.onStop()
        assertEquals(1, scheduler.cancelCount)
        scheduler.emit(
            listOf(
                workInfo(
                    WorkInfo.State.SUCCEEDED,
                    assembled = 25,
                    skipped = 0,
                    ready = 25,
                    hasMore = true,
                ),
                workInfo(WorkInfo.State.CANCELLED),
            ),
        )
        advanceUntilIdle()

        val ready = viewModel.uiState.value as AssetMemorySetupState.Ready
        assertEquals(25, ready.assembledInLastRun)
        assertEquals(true, ready.hasMore)
        assertTrue(viewModel.uiState.value !is AssetMemorySetupState.Failed)
    }

    @Test
    fun reconnects_to_active_work_without_a_new_enqueue() = runTest {
        val scheduler = RecordingScheduler(
            initial = listOf(workInfo(WorkInfo.State.RUNNING)),
        )
        val viewModel = viewModel(scheduler, pending = 10, ready = 5)
        advanceUntilIdle()

        assertEquals(0, scheduler.drainCount)
        assertTrue(viewModel.uiState.value is AssetMemorySetupState.Building)
    }

    private fun viewModel(
        scheduler: RecordingScheduler,
        pending: Int = 40,
        ready: Int = 5,
    ) = AssetMemorySetupViewModel(
        scheduler = scheduler,
        loadSnapshot = {
            CorpusCompletenessSnapshot(
                counts = CorpusCompletenessCounts(
                    memoriesReady = ready,
                    memoriesPendingAssembly = pending,
                    meaningSummaryIndexed = 0,
                    meaningEvidenceIndexed = 0,
                    meaningIndexPending = 0,
                ),
                blocked = null,
            )
        },
    )

    private fun workInfo(
        state: WorkInfo.State,
        assembled: Int = 0,
        skipped: Int = 0,
        ready: Int = 0,
        hasMore: Boolean = false,
    ): WorkInfo {
        val output = if (state == WorkInfo.State.SUCCEEDED) {
            Data.Builder()
                .putInt(AssetMemoryAssemblyWorker.KEY_ASSEMBLED, assembled)
                .putInt(AssetMemoryAssemblyWorker.KEY_SKIPPED, skipped)
                .putInt(AssetMemoryAssemblyWorker.KEY_READY_COUNT, ready)
                .putBoolean(AssetMemoryAssemblyWorker.KEY_HAS_MORE, hasMore)
                .build()
        } else {
            Data.EMPTY
        }
        return WorkInfo(
            UUID.randomUUID(),
            state,
            emptySet(),
            output,
            Data.EMPTY,
            1,
            1,
        )
    }

    private class RecordingScheduler(
        initial: List<WorkInfo> = emptyList(),
    ) : AssetMemoryAssemblyWorkScheduler {
        var drainCount: Int = 0
        var cancelCount: Int = 0
        private val infos = MutableStateFlow(initial)

        override fun enqueueDrain() {
            drainCount += 1
        }

        override fun enqueueContinuation() = Unit

        override fun cancel() {
            cancelCount += 1
        }

        override fun observeUniqueWork(): Flow<List<WorkInfo>> = infos

        fun emit(value: List<WorkInfo>) {
            infos.value = value
        }
    }
}
