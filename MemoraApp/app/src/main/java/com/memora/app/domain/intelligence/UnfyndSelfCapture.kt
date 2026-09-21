package com.memora.app.domain.intelligence

/**
 * Pictures of UNFYND itself (Ask Model D16 / defect D-14).
 *
 * A screenshot of Find contains the product chrome — "Search by meaning on
 * this phone", "Find by meaning", the former Welcome prompt "What are you
 * trying to remember?" — so it matches the cue and outranks the original file.
 * Detection uses those stored phrases plus an UNFYND-labelled filename. Not a
 * filename-only denylist. The retired Welcome prompt stays listed so older
 * self-captures still demote.
 *
 * Two chrome markers, or one marker plus an UNFYND label, is a self-capture.
 * One stray phrase in a real document is not.
 */
object UnfyndSelfCapture {
    const val FILE_IS = "a screenshot of UNFYND, not the original file"

    fun matches(label: String, text: String): Boolean {
        if (ANDROID_APP_SCREENSHOT.containsMatchIn(label.trim())) return true
        val haystack = text.lowercase()
        val chromeHits = CHROME.count { haystack.contains(it) }
        val labelled = label.contains("UNFYND", ignoreCase = true)
        return chromeHits >= 2 || (chromeHits >= 1 && labelled)
    }

    /**
     * Android names a screenshot of this app `Screenshot_<stamp>_UNFYND`.
     * OCR of that shot is often only the document on screen, so chrome
     * phrases are missing. The filename is still the self-capture. A
     * real PDF whose title happens to mention UNFYND is not this pattern.
     */
    private val ANDROID_APP_SCREENSHOT =
        Regex("""^screenshot_\d+.*unfynd""", RegexOption.IGNORE_CASE)

    private val CHROME = listOf(
        "what are you trying to remember",
        "your privacy first, on-device ai",
        "search by meaning on this phone",
        "find by meaning",
        "why this result?",
        "pdf memory",
        "screenshot memory",
        "open original",
        "opening unfynd",
    )
}
