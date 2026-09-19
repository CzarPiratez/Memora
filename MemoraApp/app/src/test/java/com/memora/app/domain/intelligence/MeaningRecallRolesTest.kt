package com.memora.app.domain.intelligence

import org.junit.Assert.assertEquals
import org.junit.Test

class MeaningRecallRolesTest {
    @Test
    fun wrappers_do_not_change_the_head() {
        assertEquals(
            listOf("swimming", "classes"),
            MeaningRecallRoles.parse("when are the swimming classes").head,
        )
        assertEquals(
            emptyList<String>(),
            MeaningRecallRoles.parse("when are the swimming classes").qualifier,
        )
    }

    @Test
    fun trailing_for_is_a_qualifier_when_a_head_already_exists() {
        val roles = MeaningRecallRoles.parse("when are the swimming classes for grade 2")
        assertEquals(listOf("swimming", "classes"), roles.head)
        assertEquals(listOf("grade"), roles.qualifier)
    }

    @Test
    fun looking_for_a_word_keeps_that_word_as_the_head() {
        val roles = MeaningRecallRoles.parse("looking for silky")
        assertEquals(listOf("silky"), roles.head)
        assertEquals(emptyList<String>(), roles.qualifier)
    }

    @Test
    fun for_without_a_prior_head_is_the_whole_job() {
        val roles = MeaningRecallRoles.parse("get me the files for the training project")
        assertEquals(listOf("training", "project"), roles.head)
        assertEquals(emptyList<String>(), roles.qualifier)
    }

    @Test
    fun list_cues_keep_every_named_word_as_head() {
        assertEquals(listOf("scan", "silky"), MeaningRecallRoles.parse("scan silky").head)
        assertEquals(listOf("wifi", "password"), MeaningRecallRoles.parse("wifi password").head)
    }

    @Test
    fun a_time_word_is_not_a_head_or_a_qualifier() {
        val roles = MeaningRecallRoles.parse("recent files with silky")
        assertEquals(listOf("silky"), roles.head)
        assertEquals(emptyList<String>(), roles.qualifier)
    }
}
