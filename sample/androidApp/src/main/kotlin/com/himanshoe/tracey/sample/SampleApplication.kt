package com.himanshoe.tracey.sample

import android.app.Application
import com.himanshoe.tracey.Tracey
import com.himanshoe.tracey.TraceyConfig
import com.himanshoe.tracey.reporter.builtin.LogcatReporter

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
