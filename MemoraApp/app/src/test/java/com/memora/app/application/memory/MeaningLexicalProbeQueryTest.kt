package com.memora.app.application.memory

import org.junit.Assert.assertEquals
import org.junit.Test

class MeaningLexicalProbeQueryTest {
    @Test
    fun a_job_cue_probes_the_topic_and_constraint_not_leftover_heads() {
        assertEquals(
            "swimming grade 2",
            MeaningLexicalProbeQuery.of("when are the swimming classes for grade 2"),
        )
        assertEquals(
            "swimming",
            MeaningLexicalProbeQuery.of("when are the swimming classes"),
        )
        assertEquals(
            "receipts hotel",
            MeaningLexicalProbeQuery.of("receipts for hotel"),
        )
    }

    @Test
    fun a_list_cue_ands_every_named_word() {
        assertEquals("scan silky", MeaningLexicalProbeQuery.of("scan silky"))
        assertEquals("wifi password", MeaningLexicalProbeQuery.of("wifi password"))
        assertEquals("passport", MeaningLexicalProbeQuery.of("passport"))
    }

    @Test
    fun wrappers_only_are_not_a_probe() {
        assertEquals("", MeaningLexicalProbeQuery.of("show me the files"))
    }
}
