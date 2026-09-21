package com.memora.app.domain.intelligence

/**
 * Roles on a meaning cue (ADR-055 slice 1).
 *
 * Wrappers and TIME words are already gone via [MeaningRecallCue.contentTokens].
 * A trailing `for` is a **qualifier** only when content remains on both sides
 * (job `for` constraint). `looking for silky` has no head before `for`, so
 * the named word stays the head. Other constraint syntax is later slices.
 *
 * Qualifier tokens may promote a file that has them. They must not open a
 * one-word Find of their own. Embed / precision still read
 * [MeaningRecallCue.contentTokens] (D-10). Pool admission and page order
 * both read [MeaningRoleScorer].
 */
data class MeaningRecallRoles(
    val head: List<String>,
    val qualifier: List<String>,
) {
    val named: List<String> get() = head + qualifier

    fun isEmpty(): Boolean = head.isEmpty() && qualifier.isEmpty()

    companion object {
        private val FOR = Regex("""\bfor\b""")

        fun parse(rawQuery: String): MeaningRecallRoles {
            val content = MeaningRecallCue.contentTokens(rawQuery)
            if (content.isEmpty()) return MeaningRecallRoles(emptyList(), emptyList())
            val normalized = MeaningRecallCue.normalize(rawQuery)
            val matches = FOR.findAll(normalized).toList()
            for (match in matches.asReversed()) {
                val before = MeaningRecallCue.contentTokens(
                    normalized.substring(0, match.range.first),
                )
                val after = MeaningRecallCue.contentTokens(
                    normalized.substring(match.range.last + 1),
                )
                if (before.isNotEmpty() && after.isNotEmpty()) {
                    return MeaningRecallRoles(head = before, qualifier = after)
                }
            }
            return MeaningRecallRoles(head = content, qualifier = emptyList())
        }
    }
}
