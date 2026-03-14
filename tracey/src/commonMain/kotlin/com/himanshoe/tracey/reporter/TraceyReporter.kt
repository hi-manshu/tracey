package com.himanshoe.tracey.reporter

import com.himanshoe.tracey.model.ReplayPayload

/**
 * The single integration point between Tracey and any destination that
 * consumes recorded interaction data.
 *
 * Implement this interface to route [ReplayPayload] to any system — a crash
 * reporter, an analytics platform, a Slack webhook, a local file, your own
 * backend, a debug overlay, or anything else.
 *
 * **Lifecycle**
 *
 * `onReplayReady` is called in two situations:
 * 1. **Manual capture** — the developer calls [com.himanshoe.tracey.Tracey.capture],
 *    [com.himanshoe.tracey.Tracey.captureAndExportTest], or taps the overlay capture
 *    button during a QA session.
 * 2. **Post-crash replay** — Tracey saved a payload to disk when the app
 *    crashed. On the next cold launch, after reporters are registered,
 *    `onReplayReady` is called automatically with the recovered payload.
 *    `payload.isCrashPayload` is `true` and `payload.crashReason` is non-null
 *    in this case.
 *
 * The reporter does not need to distinguish between these two cases unless it
 * wants to — the payload is identical in structure either way.
 *
 * **Threading**
 *
 * `onReplayReady` is called from a coroutine on [kotlinx.coroutines.Dispatchers.Default].
 * Implementations may switch to any dispatcher they need internally. The function
 * is `suspend` so reporters can perform async I/O (network, file, database) without
 * blocking.
 *
 * **Example — 10-line custom reporter**
 * ```kotlin
 * class MySlackReporter(private val webhookUrl: String) : TraceyReporter {
 *     override suspend fun onReplayReady(payload: ReplayPayload) {
 *         httpClient.post(webhookUrl) {
 *             setBody(payload.toJson())
 *             contentType(ContentType.Application.Json)
 *         }
 *     }
 * }
 * ```
 *
 * Register reporters via [com.himanshoe.tracey.TraceyConfig.reporters].
 */
interface TraceyReporter {

    /**
     * Called whenever a [ReplayPayload] is ready for consumption.
     *
     * Check [ReplayPayload.isCrashPayload] to know whether this was triggered
     * by a crash recovery or a manual capture.
     *
     * @param payload The complete, self-contained interaction replay.
     */
    suspend fun onReplayReady(payload: ReplayPayload)
}
