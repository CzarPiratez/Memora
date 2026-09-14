package com.memora.app.application.handoff

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OpenOriginalInAnotherAppTest {
    @Test
    fun a_ready_original_is_handed_to_the_viewer_with_its_own_mime_type() = runTest {
        val viewer = RecordingViewer(ExternalViewOutcome.Opened)
        val open = OpenOriginalInAnotherApp(
            prepare = {
                PreparedOriginalHandoff.Ready(
                    uri = "content://tree/document/pool",
                    mimeType = OriginalHandoffMime.PDF,
                    label = "pool.pdf",
                )
            },
            viewer = viewer,
        )

        val outcome = open(OriginalHandoffRequest.Pdf("saf", "doc-1", "pool.pdf"))

        assertEquals(OpenOriginalOutcome.Opened, outcome)
        assertEquals("content://tree/document/pool", viewer.uri)
        assertEquals(OriginalHandoffMime.PDF, viewer.mimeType)
        assertEquals("pool.pdf", viewer.label)
    }

    @Test
    fun no_reader_installed_is_its_own_outcome_so_the_file_is_not_blamed() = runTest {
        val open = OpenOriginalInAnotherApp(
            prepare = {
                PreparedOriginalHandoff.Ready("content://media/1", "image/png", "shot.png")
            },
            viewer = RecordingViewer(ExternalViewOutcome.NoAppAvailable),
        )

        assertEquals(
            OpenOriginalOutcome.NoAppAvailable,
            open(OriginalHandoffRequest.Screenshot("media", "p1", "shot.png")),
        )
    }

    @Test
    fun an_unreachable_original_never_reaches_the_viewer() = runTest {
        val viewer = RecordingViewer(ExternalViewOutcome.Opened)
        val open = OpenOriginalInAnotherApp(
            prepare = { PreparedOriginalHandoff.SourceUnavailable },
            viewer = viewer,
        )

        assertEquals(
            OpenOriginalOutcome.SourceUnavailable,
            open(OriginalHandoffRequest.Photo("media", "p1", "receipt.jpg")),
        )
        assertNull(viewer.uri)
    }

    @Test
    fun a_failed_handoff_reads_as_could_not_open() = runTest {
        val open = OpenOriginalInAnotherApp(
            prepare = { PreparedOriginalHandoff.CouldNotHandOff },
            viewer = RecordingViewer(ExternalViewOutcome.Opened),
        )

        assertEquals(
            OpenOriginalOutcome.CouldNotOpen,
            open(OriginalHandoffRequest.Pdf("saf", "doc-1", "pool.pdf")),
        )
    }

    private class RecordingViewer(
        private val outcome: ExternalViewOutcome,
    ) : ExternalOriginalViewer {
        var uri: String? = null
        var mimeType: String? = null
        var label: String? = null

        override fun view(uri: String, mimeType: String, label: String): ExternalViewOutcome {
            this.uri = uri
            this.mimeType = mimeType
            this.label = label
            return outcome
        }
    }
}
