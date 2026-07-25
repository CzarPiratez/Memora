package com.memora.app.data.pdfbox.isolation;

import android.os.Bundle;
import android.os.ParcelFileDescriptor;

/**
 * Private Binder contract for protocol v3 session/chunk streaming.
 * One already-opened descriptor in; bounded header then one chunk per pull; no URI/path.
 */
interface IIsolatedPdfParser {
    Bundle begin(in ParcelFileDescriptor source, int protocolVersion);
    Bundle nextChunk();
    void cancel();
}
