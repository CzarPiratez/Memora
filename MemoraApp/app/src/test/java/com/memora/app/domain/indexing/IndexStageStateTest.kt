package com.memora.app.domain.indexing

import com.memora.app.domain.asset.AssetFingerprint
import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IndexStageStateTest {
    private val identity = AssetIdentity(SourceId("test-src"), SourceAssetKey("key-1"))
    private val fingerprint = AssetFingerprint("key-1:gen-1:100")

    @Test
    fun compute_derivation_id_formats_consistently() {
        val id = IndexStageState.computeDerivationId(
            stage = IndexStage.OCR,
            schemaVersion = "v1",
            engineVersion = "16.0.1",
        )
        assertEquals("ocr:v1:16.0.1", id)
    }

    @Test
    fun domain_model_stores_stage_and_failure_taxonomy_separately() {
        val state = IndexStageState(
            assetIdentity = identity,
            fingerprint = fingerprint,
            stage = IndexStage.OCR,
            derivationId = "ocr:v1:16.0.1",
            currentStatus = IndexStageStatus.VALID,
            lastAttemptStatus = "SUCCESS",
            lastFailureClass = null,
            attemptCount = 1,
            engineVersion = "16.0.1",
        )

        assertEquals(IndexStageStatus.VALID, state.currentStatus)
        assertEquals("SUCCESS", state.lastAttemptStatus)
        assertNull(state.lastFailureClass)
        assertEquals(1, state.attemptCount)
    }

    @Test
    fun failed_retry_preserves_valid_current_status_with_failed_attempt() {
        val state = IndexStageState(
            assetIdentity = identity,
            fingerprint = fingerprint,
            stage = IndexStage.OCR,
            derivationId = "ocr:v1:16.0.1",
            currentStatus = IndexStageStatus.VALID,
            lastAttemptStatus = "FAILED",
            lastFailureClass = IndexFailureClass.RETRYABLE,
            lastFailureCode = "OCR_TIMEOUT",
            lastFailureMessage = "OCR read timed out",
            attemptCount = 2,
            engineVersion = "16.0.1",
        )

        assertEquals(IndexStageStatus.VALID, state.currentStatus)
        assertEquals("FAILED", state.lastAttemptStatus)
        assertEquals(IndexFailureClass.RETRYABLE, state.lastFailureClass)
        assertEquals("OCR_TIMEOUT", state.lastFailureCode)
        assertEquals(2, state.attemptCount)
    }
}
