package com.memora.app.work

import androidx.work.Data
import androidx.work.WorkInfo
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MeaningIndexWorkObservationTest {
    @Test
    fun empty_infos_are_idle() {
        assertEquals(
            MeaningIndexWorkPhase.Idle,
            MeaningIndexWorkObservation.phase(emptyList()),
        )
    }

    @Test
    fun running_uses_progress_remaining_and_sums_finished_indexed() {
        val infos = listOf(
            workInfo(
                WorkInfo.State.SUCCEEDED,
                indexed = 25,
                remaining = 100,
                hasMore = true,
            ),
            workInfo(
                state = WorkInfo.State.RUNNING,
                progressIndexed = 10,
                progressRemaining = 90,
                progressHasMore = true,
            ),
        )
        val phase = MeaningIndexWorkObservation.phase(infos)
        assertTrue(phase is MeaningIndexWorkPhase.Active)
        val totals = (phase as MeaningIndexWorkPhase.Active).totals
        assertEquals(35, totals.indexedMemories)
        assertEquals(90, totals.remainingPending)
        assertEquals(true, totals.hasMore)
    }

    @Test
    fun completed_chain_sums_every_succeeded_node() {
        val infos = listOf(
            workInfo(WorkInfo.State.SUCCEEDED, indexed = 50, remaining = 25, hasMore = true),
            workInfo(WorkInfo.State.SUCCEEDED, indexed = 25, remaining = 0, hasMore = false),
        )
        val phase = MeaningIndexWorkObservation.phase(infos)
        assertTrue(phase is MeaningIndexWorkPhase.Completed)
        val totals = (phase as MeaningIndexWorkPhase.Completed).totals
        assertEquals(75, totals.indexedMemories)
        assertEquals(0, totals.remainingPending)
        assertEquals(false, totals.hasMore)
    }

    @Test
    fun succeeded_with_stopped_flag_is_cancelled_not_completed() {
        val infos = listOf(
            workInfo(WorkInfo.State.SUCCEEDED, indexed = 25, remaining = 80, hasMore = true),
            workInfo(
                WorkInfo.State.SUCCEEDED,
                indexed = 0,
                remaining = 80,
                hasMore = true,
                stopped = true,
            ),
        )
        val phase = MeaningIndexWorkObservation.phase(infos)
        assertTrue(phase is MeaningIndexWorkPhase.Cancelled)
        assertEquals(25, (phase as MeaningIndexWorkPhase.Cancelled).totals.indexedMemories)
        assertEquals(80, phase.totals.remainingPending)
    }

    @Test
    fun cancelled_after_a_success_is_not_failed() {
        val infos = listOf(
            workInfo(WorkInfo.State.SUCCEEDED, indexed = 25, remaining = 80, hasMore = true),
            workInfo(WorkInfo.State.CANCELLED),
        )
        val phase = MeaningIndexWorkObservation.phase(infos)
        assertTrue(phase is MeaningIndexWorkPhase.Cancelled)
        assertEquals(25, (phase as MeaningIndexWorkPhase.Cancelled).totals.indexedMemories)
        assertEquals(80, phase.totals.remainingPending)
    }

    @Test
    fun engine_unavailable_flag_survives_on_completed() {
        val infos = listOf(
            workInfo(
                WorkInfo.State.SUCCEEDED,
                indexed = 0,
                remaining = 0,
                engineUnavailable = true,
            ),
        )
        val totals = MeaningIndexWorkObservation.totals(infos)
        assertEquals(true, totals.engineUnavailable)
    }

    private fun workInfo(
        state: WorkInfo.State,
        indexed: Int = 0,
        remaining: Int = 0,
        hasMore: Boolean = false,
        engineUnavailable: Boolean = false,
        stopped: Boolean = false,
        progressIndexed: Int? = null,
        progressRemaining: Int? = null,
        progressHasMore: Boolean? = null,
    ): WorkInfo {
        val output = if (state == WorkInfo.State.SUCCEEDED) {
            Data.Builder()
                .putInt(MeaningIndexWorker.KEY_INDEXED, indexed)
                .putInt(MeaningIndexWorker.KEY_REMAINING, remaining)
                .putBoolean(MeaningIndexWorker.KEY_HAS_MORE, hasMore)
                .putBoolean(MeaningIndexWorker.KEY_ENGINE_UNAVAILABLE, engineUnavailable)
                .putBoolean(MeaningIndexWorker.KEY_STOPPED, stopped)
                .build()
        } else {
            Data.EMPTY
        }
        val progress = Data.Builder().apply {
            progressIndexed?.let { putInt(MeaningIndexWorker.KEY_INDEXED, it) }
            progressRemaining?.let { putInt(MeaningIndexWorker.KEY_REMAINING, it) }
            progressHasMore?.let { putBoolean(MeaningIndexWorker.KEY_HAS_MORE, it) }
        }.build()
        return WorkInfo(
            UUID.randomUUID(),
            state,
            emptySet(),
            output,
            progress,
            1,
            1,
        )
    }
}
