package com.himanshoe.tracey.sample

import android.app.Application
import com.himanshoe.tracey.Tracey
import com.himanshoe.tracey.TraceyConfig
import com.himanshoe.tracey.reporter.builtin.LogcatReporter

/**
 * Application-only Tracey setup — no TraceyHost composable needed.
 *
 * Installing here instead of inside TraceyHost gives two advantages:
 *  1. Crash handler is active before the first composition, so crashes during
 *     early startup (DI graphs, database migrations, etc.) are also captured.
 *  2. The crash replay from the previous session is dispatched to reporters
 *     as early as possible, before any UI is rendered.
 *
 * Trade-off: gesture recording (clicks, scrolls, swipes, pinches) is NOT
 * available without TraceyHost. Only these events are captured:
 *  - AppForeground / AppBackground  (via ActivityLifecycleCallbacks)
 *  - ScreenView                     (via Tracey.screen / Tracey.route)
 *  - Breadcrumb                     (via Tracey.log)
 *  - Crash                          (via uncaught exception handler)
 */
class SampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        Tracey.install(
            TraceyConfig(
                enabled = true,
                bufferDurationSeconds = 30,
                maxEvents = 500,
                trackLifecycle = true,
                reporters = listOf(LogcatReporter()),
            )
        )
    }
}
