package com.memora.app.ui.search

/**
 * Hide from Find — user-initiated visibility, not CR-08 erase.
 */
object FindHiddenCopy {
    const val HIDE_LABEL = "Hide from Find"
    const val SHOW_AGAIN_LABEL = "Show again"
    const val HIDDEN_NOTICE = "Hidden from Find"
    const val ALL_HIDDEN_BODY =
        "This search matched files you hid from Find. Memory is still on this phone."

    fun panelTitle(count: Int): String {
        require(count > 0) { "Hidden-from-this-search copy needs a count." }
        return if (count == 1) {
            "1 hidden from this search"
        } else {
            "$count hidden from this search"
        }
    }
}
