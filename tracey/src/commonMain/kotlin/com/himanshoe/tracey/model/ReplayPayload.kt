package com.himanshoe.tracey.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * The universal output of Tracey — a fully self-contained snapshot of
 * everything the user did in the 30 seconds before this payload was built.
 *
 * [ReplayPayload] is destination-agnostic. Every [com.himanshoe.tracey.reporter.TraceyReporter]
 * receives exactly this and decides what to do with it: attach it to Crashlytics,
 * POST it to a webhook, write it to a file, generate a Compose UI test, display
 * it in a debug panel, or send it to your own backend.
 *
 * The payload is fully serializable via `kotlinx.serialization` so `toJson()`
 * gives you a stable JSON string that any platform can parse.
 *
 * @property sessionId      Stable identifier for this recording session. Sourced
 *                          from [com.himanshoe.tracey.TraceyConfig.sessionIdProvider] so
 *                          teams can correlate it with their own analytics session.
 * @property appVersion     Version name of the host application.
 * @property capturedAtMs   Epoch millisecond when this payload was assembled.
 * @property durationMs     Time span from the oldest buffered event to capture time.
 * @property crashReason    Non-null only when the payload was auto-captured after an
 *                          uncaught exception. Contains the exception class and message.
 *                          Null for all manual [com.himanshoe.tracey.Tracey.capture] calls.
 * @property deviceInfo     Platform and hardware metadata at capture time.
 * @property events         Ordered list of every interaction recorded in the buffer,
 *                          oldest first.
 * @property timeline       Pre-formatted, human-readable interaction log ready to
 *                          paste into a bug report, Slack message, or GitHub issue.
 */
@Immutable
@Serializable
data class ReplayPayload(
    val sessionId: String,
    val appVersion: String,
    val capturedAtMs: Long,
    val durationMs: Long,
    val crashReason: String?,
    val deviceInfo: DeviceInfo,
    val events: List<InteractionEvent>,
    val timeline: String,
    /**
     * PNG-encoded screenshot captured at the moment this payload was assembled.
     * Excluded from JSON serialization — attach it to crash reports or upload it
     * separately via your [com.himanshoe.tracey.reporter.TraceyReporter].
     * Null when screenshot capture is unavailable on the current platform.
     */
    @Transient val screenshotPng: ByteArray? = null,
) {
    /**
     * Serializes this payload to a compact JSON string using
     * `kotlinx.serialization`. The resulting string is stable across SDK
     * versions as long as field names are preserved.
     */
    fun toJson(): String = traceyJson.encodeToString(this)

    /**
     * Returns true if this payload was auto-captured after an uncaught exception
     * rather than triggered manually via [com.himanshoe.tracey.Tracey.capture].
     */
    val isCrashPayload: Boolean get() = crashReason != null

    companion object {
        /**
         * Shared [Json] instance with lenient decoding so consumers on other
         * platforms can parse older payload versions that may have fewer fields.
         */
        val traceyJson: Json = Json {
            prettyPrint = false
            ignoreUnknownKeys = true
            encodeDefaults = true
            explicitNulls = false
        }

        /**
         * Deserializes a JSON string produced by [toJson] back into a [ReplayPayload].
         * Returns null if parsing fails so callers can handle stale or malformed data.
         */
        fun fromJson(json: String): ReplayPayload? = runCatching {
            traceyJson.decodeFromString<ReplayPayload>(json)
        }.getOrNull()
    }
}
