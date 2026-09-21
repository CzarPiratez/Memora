package com.memora.app.domain.intelligence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MeaningRoleAdmissionTest {
    private val constrained = MeaningRoleScorer(
        MeaningRecallRoles(head = listOf("topic", "kind"), qualifier = listOf("place")),
    )
    private val unconstrained = MeaningRoleScorer(
        MeaningRecallRoles(head = listOf("topic", "kind"), qualifier = emptyList()),
    )

    @Test
    fun a_job_family_keeps_a_seat_when_the_cosine_majority_only_shares_a_constraint() {
        val majority = (0 until 4).map { index ->
            item("crowd-$index", head = setOf("kind"), qualifier = setOf("place"))
        }
        val gold = item("gold", head = setOf("topic"), qualifier = setOf("place"))
        val admitted = MeaningRoleAdmission.take(
            items = majority + gold,
            limit = 2,
            overlapOf = { it.overlap },
            scorer = constrained,
        )
        assertTrue(admitted.any { it.id == "gold" })
        assertEquals(2, admitted.size)
        assertEquals(listOf("crowd-0", "gold"), admitted.map { it.id })
    }

    @Test
    fun topic_plus_constraint_claims_the_scarce_seat_ahead_of_leftover_job_words() {
        val leftoverPlusConstraint = (0 until 4).map { index ->
            item("crowd-$index", head = setOf("kind"), qualifier = setOf("place"))
        }
        val gold = item(
            "gold",
            head = setOf("topic"),
            qualifier = setOf("place"),
        )
        val admitted = MeaningRoleAdmission.take(
            items = leftoverPlusConstraint + gold,
            limit = 1,
            overlapOf = { it.overlap },
            scorer = constrained,
        )
        assertEquals(listOf("gold"), admitted.map { it.id })
    }

    @Test
    fun cosine_neighbours_fill_leftover_seats_when_nothing_matches() {
        val neighbours = (0 until 4).map { index ->
            item("near-$index", head = emptySet(), qualifier = emptySet())
        }
        val admitted = MeaningRoleAdmission.take(
            items = neighbours,
            limit = 2,
            overlapOf = { it.overlap },
            scorer = unconstrained,
        )
        assertEquals(listOf("near-0", "near-1"), admitted.map { it.id })
    }

    private fun item(
        id: String,
        head: Set<String>,
        qualifier: Set<String>,
    ) = Item(id, MeaningRoleOverlap(head, qualifier))

    private data class Item(
        val id: String,
        val overlap: MeaningRoleOverlap,
    )
}
