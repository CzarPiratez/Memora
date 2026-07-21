package com.memora.app.data.pdfbox.isolation;

import android.os.Bundle;
import android.os.ParcelFileDescriptor;

/** Private interface: one already-opened descriptor in, bounded status facts out. */
interface IIsolatedPdfParser {
    Bundle parse(in ParcelFileDescriptor source, int protocolVersion);
}
