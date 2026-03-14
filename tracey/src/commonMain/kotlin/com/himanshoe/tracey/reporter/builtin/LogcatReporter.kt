package com.himanshoe.tracey.reporter.builtin

import com.himanshoe.tracey.model.InteractionEvent
import com.himanshoe.tracey.model.ReplayPayload
import com.himanshoe.tracey.platform.platformLog
import com.himanshoe.tracey.reporter.TraceyReporter
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * A built-in [TraceyReporter] that prints the interaction timeline to the
 * platform's native log output.
 *
 * On Android this writes to Logcat via `android.util.Log.d`.
 * On iOS it writes to stdout.
 *
 * Each event is printed with the local clock time at the moment it occurred,
 * e.g. `14:32:05.123  CLICK  HomeScreen > AddToCartButton`.
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
            if (payload.isCrashPayload) {
                appendLine("💥 ══ CRASH REPLAY FROM PREVIOUS SESSION ══ 💥")
                appendLine("💥 Crash    : ${payload.crashReason}")
                appendLine("💥 ════════════════════════════════════════ 💥")
            } else {
                appendLine("══ Tracey Replay ══════════════════════")
            }
            appendLine("Session  : ${payload.sessionId}")
            appendLine("App      : ${payload.appVersion}")
            appendLine("Device   : ${payload.deviceInfo.model} (${payload.deviceInfo.platform})")
            appendLine("OS       : ${payload.deviceInfo.osVersion}")
            appendLine("Duration : ${payload.durationMs}ms")
            appendLine("Events   : ${payload.events.size}")
            appendLine("══ Timeline (local time) ═════════════════")
        }
        platformLog(tag, header + buildLocalTimeline(payload))
    }

    private fun buildLocalTimeline(payload: ReplayPayload): String = buildString {
        payload.events.forEach { event ->
            val time = localTime(event.timestampMs)
            val line = when (event) {
                is InteractionEvent.Click ->
                    "$time  CLICK      ${event.path}"
                is InteractionEvent.LongPress ->
                    "$time  LONG PRESS ${event.path} (${event.holdDurationMs}ms)"
                is InteractionEvent.Scroll ->
                    "$time  SCROLL     ${event.path} (Δx=${event.deltaX.fmt()}, Δy=${event.deltaY.fmt()})"
                is InteractionEvent.Swipe ->
                    "$time  SWIPE      ${event.path} (${event.startX.fmt()},${event.startY.fmt()} → ${event.endX.fmt()},${event.endY.fmt()})"
                is InteractionEvent.Pinch ->
                    "$time  PINCH      ${event.path} (zoom=${event.zoomDelta.fmt()}×)"
                is InteractionEvent.AppForeground ->
                    "$time  FOREGROUND App came to foreground"
                is InteractionEvent.AppBackground ->
                    "$time  BACKGROUND App went to background"
                is InteractionEvent.ScreenView ->
                    "$time  SCREEN     ${event.screenName}"
                is InteractionEvent.Breadcrumb ->
                    "$time  LOG        ${event.message}"
            }
            appendLine(line)
        }
        if (payload.isCrashPayload) {
            appendLine("${localTime(payload.capturedAtMs)}  💥 CRASH   ${payload.crashReason}")
        }
    }

    private fun localTime(epochMs: Long): String {
        val local = Instant.fromEpochMilliseconds(epochMs)
            .toLocalDateTime(TimeZone.currentSystemDefault())
        val h  = local.hour.toString().padStart(2, '0')
        val m  = local.minute.toString().padStart(2, '0')
        val s  = local.second.toString().padStart(2, '0')
        val ms = (local.nanosecond / 1_000_000).toString().padStart(3, '0')
        return "$h:$m:$s.$ms"
    }

    private fun Float.fmt(): String {
        val intPart  = toInt()
        val fracPart = (kotlin.math.abs(this) * 10).toInt() % 10
        return "$intPart.$fracPart"
    }
}
