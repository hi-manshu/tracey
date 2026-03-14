package com.himanshoe.tracey

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import com.himanshoe.tracey.platform.generateUUID
import com.himanshoe.tracey.reporter.TraceyReporter

/**
 * Full configuration for Tracey — recording settings and UI preferences in one place.
 *
 * In Compose, prefer the [rememberTraceyConfig] helper over constructing this directly:
 *
 * ```kotlin
 * TraceyHost(
 *     traceyConfig = rememberTraceyConfig(
 *         showOverlay = BuildConfig.DEBUG,
 *         reporters   = listOf(LogcatReporter()),
 *     )
 * ) {
 *     MyApp()
 * }
 * ```
 *
 * For non-Compose entry points (e.g. `Application.onCreate`) pass this directly
 * to [Tracey.install]. In that context [showOverlay] has no effect.
 *
 * @property enabled Master on/off switch. Set to `BuildConfig.DEBUG` for zero prod overhead.
 * @property showOverlay Show the floating debug overlay (gesture trails + live event log).
 *   Defaults to `false`. Pass `BuildConfig.DEBUG` to show it in debug builds only.
 * @property bufferDurationSeconds Seconds of history the ring buffer retains. Defaults to `30`.
 * @property maxEvents Hard cap on stored event count. Defaults to `500`.
 * @property reporters [TraceyReporter] implementations that receive payloads on capture or crash.
 * @property redactedTags `testTag` values whose events are silently dropped (passwords, etc.).
 * @property trackLifecycle Auto-record foreground/background and screen transitions.
 * @property generateHtmlReport Auto-save an HTML report on every [Tracey.capture] call.
 * @property sessionIdProvider Lambda that produces the session ID. Defaults to a random UUID.
 */
@Immutable
data class TraceyConfig(
    val enabled: Boolean = true,
    val showOverlay: Boolean = false,
    val bufferDurationSeconds: Int = 30,
    val maxEvents: Int = 500,
    val reporters: List<TraceyReporter> = emptyList(),
    val redactedTags: List<String> = emptyList(),
    val trackLifecycle: Boolean = true,
    val generateHtmlReport: Boolean = false,
    val sessionIdProvider: () -> String = { generateUUID() },
)

/**
 * Creates and [remember]s a [TraceyConfig] across recompositions.
 *
 * This is the idiomatic Compose way to configure [TraceyHost]:
 *
 * ```kotlin
 * TraceyHost(
 *     traceyConfig = rememberTraceyConfig(
 *         showOverlay = BuildConfig.DEBUG,
 *         reporters   = listOf(LogcatReporter()),
 *         bufferDurationSeconds = 60,
 *     )
 * ) {
 *     MyApp()
 * }
 * ```
 *
 * The config is re-created only when one of the supplied parameters changes,
 * so [TraceyHost] never reinstalls unnecessarily.
 */
@Composable
fun rememberTraceyConfig(
    enabled: Boolean = true,
    showOverlay: Boolean = false,
    bufferDurationSeconds: Int = 30,
    maxEvents: Int = 500,
    reporters: List<TraceyReporter> = emptyList(),
    redactedTags: List<String> = emptyList(),
    trackLifecycle: Boolean = true,
    generateHtmlReport: Boolean = false,
    sessionIdProvider: () -> String = { generateUUID() },
): TraceyConfig = remember(
    enabled, showOverlay, bufferDurationSeconds, maxEvents,
    reporters, redactedTags, trackLifecycle, generateHtmlReport,
) {
    TraceyConfig(
        enabled = enabled,
        showOverlay = showOverlay,
        bufferDurationSeconds = bufferDurationSeconds,
        maxEvents = maxEvents,
        reporters = reporters,
        redactedTags = redactedTags,
        trackLifecycle = trackLifecycle,
        generateHtmlReport = generateHtmlReport,
        sessionIdProvider = sessionIdProvider,
    )
}
