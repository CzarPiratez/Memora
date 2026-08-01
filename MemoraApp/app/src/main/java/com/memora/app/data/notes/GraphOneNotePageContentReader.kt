package com.memora.app.data.notes

import com.memora.app.application.notes.OneNoteInteractiveAuth
import com.memora.app.domain.asset.Asset
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.extraction.NotePageExtractionRecord
import com.memora.app.domain.extraction.NotePageReadResult
import javax.inject.Inject
import javax.inject.Singleton

/** Reads OneNote page HTML via Graph and converts to plain text for N4 extract. */
interface OneNotePageContentReader {
    suspend fun read(asset: Asset): NotePageReadResult
}

@Singleton
class GraphOneNotePageContentReader @Inject constructor(
    private val oneNoteAuth: OneNoteInteractiveAuth,
    private val graphGateway: OneNotePagesGraphGateway,
) : OneNotePageContentReader {
    override suspend fun read(asset: Asset): NotePageReadResult {
        if (asset.type != AssetType.NOTE) return NotePageReadResult.RetryableFailure
        val session = oneNoteAuth.ensureSession()
            ?: return NotePageReadResult.AccessStopped
        val contentUrl = contentRequestUrl(asset.location.value)
            ?: return NotePageReadResult.RetryableFailure

        return when (
            val content = graphGateway.fetchPageContent(session.accessToken, contentUrl)
        ) {
            OneNotePageContentGraphResult.Unauthorized -> NotePageReadResult.AccessStopped
            is OneNotePageContentGraphResult.Failed -> NotePageReadResult.RetryableFailure
            is OneNotePageContentGraphResult.Ok -> {
                val (plain, truncated) = OneNoteHtmlPlainText.toPlainText(
                    html = content.html,
                    maxChars = NotePageExtractionRecord.MAX_STORED_CHARS,
                )
                NotePageReadResult.Text(
                    fullText = plain,
                    textTruncated = truncated,
                    engineId = NotePageExtractionRecord.ENGINE_GRAPH_HTML_PLAINTEXT,
                    engineVersion = NotePageExtractionRecord.ENGINE_VERSION_V1,
                )
            }
        }
    }

    companion object {
        fun contentRequestUrl(location: String): String? {
            val trimmed = location.trim()
            if (trimmed.isEmpty()) return null
            if (!(trimmed.startsWith("http://") || trimmed.startsWith("https://"))) return null
            return when {
                trimmed.endsWith("/content") -> trimmed
                trimmed.contains("/onenote/pages/") -> trimmed.trimEnd('/') + "/content"
                else -> trimmed
            }
        }
    }
}
