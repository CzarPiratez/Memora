package com.memora.app.data.intelligence

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import com.memora.app.domain.intelligence.RecallRankDeviceSnapshot

/**
 * Platform adapter: RAM / ABI signals for [RecallRankDevicePolicy].
 */
object AndroidRecallRankDeviceSignals {
    fun snapshot(context: Context): RecallRankDeviceSnapshot {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return RecallRankDeviceSnapshot(
            totalRamBytes = totalRamBytes(activityManager),
            primaryAbi = Build.SUPPORTED_ABIS.firstOrNull(),
            isEmulator = isEmulator(),
        )
    }

    private fun totalRamBytes(activityManager: ActivityManager): Long? {
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return memoryInfo.totalMem.takeIf { it > 0L }
    }

    private fun isEmulator(): Boolean {
        val fingerprint = Build.FINGERPRINT.lowercase()
        val model = Build.MODEL.lowercase()
        val brand = Build.BRAND.lowercase()
        val device = Build.DEVICE.lowercase()
        val product = Build.PRODUCT.lowercase()
        return fingerprint.startsWith("generic") ||
            fingerprint.contains("vbox") ||
            fingerprint.contains("emulator") ||
            model.contains("emulator") ||
            model.contains("sdk_gphone") ||
            brand.startsWith("generic") ||
            device.startsWith("generic") ||
            product.contains("sdk") ||
            product.contains("emulator")
    }
}
