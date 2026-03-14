package com.himanshoe.tracey.reporter

import com.himanshoe.tracey.model.ReplayPayload
import com.himanshoe.tracey.platform.platformLog
import kotlin.concurrent.Volatile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Fans a [ReplayPayload] out to every registered [TraceyReporter] concurrently.
 *
 * Each reporter runs in its own supervised coroutine so a failure in one reporter
 * never prevents the others from receiving the payload. Errors are swallowed and
 * logged via [platformLog] rather than propagated, keeping the host app stable.
 *
 * @param dispatcher The [CoroutineDispatcher] used to launch reporter coroutines.
 *                   Defaults to [Dispatchers.Default].
 */
internal class ReporterDispatcher(
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    @Volatile
    private var reporters: List<TraceyReporter> = emptyList()

    /**
     * Replaces the active reporter list. Safe to call after [dispatch] — the
     * new list takes effect on the next dispatch call.
     */
    fun setReporters(list: List<TraceyReporter>) {
        reporters = list
    }

    /**
     * Delivers [payload] to every registered reporter concurrently.
     * Each reporter runs in its own supervised coroutine.
     */
    suspend fun dispatch(payload: ReplayPayload) {
        val snapshot = reporters
        if (snapshot.isEmpty()) return

        snapshot.forEach { reporter ->
            scope.launch {
                runCatching { reporter.onReplayReady(payload) }
                    .onFailure { error ->
                        platformLog(
                            tag = "Tracey",
                            message = "Reporter ${reporter::class.simpleName} threw: ${error.message}",
                        )
                    }
            }
        }
    }
}
