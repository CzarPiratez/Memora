package com.memora.app.application.intelligence

import com.memora.app.domain.intelligence.AiPackInstallState
import com.memora.app.domain.intelligence.AiPackPayloadStore
import com.memora.app.domain.intelligence.AiPackPayloadVerifier
import com.memora.app.domain.intelligence.EmbeddingFirstAiPackTrack
import com.memora.app.domain.intelligence.InMemoryAiPackInstallLedger
import com.memora.app.domain.intelligence.LedgerBackedAiPackManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivateOfflineEmbeddingPackContainerTest {
    @Test
    fun activates_after_disclosure_without_network() {
        val ledger = InMemoryAiPackInstallLedger()
        val store = InMemoryAiPackPayloadStore()
        ledger.acknowledgeDisclosure(
            EmbeddingFirstAiPackTrack.plannedDisclosure(atEpochMs = 1L),
        )
        val useCase = ActivateOfflineEmbeddingPackContainer(
            ledger = ledger,
            payloadStore = store,
            verifier = AiPackPayloadVerifier(),
        )

        val result = useCase(nowEpochMs = 2L)
        assertEquals(ActivateOfflineEmbeddingPackResult.Activated, result)
        assertEquals(
            AiPackInstallState.ACTIVE,
            LedgerBackedAiPackManager(ledger)
                .installationState(EmbeddingFirstAiPackTrack.PLANNED_PACK_ID),
        )
        assertTrue(
            store.readPayload(EmbeddingFirstAiPackTrack.PLANNED_PACK_ID)?.isNotEmpty() == true,
        )
    }

    @Test
    fun requires_disclosure_first() {
        val useCase = ActivateOfflineEmbeddingPackContainer(
            ledger = InMemoryAiPackInstallLedger(),
            payloadStore = InMemoryAiPackPayloadStore(),
            verifier = AiPackPayloadVerifier(),
        )
        assertEquals(
            ActivateOfflineEmbeddingPackResult.DisclosureRequired,
            useCase(nowEpochMs = 1L),
        )
    }

    private class InMemoryAiPackPayloadStore : AiPackPayloadStore {
        private val payloads = linkedMapOf<String, ByteArray>()

        override fun writePayload(packId: String, payload: ByteArray) {
            payloads[packId] = payload.copyOf()
        }

        override fun readPayload(packId: String): ByteArray? = payloads[packId]?.copyOf()

        override fun deletePayload(packId: String) {
            payloads.remove(packId)
        }

        override fun clearAll() {
            payloads.clear()
        }
    }
}
