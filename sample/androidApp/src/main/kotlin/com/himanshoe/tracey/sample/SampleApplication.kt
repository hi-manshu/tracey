package com.himanshoe.tracey.sample

import android.app.Application
import com.himanshoe.tracey.Tracey
import com.himanshoe.tracey.TraceyConfig
import com.himanshoe.tracey.reporter.builtin.LogcatReporter

/**
 * Sample Application that installs Tracey at startup.
 *
 * The SDK auto-captures the [android.content.Context] via [com.himanshoe.tracey.TraceyInitProvider]
 * so there is no need to pass a context here. Just call [Tracey.install].
 */
class SampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        Tracey.install(
            TraceyConfig(
                enabled = true,
                bufferDurationSeconds = 30,
                maxEvents = 500,
                reporters = listOf(
                    LogcatReporter(),
                    // Add your own TraceyReporter implementations here:
                    // MyCrashlyticsReporter(),
                    // MySlackReporter(webhookUrl = "https://..."),
                ),
            )
        )
    }
}
