package com.memora.app.application.intelligence

import com.memora.app.domain.intelligence.MeaningEncoderChallengerDownloader
import com.memora.app.domain.intelligence.MeaningEncoderChallengerPackPresence
import com.memora.app.domain.intelligence.OnDeviceEmbeddingModelDownloadOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadMeaningEncoderChallengerPackTest {
    @Test
    fun already_installed_does_not_download() {
        val downloader = RecordingDownloader()
        val result = DownloadMeaningEncoderChallengerPack(
            presence = MeaningEncoderChallengerPackPresence { true },
            downloader = downloader,
        )()
        assertEquals(DownloadMeaningEncoderChallengerPackResult.AlreadyInstalled, result)
        assertEquals(0, downloader.calls)
    }

    @Test
    fun installs_when_downloader_succeeds() {
        val result = DownloadMeaningEncoderChallengerPack(
            presence = MeaningEncoderChallengerPackPresence { false },
            downloader = RecordingDownloader(
                outcome = OnDeviceEmbeddingModelDownloadOutcome.Installed,
            ),
        )()
        assertEquals(DownloadMeaningEncoderChallengerPackResult.Installed, result)
    }

    @Test
    fun surfaces_download_failure() {
        val result = DownloadMeaningEncoderChallengerPack(
            presence = MeaningEncoderChallengerPackPresence { false },
            downloader = RecordingDownloader(
                outcome = OnDeviceEmbeddingModelDownloadOutcome.Failed("network down"),
            ),
        )()
        assertTrue(result is DownloadMeaningEncoderChallengerPackResult.Failed)
        assertEquals(
            "network down",
            (result as DownloadMeaningEncoderChallengerPackResult.Failed).reason,
        )
    }

    private class RecordingDownloader(
        private val outcome: OnDeviceEmbeddingModelDownloadOutcome =
            OnDeviceEmbeddingModelDownloadOutcome.Installed,
    ) : MeaningEncoderChallengerDownloader {
        var calls = 0
        override fun downloadChallengerModel(): OnDeviceEmbeddingModelDownloadOutcome {
            calls += 1
            return outcome
        }
    }
}
