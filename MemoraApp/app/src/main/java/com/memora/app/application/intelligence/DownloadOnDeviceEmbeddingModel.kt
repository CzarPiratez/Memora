package com.memora.app.application.intelligence

import com.memora.app.domain.intelligence.AiPackInstallLedger
import com.memora.app.domain.intelligence.EmbeddingFirstAiPackTrack
import com.memora.app.domain.intelligence.OnDeviceEmbeddingModelDownloadOutcome
import com.memora.app.domain.intelligence.OnDeviceEmbeddingModelDownloader
import com.memora.app.domain.intelligence.OnDeviceEmbeddingModelStore
import javax.inject.Inject

/**
 * Downloads the product BGE-small meaning pack into private storage
 * (ADR-055 slice 4).
 *
 * Requires prior disclosure acknowledgment. Downloads model bytes only — never
 * uploads Memories or source content. After install, retired USE / average-word
 * vectors are purged so Room never mixes pack identities.
 */
class DownloadOnDeviceEmbeddingModel @Inject constructor(
    private val ledger: AiPackInstallLedger,
    private val modelStore: OnDeviceEmbeddingModelStore,
    private val downloader: OnDeviceEmbeddingModelDownloader,
    private val purgeRetiredMeaningEmbeddings: PurgeRetiredMeaningEmbeddings,
) {
    operator fun invoke(): DownloadOnDeviceEmbeddingModelResult {
        val entry = ledger.entry(EmbeddingFirstAiPackTrack.PLANNED_PACK_ID)
        if (entry?.disclosureAcknowledgedAtEpochMs == null) {
            return DownloadOnDeviceEmbeddingModelResult.DisclosureRequired
        }
        if (modelStore.isInstalled()) {
            return DownloadOnDeviceEmbeddingModelResult.AlreadyInstalled
        }

        return when (val outcome = downloader.downloadProductModel()) {
            OnDeviceEmbeddingModelDownloadOutcome.Installed -> {
                purgeRetiredMeaningEmbeddings()
                DownloadOnDeviceEmbeddingModelResult.Installed
            }
            is OnDeviceEmbeddingModelDownloadOutcome.Failed ->
                DownloadOnDeviceEmbeddingModelResult.Failed(outcome.reason)
        }
    }
}

sealed interface DownloadOnDeviceEmbeddingModelResult {
    data object DisclosureRequired : DownloadOnDeviceEmbeddingModelResult

    data object AlreadyInstalled : DownloadOnDeviceEmbeddingModelResult

    data object Installed : DownloadOnDeviceEmbeddingModelResult

    data class Failed(val reason: String) : DownloadOnDeviceEmbeddingModelResult {
        init {
            require(reason.isNotBlank())
        }
    }
}
