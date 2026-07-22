package com.memora.app.data.saf

import android.database.Cursor
import android.database.MatrixCursor
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsProvider
import android.util.Base64
import java.io.IOException
import java.io.OutputStream
import kotlin.concurrent.thread

/**
 * Debug/test fixture only. It exposes one synthetic PDF through one tree and
 * rejects every mode except `r`. This source set is excluded from release APKs.
 */
class SyntheticPdfDocumentsProvider : DocumentsProvider() {
    override fun onCreate(): Boolean = true

    override fun queryRoots(projection: Array<String>?): Cursor = MatrixCursor(
        projection ?: DEFAULT_ROOT_PROJECTION,
    ).apply {
        newRow()
            .add(DocumentsContract.Root.COLUMN_ROOT_ID, ROOT_DOCUMENT_ID)
            .add(DocumentsContract.Root.COLUMN_DOCUMENT_ID, ROOT_DOCUMENT_ID)
            .add(DocumentsContract.Root.COLUMN_TITLE, "Memora synthetic PDF fixture")
            .add(DocumentsContract.Root.COLUMN_FLAGS, 0)
    }

    override fun queryDocument(documentId: String, projection: Array<String>?): Cursor = MatrixCursor(
        projection ?: DEFAULT_DOCUMENT_PROJECTION,
    ).apply {
        require(documentId == ROOT_DOCUMENT_ID || documentId == PDF_DOCUMENT_ID)
        addDocumentRow(this, documentId)
    }

    override fun queryChildDocuments(
        parentDocumentId: String,
        projection: Array<String>?,
        sortOrder: String?,
    ): Cursor = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION).apply {
        require(parentDocumentId == ROOT_DOCUMENT_ID)
        addDocumentRow(this, PDF_DOCUMENT_ID)
    }

    override fun openDocument(
        documentId: String,
        mode: String,
        signal: CancellationSignal?,
    ): ParcelFileDescriptor {
        require(documentId == PDF_DOCUMENT_ID)
        require(mode == "r") { "Synthetic fixture permits read-only opening only." }
        signal?.throwIfCanceled()

        val pipe = ParcelFileDescriptor.createPipe()
        thread(name = "memora-synthetic-pdf-fixture", isDaemon = true) {
            ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]).use { output ->
                try {
                    writeSyntheticPdf(output)
                } catch (_: IOException) {
                    // The broker may close its duplicate immediately after reading.
                    // This synthetic one-shot pipe has no durable source to protect.
                }
            }
        }
        return pipe[0]
    }

    override fun isChildDocument(parentDocumentId: String, documentId: String): Boolean =
        parentDocumentId == ROOT_DOCUMENT_ID && documentId == PDF_DOCUMENT_ID

    private fun addDocumentRow(cursor: MatrixCursor, documentId: String) {
        cursor.newRow()
            .add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, documentId)
            .add(
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                if (documentId == ROOT_DOCUMENT_ID) {
                    DocumentsContract.Document.MIME_TYPE_DIR
                } else {
                    PDF_MIME_TYPE
                },
            )
            .add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, "synthetic-report.pdf")
            .add(DocumentsContract.Document.COLUMN_SIZE, SYNTHETIC_PDF.size.toLong())
            .add(DocumentsContract.Document.COLUMN_LAST_MODIFIED, FIXTURE_MODIFIED_AT)
            .add(DocumentsContract.Document.COLUMN_FLAGS, 0)
    }

    private fun writeSyntheticPdf(output: OutputStream) {
        output.write(SYNTHETIC_PDF)
        output.flush()
    }

    internal companion object {
        val DEFAULT_ROOT_PROJECTION = arrayOf(
            DocumentsContract.Root.COLUMN_ROOT_ID,
            DocumentsContract.Root.COLUMN_DOCUMENT_ID,
            DocumentsContract.Root.COLUMN_TITLE,
            DocumentsContract.Root.COLUMN_FLAGS,
        )
        val DEFAULT_DOCUMENT_PROJECTION = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_FLAGS,
        )
        const val ROOT_DOCUMENT_ID = "memora-root"
        const val PDF_DOCUMENT_ID = "synthetic-report"
        const val PDF_MIME_TYPE = "application/pdf"
        const val FIXTURE_MODIFIED_AT = 1_735_689_600_000L
        /**
         * A one-page, selectable-text, repository-owned PDF. The provider has no real source
         * access and this small fixture is available only from the debug source set.
         */
        val SYNTHETIC_PDF = Base64.decode(
            "JVBERi0xLjQKJeLjz9MKMSAwIG9iago8PCAvVHlwZSAvQ2F0YWxvZyAvUGFnZXMgMiAwIFIgPj4K" +
                "ZW5kb2JqCjIgMCBvYmoKPDwgL1R5cGUgL1BhZ2VzIC9LaWRzIFszIDAgUl0gL0NvdW50IDEgPj4K" +
                "ZW5kb2JqCjMgMCBvYmoKPDwgL1R5cGUgL1BhZ2UgL1BhcmVudCAyIDAgUiAvTWVkaWFCb3ggWzAg" +
                "MCA2MTIgNzkyXSAvUmVzb3VyY2VzIDw8IC9Gb250IDw8IC9GMSA0IDAgUiA+PiA+PiAvQ29udGVu" +
                "dHMgNSAwIFIgPj4KZW5kb2JqCjQgMCBvYmoKPDwgL1R5cGUgL0ZvbnQgL1N1YnR5cGUgL1R5cGUx" +
                "IC9CYXNlRm9udCAvSGVsdmV0aWNhID4+CmVuZG9iago1IDAgb2JqCjw8IC9MZW5ndGggNDYgPj4K" +
                "c3RyZWFtCkJUIC9GMSAxMiBUZiA3MiA3MjAgVGQgKE1lbW9yYSBmaXh0dXJlKSBUaiBFVAplbmRz" +
                "dHJlYW0KZW5kb2JqCnhyZWYKMCA2CjAwMDAwMDAwMDAgNjU1MzUgZiAKMDAwMDAwMDAxNSAwMDAw" +
                "MCBuIAowMDAwMDAwMDY0IDAwMDAwIG4gCjAwMDAwMDAxMjEgMDAwMDAgbiAKMDAwMDAwMDI0NyAw" +
                "MDAwMCBuIAowMDAwMDAwMzE3IDAwMDAwIG4gCnRyYWlsZXIKPDwgL1NpemUgNiAvUm9vdCAxIDAg" +
                "UiA+PgpzdGFydHhyZWYKNDEyCiUlRU9GCg==",
            Base64.NO_WRAP,
        )
    }
}
