package com.memora.app.data.security

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.Process
import java.io.File
import java.io.RandomAccessFile

/**
 * Debug-only helper that arms conversion at a journal phase in a secondary process,
 * then stays alive briefly so instrumentation can induce a real `am crash` / kill.
 * Excluded from release by the debug source set.
 */
class ConversionLiveDeathWorkerService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val phaseName = intent?.getStringExtra(EXTRA_PHASE)
        if (phaseName.isNullOrBlank()) {
            ConversionLiveDeathMarker.writeFailed(applicationContext, "Missing phase extra.")
            stopSelf()
            return START_NOT_STICKY
        }
        val phase = runCatching {
            DatabaseEncryptionConversionPhase.valueOf(phaseName)
        }.getOrElse {
            ConversionLiveDeathMarker.writeFailed(applicationContext, "Unknown phase: $phaseName")
            stopSelf()
            return START_NOT_STICKY
        }

        Thread(
            {
                try {
                    MemoraEncryptedDatabaseOpener.prepareConversionStoppingAfterPhaseForTest(
                        applicationContext,
                        phase,
                    )
                    ConversionLiveDeathMarker.writeArmed(
                        context = applicationContext,
                        phase = phase,
                        pid = Process.myPid(),
                    )
                    // Hold alive so the instrumentation process can issue am crash / kill.
                    Thread.sleep(CRASH_WINDOW_MILLIS)
                    stopSelf()
                } catch (error: Exception) {
                    ConversionLiveDeathMarker.writeFailed(
                        applicationContext,
                        error.message ?: error.javaClass.simpleName,
                    )
                    stopSelf()
                }
            },
            "memora-conversion-live-death-worker",
        ).start()
        return START_NOT_STICKY
    }

    companion object {
        const val EXTRA_PHASE = "phase"
        private const val CRASH_WINDOW_MILLIS = 12_000L

        fun intent(context: Context, phase: DatabaseEncryptionConversionPhase): Intent =
            Intent(context, ConversionLiveDeathWorkerService::class.java).putExtra(
                EXTRA_PHASE,
                phase.name,
            )
    }
}

/**
 * Cross-process marker under no-backup storage so the instrumentation process can
 * discover the secondary conversion worker pid and outcome.
 */
object ConversionLiveDeathMarker {
    private const val FILE_NAME = "conversion_live_death.marker"
    private const val ARMED = "ARMED"
    private const val FAILED = "FAILED"

    fun clear(context: Context) {
        markerFile(context).delete()
    }

    fun writeArmed(
        context: Context,
        phase: DatabaseEncryptionConversionPhase,
        pid: Int,
    ) {
        val file = markerFile(context)
        file.parentFile?.mkdirs()
        val payload = "$ARMED|${phase.name}|$pid\n"
        RandomAccessFile(file, "rw").use { raf ->
            raf.setLength(0)
            raf.write(payload.toByteArray(Charsets.UTF_8))
            raf.fd.sync()
        }
    }

    fun writeFailed(context: Context, message: String) {
        val file = markerFile(context)
        file.parentFile?.mkdirs()
        val payload = "$FAILED|$message\n"
        RandomAccessFile(file, "rw").use { raf ->
            raf.setLength(0)
            raf.write(payload.toByteArray(Charsets.UTF_8))
            raf.fd.sync()
        }
    }

    fun readArmed(context: Context): Armed? {
        val line = markerFile(context).takeIf { it.exists() }?.readText(Charsets.UTF_8)?.trim()
            ?: return null
        val parts = line.split('|')
        if (parts.size < 3 || parts[0] != ARMED) {
            return null
        }
        val phase = runCatching {
            DatabaseEncryptionConversionPhase.valueOf(parts[1])
        }.getOrNull() ?: return null
        val pid = parts[2].toIntOrNull() ?: return null
        return Armed(phase = phase, pid = pid)
    }

    fun readFailed(context: Context): String? {
        val line = markerFile(context).takeIf { it.exists() }?.readText(Charsets.UTF_8)?.trim()
            ?: return null
        val parts = line.split('|', limit = 2)
        if (parts.size < 2 || parts[0] != FAILED) {
            return null
        }
        return parts[1]
    }

    private fun markerFile(context: Context): File =
        File(context.applicationContext.noBackupFilesDir, FILE_NAME)

    data class Armed(
        val phase: DatabaseEncryptionConversionPhase,
        val pid: Int,
    )
}
