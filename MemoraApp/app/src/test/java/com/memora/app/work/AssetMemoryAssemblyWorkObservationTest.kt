package com.memora.app.work

import androidx.work.Data
import androidx.work.WorkInfo
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AssetMemoryAssemblyWorkObservationTest {
    @Test
    fun empty_infos_are_idle() {
        assertEquals(
            AssetMemoryAssemblyWorkPhase.Idle,
            AssetMemoryAssemblyWorkObservation.phase(emptyList()),
        )
    }

    @Test
    fun running_or_enqueued_is_active_and_sums_finished_batches() {
        val infos = listOf(
            workInfo(
                WorkInfo.State.SUCCEEDED,
                assembled = 25,
                skipped = 1,
                ready = 26,
                hasMore = true,
            ),
            workInfo(WorkInfo.State.RUNNING),
        )
        val phase = AssetMemoryAssemblyWorkObservation.phase(infos)
        assertTrue(phase is AssetMemoryAssemblyWorkPhase.Active)
        val totals = (phase as AssetMemoryAssemblyWorkPhase.Active).totals
        assertEquals(25, totals.assembled)
        assertEquals(1, totals.skipped)
        assertEquals(26, totals.readyCount)
        assertEquals(true, totals.hasMore)
    }

    @Test
    fun completed_chain_sums_every_succeeded_batch() {
        val infos = listOf(
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
                skipped = 2,
                ready = 28,
                hasMore = false,
            ),
        )
        val phase = AssetMemoryAssemblyWorkObservation.phase(infos)
        assertTrue(phase is AssetMemoryAssemblyWorkPhase.Completed)
        val totals = (phase as AssetMemoryAssemblyWorkPhase.Completed).totals
        assertEquals(28, totals.assembled)
        assertEquals(2, totals.skipped)
        assertEquals(28, totals.readyCount)
        assertEquals(false, totals.hasMore)
    }

    @Test
    fun cancelled_after_a_success_is_not_failed() {
        val infos = listOf(
            workInfo(
                WorkInfo.State.SUCCEEDED,
                assembled = 25,
                skipped = 0,
                ready = 25,
                hasMore = true,
            ),
            workInfo(WorkInfo.State.CANCELLED),
        )
        val phase = AssetMemoryAssemblyWorkObservation.phase(infos)
        assertTrue(phase is AssetMemoryAssemblyWorkPhase.Cancelled)
        assertEquals(25, (phase as AssetMemoryAssemblyWorkPhase.Cancelled).totals.assembled)
    }

    @Test
    fun terminal_failure_wins_over_cancel_in_the_same_chain() {
        val infos = listOf(
            workInfo(WorkInfo.State.FAILED),
            workInfo(WorkInfo.State.CANCELLED),
        )
        assertEquals(
            AssetMemoryAssemblyWorkPhase.Failed,
            AssetMemoryAssemblyWorkObservation.phase(infos),
        )
    }

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
}
