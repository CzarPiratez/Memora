package com.memora.app.data.open

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The Activity currently on screen, held weakly.
 *
 * Starting another app's viewer from the *Activity* keeps it in UNFYND's task,
 * so Back returns the person to UNFYND with the preview still open. Starting it
 * from the application context needs `FLAG_ACTIVITY_NEW_TASK`, which can instead
 * surface the viewer's own existing task — showing a different document and
 * losing the way back. Never retains a finished Activity.
 */
@Singleton
class ForegroundActivityTracker @Inject constructor() :
    Application.ActivityLifecycleCallbacks {
    @Volatile
    private var resumed: WeakReference<Activity>? = null

    fun current(): Activity? = resumed?.get()?.takeIf { !it.isFinishing && !it.isDestroyed }

    override fun onActivityResumed(activity: Activity) {
        resumed = WeakReference(activity)
    }

    override fun onActivityPaused(activity: Activity) {
        if (resumed?.get() === activity) resumed = null
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (resumed?.get() === activity) resumed = null
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

    override fun onActivityStarted(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
}
