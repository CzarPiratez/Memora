package com.memora.app.data.notes

/**
 * Deterministic HTML → plain text for OneNote Graph page content.
 * Not a full browser renderer; strips tags/scripts and decodes common entities.
 */
object OneNoteHtmlPlainText {
    fun toPlainText(html: String, maxChars: Int): Pair<String, Boolean> {
        require(maxChars > 0)
        var text = html
        text = SCRIPT_OR_STYLE.replace(text, " ")
        text = BREAK.replace(text, "\n")
        text = BLOCK_CLOSE.replace(text, "\n")
        text = TAG.replace(text, " ")
        text = decodeBasicEntities(text)
        text = WHITESPACE.replace(text, " ")
        text = MULTI_NEWLINE.replace(text, "\n\n").trim()
        val truncated = text.length > maxChars
        return text.take(maxChars) to truncated
    }

    private fun decodeBasicEntities(value: String): String {
        var out = value
            .replace("&nbsp;", " ", ignoreCase = true)
            .replace("&amp;", "&", ignoreCase = true)
            .replace("&lt;", "<", ignoreCase = true)
            .replace("&gt;", ">", ignoreCase = true)
            .replace("&quot;", "\"", ignoreCase = true)
            .replace("&#39;", "'")
            .replace("&apos;", "'", ignoreCase = true)
        out = NUMERIC_ENTITY.replace(out) { match ->
            val code = match.groupValues[1].toIntOrNull() ?: return@replace match.value
            if (code in 1..0x10FFFF) String(Character.toChars(code)) else match.value
        }
        out = HEX_ENTITY.replace(out) { match ->
            val code = match.groupValues[1].toIntOrNull(16) ?: return@replace match.value
            if (code in 1..0x10FFFF) String(Character.toChars(code)) else match.value
        }
        return out
    }

    private val SCRIPT_OR_STYLE =
        Regex("(?is)<(script|style)\\b[^>]*>.*?</\\1>")
    private val BREAK = Regex("(?is)<br\\s*/?>")
    private val BLOCK_CLOSE = Regex("(?is)</(p|div|li|h[1-6]|tr)>")
    private val TAG = Regex("(?is)<[^>]+>")
    private val WHITESPACE = Regex("[ \\t\\x0B\\f\\r]+")
    private val MULTI_NEWLINE = Regex("\\n{3,}")
    private val NUMERIC_ENTITY = Regex("&#(\\d+);")
    private val HEX_ENTITY = Regex("&#x([0-9a-fA-F]+);")
}
