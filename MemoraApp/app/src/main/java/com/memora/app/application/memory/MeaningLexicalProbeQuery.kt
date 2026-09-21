package com.memora.app.application.memory

import com.memora.app.domain.intelligence.MeaningRecallRoles
import com.memora.app.domain.intelligence.MeaningRoleScorer

/**
 * Lexical probe for Meaning candidate fusion (ADR-055 slice 2).
 *
 * Keyword Find stays a generator. On a job cue the leftover head words
 * must not AND-veto reachability: the probe is the topic plus any
 * constraint. List cues still AND every named word.
 *
 * Vocabulary-free. Not a synonym. Not a second Find product.
 */
object MeaningLexicalProbeQuery {
    fun of(rawQuery: String): String {
        val roles = MeaningRecallRoles.parse(rawQuery)
        if (roles.isEmpty()) return ""
        val tokens = if (MeaningRoleScorer.forQuery(rawQuery).preferTopic) {
            listOfNotNull(roles.head.firstOrNull()) + roles.qualifier
        } else {
            roles.named
        }
        return tokens.joinToString(" ")
    }
}
