package com.memora.app.domain.intelligence

/**
 * Bert WordPiece encoder for MS MARCO MiniLM cross-encoder pairs (ADR-051 Stage A).
 *
 * Matches HuggingFace `BertTokenizer` settings used by
 * `temsa/ms-marco-MiniLM-L-6-v2-onnx-cpu-qint8` (`do_lower_case=true`).
 * Not a product Find path by itself.
 */
class BertWordPieceTokenizer(
    private val tokenToId: Map<String, Int>,
) {
    data class EncodedPair(
        val inputIds: LongArray,
        val attentionMask: LongArray,
        val tokenTypeIds: LongArray,
    ) {
        init {
            require(inputIds.size == attentionMask.size)
            require(inputIds.size == tokenTypeIds.size)
            require(inputIds.isNotEmpty())
        }

        val length: Int get() = inputIds.size
    }

    init {
        require(tokenToId.isNotEmpty()) { "BERT vocab cannot be empty." }
        require(tokenToId.containsKey(CLS) && tokenToId.containsKey(SEP) && tokenToId.containsKey(PAD)) {
            "BERT vocab must include [CLS], [SEP], and [PAD]."
        }
    }

    fun encodePair(
        query: String,
        passage: String,
        maxLength: Int = DEFAULT_MAX_LENGTH,
    ): EncodedPair {
        require(query.isNotBlank())
        require(passage.isNotBlank())
        require(maxLength >= 8)

        val queryIds = wordPiece(basicTokenize(query))
        val passageIds = wordPiece(basicTokenize(passage))
        // [CLS] query [SEP] passage [SEP] — truncate passage first to fit.
        val maxContent = maxLength - 3
        var q = queryIds
        var p = passageIds
        if (q.size + p.size > maxContent) {
            val qKeep = q.size.coerceAtMost(maxContent / 2)
            q = q.take(qKeep)
            p = p.take((maxContent - q.size).coerceAtLeast(0))
        }

        val ids = ArrayList<Int>(maxLength)
        val types = ArrayList<Int>(maxLength)
        ids.add(idOf(CLS))
        types.add(0)
        q.forEach {
            ids.add(it)
            types.add(0)
        }
        ids.add(idOf(SEP))
        types.add(0)
        p.forEach {
            ids.add(it)
            types.add(1)
        }
        ids.add(idOf(SEP))
        types.add(1)

        while (ids.size < maxLength) {
            ids.add(idOf(PAD))
            types.add(0)
        }

        val inputIds = LongArray(maxLength) { ids[it].toLong() }
        val attention = LongArray(maxLength) { index ->
            if (ids[index] == idOf(PAD)) 0L else 1L
        }
        val tokenTypes = LongArray(maxLength) { types[it].toLong() }
        return EncodedPair(inputIds, attention, tokenTypes)
    }

    /**
     * Single-sequence encode for bi-encoder packs (BGE / MiniLM sentence).
     * Shape: [CLS] tokens [SEP] + pad. All token_type_ids are 0.
     */
    fun encodeSingle(
        text: String,
        maxLength: Int = DEFAULT_MAX_LENGTH,
    ): EncodedPair {
        require(text.isNotBlank())
        require(maxLength >= 4)

        val content = wordPiece(basicTokenize(text))
        val maxContent = maxLength - 2
        val kept = content.take(maxContent)

        val ids = ArrayList<Int>(maxLength)
        ids.add(idOf(CLS))
        kept.forEach { ids.add(it) }
        ids.add(idOf(SEP))
        while (ids.size < maxLength) {
            ids.add(idOf(PAD))
        }

        val inputIds = LongArray(maxLength) { ids[it].toLong() }
        val attention = LongArray(maxLength) { index ->
            if (ids[index] == idOf(PAD)) 0L else 1L
        }
        val tokenTypes = LongArray(maxLength) { 0L }
        return EncodedPair(inputIds, attention, tokenTypes)
    }

    private fun basicTokenize(text: String): List<String> {
        val lowered = text.lowercase()
        val cleaned = StringBuilder()
        for (ch in lowered) {
            when {
                ch.isWhitespace() -> cleaned.append(' ')
                isPunctuation(ch) -> cleaned.append(' ').append(ch).append(' ')
                else -> cleaned.append(ch)
            }
        }
        return cleaned.split(WHITESPACE).map { it.trim() }.filter { it.isNotEmpty() }
    }

    private fun wordPiece(tokens: List<String>): List<Int> {
        val out = ArrayList<Int>()
        for (token in tokens) {
            if (tokenToId.containsKey(token)) {
                out.add(tokenToId.getValue(token))
                continue
            }
            var start = 0
            var matchedAny = false
            while (start < token.length) {
                var end = token.length
                var found: Int? = null
                while (start < end) {
                    val piece = if (start == 0) {
                        token.substring(start, end)
                    } else {
                        "##" + token.substring(start, end)
                    }
                    val id = tokenToId[piece]
                    if (id != null) {
                        found = id
                        break
                    }
                    end -= 1
                }
                if (found == null) {
                    out.add(idOf(UNK))
                    matchedAny = true
                    break
                }
                out.add(found)
                matchedAny = true
                start = end
            }
            if (!matchedAny) {
                out.add(idOf(UNK))
            }
        }
        return out
    }

    private fun idOf(token: String): Int =
        tokenToId[token] ?: error("Missing required special token $token")

    companion object {
        const val CLS = "[CLS]"
        const val SEP = "[SEP]"
        const val PAD = "[PAD]"
        const val UNK = "[UNK]"
        const val DEFAULT_MAX_LENGTH = 128
        private val WHITESPACE = Regex("\\s+")

        fun loadFromLines(lines: Sequence<String>): BertWordPieceTokenizer {
            val map = LinkedHashMap<String, Int>()
            lines.forEachIndexed { index, raw ->
                val token = raw.trimEnd('\r')
                if (token.isNotEmpty() || raw.isNotEmpty()) {
                    // Empty vocab lines are rare; preserve exact line including empty.
                    map.putIfAbsent(token, index)
                }
            }
            require(map.isNotEmpty())
            return BertWordPieceTokenizer(map)
        }

        fun loadFromReader(reader: java.io.BufferedReader): BertWordPieceTokenizer =
            reader.use { loadFromLines(it.lineSequence()) }

        private fun isPunctuation(ch: Char): Boolean =
            ch in "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~" ||
                Character.getType(ch) == Character.CONNECTOR_PUNCTUATION.toInt() ||
                Character.getType(ch) == Character.DASH_PUNCTUATION.toInt() ||
                Character.getType(ch) == Character.START_PUNCTUATION.toInt() ||
                Character.getType(ch) == Character.END_PUNCTUATION.toInt() ||
                Character.getType(ch) == Character.INITIAL_QUOTE_PUNCTUATION.toInt() ||
                Character.getType(ch) == Character.FINAL_QUOTE_PUNCTUATION.toInt() ||
                Character.getType(ch) == Character.OTHER_PUNCTUATION.toInt()
    }
}
