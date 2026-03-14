package com.himanshoe.tracey.reporter.builtin

import com.himanshoe.tracey.model.ReplayPayload
import com.himanshoe.tracey.platform.platformLog
import com.himanshoe.tracey.platform.saveToDisk
import com.himanshoe.tracey.reporter.TraceyReporter

/**
 * A built-in [TraceyReporter] that persists each [ReplayPayload] as a JSON
 * file in the platform's appropriate writable directory.
 *
 * | Platform | Location |
 * |----------|----------|
 * | Android  | `<filesDir>/tracey/<sessionId>.json` |
 * | iOS      | `<Documents>/tracey/<sessionId>.json` |
 * | Desktop  | `<userHome>/.tracey/<sessionId>.json` |
 * | WasmJs   | `localStorage` key `tracey_<sessionId>` |
 *
 * Each payload is stored under its own key derived from the session ID so
 * multiple sessions accumulate without overwriting each other.
 *
 * This reporter has zero external dependencies and is useful for:
 * - Attaching replays to bug reports by pulling them from the device.
 * - Feeding replays into a CI pipeline via `adb pull` or equivalent.
 * - Offline QA sessions where network access is unavailable.
 *
 * Usage:
 * ```kotlin
 * Tracey.install(
 *     TraceyConfig(reporters = listOf(FileReporter()))
 * )
 * ```
 */
class FileReporter : TraceyReporter {

    override suspend fun onReplayReady(payload: ReplayPayload) {
        val key = "tracey_replay_${payload.sessionId}"
        runCatching { saveToDisk(key, payload.toJson()) }
            .onFailure { error ->
                platformLog("Tracey", "FileReporter failed to write payload: ${error.message}")
            }
            .onSuccess {
                platformLog("Tracey", "FileReporter saved replay for session ${payload.sessionId}")
            }
    }
}
