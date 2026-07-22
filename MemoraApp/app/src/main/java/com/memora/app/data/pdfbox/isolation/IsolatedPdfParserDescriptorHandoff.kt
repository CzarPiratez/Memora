package com.memora.app.data.pdfbox.isolation

import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import java.io.IOException

/**
 * Consumes a descriptor borrowed from a source-custody component.
 *
 * The parser client owns and closes every descriptor supplied to it. This port therefore never
 * transfers the borrowed handle itself; the Android implementation duplicates it first and gives
 * the client the duplicate. The custody component remains solely responsible for closing its own
 * borrowed descriptor when this call returns.
 */
internal fun interface BorrowedPdfDescriptorParser {
    fun parseBorrowed(
        borrowedDescriptor: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal?,
    ): IsolatedPdfParserClientResult
}

/**
 * Ownership adapter between the SAF broker and the isolated parser client.
 *
 * It has no URI, source identity, content, Room, UI, or worker dependency. A duplication failure
 * is deliberately reduced to the same content-free retryable failure as a transport failure.
 */
internal class IsolatedPdfParserDescriptorHandoff(
    private val client: IsolatedPdfParserClient,
) : BorrowedPdfDescriptorParser {
    override fun parseBorrowed(
        borrowedDescriptor: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal?,
    ): IsolatedPdfParserClientResult = try {
        client.parse(
            source = borrowedDescriptor.dup(),
            cancellationSignal = cancellationSignal,
        )
    } catch (_: IOException) {
        IsolatedPdfParserClientResult(
            outcome = IsolatedPdfParserClientOutcome.FAILURE,
            retryable = true,
        )
    }
}
