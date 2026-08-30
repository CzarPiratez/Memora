package com.memora.app.domain.memory

import org.junit.Assert.assertEquals
import org.junit.Test

class RecallQueryConstraintClassifierTest {
    @Test
    fun blank_query_has_no_constraints() {
        val constraints = RecallQueryConstraintClassifier.classify("   ")

        assertEquals(RecallConstraintStrength.NONE, constraints.time)
        assertEquals(RecallConstraintStrength.NONE, constraints.topic)
    }

    @Test
    fun explicit_year_in_query_sets_explicit_time() {
        val constraints = RecallQueryConstraintClassifier.classify("notes in 2024")

        assertEquals(RecallConstraintStrength.EXPLICIT, constraints.time)
        assertEquals("2024", constraints.timeCue)
    }

    @Test
    fun advisory_recent_sets_advisory_time() {
        val constraints = RecallQueryConstraintClassifier.classify("recent screenshots")

        assertEquals(RecallConstraintStrength.ADVISORY, constraints.time)
        assertEquals("recent", constraints.timeCue)
    }

    @Test
    fun quoted_title_sets_explicit_topic() {
        val constraints = RecallQueryConstraintClassifier.classify("""file titled "March Invoice"""")

        assertEquals(RecallConstraintStrength.EXPLICIT, constraints.topic)
        assertEquals("march invoice", constraints.topicCue)
    }
}
