package com.memora.app.data.mediastore

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.discovery.DiscoveryRequest
import com.memora.app.domain.discovery.DiscoveryResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises only the emulator's live MediaStore catalogue. It does not insert, edit,
 * delete, open, thumbnail, persist, or index any media item.
 */
@RunWith(AndroidJUnit4::class)
class MediaStoreImageDiscoverySourceTest {
    @Test
    fun reportsAnHonestBoundedOutcomeForCurrentMediaAccess() = runBlocking {
        val source = MediaStoreImageDiscoverySource(ApplicationProvider.getApplicationContext())

        when (val result = source.discover(DiscoveryRequest(batchSize = 2))) {
            is DiscoveryResult.Page -> {
                assertEquals(source.capability.sourceId, result.value.sourceId)
                assertTrue(result.value.assets.size <= 2)
                assertTrue(result.value.assets.all { asset ->
                    asset.type == AssetType.PHOTO || asset.type == AssetType.SCREENSHOT
                })
                assertEquals(source.capability.sourceId, result.value.checkpoint.sourceId)
            }

            DiscoveryResult.AccessRequired -> fail(
                "Grant photo access in Memora before running this MediaStore query test."
            )
            DiscoveryResult.AccessRevoked -> fail("The emulator revoked access during the MediaStore query.")
            is DiscoveryResult.Failed -> fail("Unexpected MediaStore query failure: ${result.failure.code}")
        }
    }
}
