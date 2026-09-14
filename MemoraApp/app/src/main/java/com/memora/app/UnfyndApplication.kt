package com.memora.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.memora.app.data.open.ForegroundActivityTracker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class UnfyndApplication : Application(), Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    /** Lets Open in another app start the viewer inside UNFYND's task. */
    @Inject
    lateinit var foregroundActivityTracker: ForegroundActivityTracker

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(foregroundActivityTracker)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
