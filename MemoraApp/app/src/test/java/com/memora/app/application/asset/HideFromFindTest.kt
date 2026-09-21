package com.memora.app.application.asset

import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.asset.FindHiddenItem
import com.memora.app.domain.asset.FindHiddenStore
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class HideFromFindTest {
    private val clock = Clock.fixed(Instant.parse("2026-09-21T16:00:00Z"), ZoneOffset.UTC)
    private val store = InMemoryFindHiddenStore()
    private val hide = HideFromFind(store, clock)
    private val showAgain = ShowAgainOnFind(store)
    private val load = LoadFindHidden(store)

    @Test
    fun hide_is_reversible_and_does_not_invent_a_second_row() = runTest {
        hide("photos", "42", "lake.jpg")
        hide("photos", "42", "lake.jpg")

        val hidden = load()
        assertEquals(1, hidden.size)
        assertEquals("lake.jpg", hidden.single().label)
        assertEquals(clock.instant(), hidden.single().hiddenAt)
        assertEquals(
            setOf(AssetIdentity(SourceId("photos"), SourceAssetKey("42"))),
            store.hiddenIdentities(
                listOf(
                    AssetIdentity(SourceId("photos"), SourceAssetKey("42")),
                    AssetIdentity(SourceId("photos"), SourceAssetKey("other")),
                ),
            ),
        )

        showAgain("photos", "42")
        assertTrue(load().isEmpty())
        assertTrue(
            store.hiddenIdentities(
                listOf(AssetIdentity(SourceId("photos"), SourceAssetKey("42"))),
            ).isEmpty(),
        )
    }
}

internal class InMemoryFindHiddenStore : FindHiddenStore {
    private val rows = linkedMapOf<AssetIdentity, FindHiddenItem>()

    override suspend fun hide(item: FindHiddenItem) {
        rows[item.asIdentity()] = item
    }

    override suspend fun showAgain(identity: AssetIdentity) {
        rows.remove(identity)
    }

    override suspend fun listAll(): List<FindHiddenItem> = rows.values.toList()

    override suspend fun hiddenIdentities(
        among: Collection<AssetIdentity>,
    ): Set<AssetIdentity> {
        val wanted = among.toSet()
        return rows.keys.filter { it in wanted }.toSet()
    }
}
