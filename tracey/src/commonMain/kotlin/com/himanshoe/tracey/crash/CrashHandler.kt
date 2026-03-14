package com.himanshoe.tracey.crash

import com.himanshoe.tracey.model.ReplayPayload
import com.himanshoe.tracey.platform.currentEpochMillis
import com.himanshoe.tracey.platform.deleteFromDisk
import com.himanshoe.tracey.platform.installUncaughtExceptionHandler
import com.himanshoe.tracey.platform.loadFromDisk
import com.himanshoe.tracey.platform.platformLog
import com.himanshoe.tracey.platform.saveToDisk
import com.himanshoe.tracey.reporter.ReporterDispatcher
import kotlin.concurrent.Volatile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Installs a platform-level uncaught-exception handler and manages the
 * crash-replay lifecycle across app sessions.
 *
 * **On crash (current session)**
 *
 * When the process is about to terminate due to an unhandled exception,
 * [CrashHandler] synchronously writes the current [ReplayPayload] to durable
 * storage via [saveToDisk]. Because the process is dying, no async work or
 * network calls are attempted — only a fast disk write. The previous uncaught
 * exception handler (Crashlytics, Sentry, etc.) is then called so those SDKs
 * continue to function normally.
 *
 * **On next launch**
 *
 * [checkAndDispatchPendingReplay] is called during [com.himanshoe.tracey.Tracey.install].
 * If a saved crash replay exists on disk it is read, deserialized, dispatched
 * to all registered reporters via [ReporterDispatcher.dispatch], and then
 * deleted so it is only delivered once.
 *
 * This two-phase approach (save-on-crash, dispatch-on-next-launch) ensures the
 * app is fully initialized — and reporters are registered — before any payload
 * is delivered. It mirrors the approach used by Crashlytics and Sentry.
 *
 * @param dispatcher Dispatcher used for async operations (payload dispatch on next launch).
 */
internal class CrashHandler(
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val scope = CoroutineScope(dispatcher)

    /**
     * Guards against re-entrant crash handling. If saving the replay itself throws
     * an uncaught exception, the handler would be invoked again, causing a stack
     * overflow. This flag ensures the handler body runs at most once.
     */
    @Volatile
    private var handlerInvoked = false

    companion object {
        private const val CRASH_REPLAY_KEY = "tracey_crash_replay"
        private const val TAG = "Tracey"
    }

    /**
     * Installs the uncaught-exception handler. [payloadProvider] is a lambda
     * that synchronously assembles the current [ReplayPayload] — it must be
     * fast since it runs on the crashing thread.
     */
    fun install(payloadProvider: (crashReason: String) -> ReplayPayload) {
        installUncaughtExceptionHandler { throwable ->
            if (handlerInvoked) return@installUncaughtExceptionHandler
            handlerInvoked = true
            runCatching {
                val reason = "${throwable::class.simpleName}: ${throwable.message}"
                val payload = payloadProvider(reason)
                saveToDisk(CRASH_REPLAY_KEY, payload.toJson())
            }.onFailure { error ->
                platformLog(TAG, "Failed to save crash replay: ${error.message}")
            }
        }
    }

    /**
     * Checks for a crash replay saved by a previous session. If found, delivers
     * it to [dispatcher] via [reporterDispatcher] and removes it from disk.
     * Safe to call multiple times — only acts when a pending replay exists.
     */
    fun checkAndDispatchPendingReplay(reporterDispatcher: ReporterDispatcher) {
        val json = loadFromDisk(CRASH_REPLAY_KEY) ?: return
        deleteFromDisk(CRASH_REPLAY_KEY)

        val payload = ReplayPayload.fromJson(json) ?: run {
            platformLog(TAG, "Failed to parse saved crash replay — discarding.")
            return
        }

        scope.launch {
            runCatching { reporterDispatcher.dispatch(payload) }
                .onFailure { error ->
                    platformLog(TAG, "Failed to dispatch crash replay: ${error.message}")
                }
        }
    }
}
