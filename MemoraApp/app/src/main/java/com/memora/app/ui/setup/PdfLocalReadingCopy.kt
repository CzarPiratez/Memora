package com.memora.app.ui.setup

/**
 * Plain-language copy for the ADR-017 visible PDF local-reading recovery flow.
 *
 * These strings must remain free of encryption, Binder, and parser-implementation jargon.
 * This object does not open documents or invoke the isolated parser.
 */
object PdfLocalReadingCopy {
    const val SECTION_TITLE = "Local PDF reading"

    const val SCOPE_BODY =
        "When Memora reads PDF text, it will use only the folder you connected, " +
            "keep that access read-only, and leave your original files unchanged. " +
            "Reading stays on this phone."

    const val CONTINUE_LABEL = "Continue"

    const val READY_BODY =
        "Start reads indexed PDFs from your connected folder on this phone in the background " +
            "and, when reading succeeds, saves their text for search on this phone."

    const val START_LABEL = "Start local reading"

    const val IN_PROGRESS_BODY =
        "Memora is reading PDFs from your connected folder on this phone. " +
            "When reading succeeds, text is saved for search here. Your originals are unchanged."

    const val PAUSE_LABEL = "Pause"

    const val PAUSED_BODY =
        "Local reading is paused. You can resume or stop."

    const val RESUME_LABEL = "Resume"

    const val STOP_LABEL = "Stop"

    const val COMPLETED_BODY =
        "Memora finished reading PDFs from this folder and saved text for search on this phone " +
            "where reading succeeded. Your originals are unchanged."

    const val RETRYABLE_BODY =
        "Memora could not finish this local reading step. Index the PDF folder if you have not, " +
            "then try again. Your original PDFs are unchanged."

    const val RETRY_LABEL = "Try again"

    const val ACCESS_RECOVERY_BODY =
        "Memora no longer has read access to this PDF folder. Reconnect the folder to continue. " +
            "Your original PDFs are unchanged."

    const val RECONNECT_HINT = "Use Connect another PDF folder above when you are ready."

    const val PASSWORD_BODY =
        "This PDF needs a password before Memora can read its text. Memora does not store or guess passwords. " +
            "Your original file is unchanged."

    const val UNAVAILABLE_BODY =
        "Memora could not find a PDF ready for this local reading check. Index the connected folder, " +
            "then try again. Your original PDFs are unchanged."

    /** Demo control so retry copy can be verified without forcing a failing document. */
    const val SHOW_RETRYABLE_DEMO_LABEL = "Show example problem"
}
