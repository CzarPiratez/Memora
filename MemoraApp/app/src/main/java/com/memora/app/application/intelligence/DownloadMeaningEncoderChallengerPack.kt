package com.memora.app.application.intelligence

import com.memora.app.domain.intelligence.MeaningEncoderChallengerDownloader
import com.memora.app.domain.intelligence.MeaningEncoderChallengerPackPresence
import com.memora.app.domain.intelligence.OnDeviceEmbeddingModelDownloadOutcome
import javax.inject.Inject

/**
 * Downloads the BGE-small challenger pack for the encoder bake-off only.
 * Does not touch the product USE pack or the meaning index.
 */
class DownloadMeaningEncoderChallengerPack @Inject constructor(
    private val presence: MeaningEncoderChallengerPackPresence,
    private val downloader: MeaningEncoderChallengerDownloader,
) {
    operator fun invoke(): DownloadMeaningEncoderChallengerPackResult {
        if (presence.isInstalled()) {
            return DownloadMeaningEncoderChallengerPackResult.AlreadyInstalled
        }
        return when (val outcome = downloader.downloadChallengerModel()) {
            OnDeviceEmbeddingModelDownloadOutcome.Installed ->
                DownloadMeaningEncoderChallengerPackResult.Installed
            is OnDeviceEmbeddingModelDownloadOutcome.Failed ->
                DownloadMeaningEncoderChallengerPackResult.Failed(outcome.reason)
        }
    }
}

sealed interface DownloadMeaningEncoderChallengerPackResult {
    data object AlreadyInstalled : DownloadMeaningEncoderChallengerPackResult

    data object Installed : DownloadMeaningEncoderChallengerPackResult

    data class Failed(val reason: String) : DownloadMeaningEncoderChallengerPackResult {
        init {
            require(reason.isNotBlank())
        }
    }
}
