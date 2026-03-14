package com.himanshoe.tracey.reporter.builtin

import com.himanshoe.tracey.model.ReplayPayload
import com.himanshoe.tracey.platform.platformLog
import com.himanshoe.tracey.reporter.TraceyReporter

/**
 * A built-in [TraceyReporter] that prints the interaction timeline to the
 * platform's native log output.
 *
 * On Android this writes to Logcat via `android.util.Log.d`.
 * On Desktop and iOS it writes to stdout.
 * On WasmJs it calls `console.log`.
 *
 * This reporter has zero external dependencies and is useful during development
 * and for verifying that Tracey is recording correctly before wiring up a
 * production destination.
 *
 * Usage:
 * ```kotlin
 * Tracey.install(
 *     TraceyConfig(reporters = listOf(LogcatReporter()))
 * )
 * ```
 *
 * @param tag The log tag. Defaults to `"Tracey"`.
 */
class LogcatReporter(private val tag: String = "Tracey") : TraceyReporter {

    override suspend fun onReplayReady(payload: ReplayPayload) {
        val header = buildString {
            appendLine("══ Tracey Replay ══════════════════════")
            appendLine("Session  : ${payload.sessionId}")
            appendLine("App      : ${payload.appVersion}")
            appendLine("Device   : ${payload.deviceInfo.model} (${payload.deviceInfo.platform})")
            appendLine("OS       : ${payload.deviceInfo.osVersion}")
            appendLine("Duration : ${payload.durationMs}ms")
            appendLine("Events   : ${payload.events.size}")
            if (payload.isCrashPayload) {
                appendLine("Crash    : ${payload.crashReason}")
            }
            appendLine("══ Timeline ══════════════════════════════")
        }
        platformLog(tag, header + payload.timeline)
    }
}
