package com.himanshoe.tracey.buffer

import com.himanshoe.tracey.model.InteractionEvent
import kotlin.concurrent.Volatile
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A thread-safe, memory-bounded circular buffer that holds the most recent
 * [InteractionEvent] records for the Tracey recording engine.
 *
 * The buffer enforces two independent limits — whichever is hit first causes
 * the oldest event to be evicted:
 * - **Time window**: events older than [maxDurationMs] milliseconds are pruned.
 * - **Event count**: the buffer never exceeds [maxEvents] entries.
 *
 * All mutations and reads are serialised through a [Mutex] so the buffer is
 * safe to write from the main thread (Compose frame callbacks) and read from
 * any coroutine dispatcher simultaneously.
 *
 * @param maxDurationMs Maximum age of events in milliseconds. Defaults to 30 s.
 * @param maxEvents     Hard cap on the number of stored events. Defaults to 500.
 */
internal class RingBuffer(
    maxDurationMs: Long = 30_000L,
    maxEvents: Int = 500,
) {
    @Volatile private var maxDurationMs: Long = maxDurationMs
    @Volatile private var maxEvents: Int = maxEvents

    private val mutex = Mutex()
    private val buffer = ArrayDeque<InteractionEvent>(maxEvents)

    /**
     * Reconfigures the buffer limits. Takes effect on the next [add] call.
     * Existing events are not evicted immediately — pruning happens lazily.
     *
     * Safe to call from [com.himanshoe.tracey.Tracey.install] before any
     * recording starts.
     */
    internal fun configure(maxDurationMs: Long, maxEvents: Int) {
        this.maxDurationMs = maxDurationMs
        this.maxEvents = maxEvents
    }

    /**
     * Appends [event] to the buffer and prunes any events that now violate the
     * time or count limits. The pruning step uses [event]'s own timestamp as
     * the "now" reference so the buffer stays consistent even if the system
     * clock drifts between calls.
     */
    suspend fun add(event: InteractionEvent): Unit = mutex.withLock {
        buffer.addLast(event)
        prune(nowMs = event.timestampMs)
    }

    /**
     * Returns an immutable snapshot of all events currently in the buffer,
     * ordered oldest-first. Safe to call from any coroutine context.
     */
    suspend fun snapshot(): List<InteractionEvent> = mutex.withLock {
        buffer.toList()
    }

    /**
     * Removes all events from the buffer.
     * Typically called after a [com.himanshoe.tracey.model.ReplayPayload] has been
     * built and dispatched so the next session starts clean.
     */
    suspend fun clear(): Unit = mutex.withLock {
        buffer.clear()
    }

    /** Returns the current number of events without acquiring the mutex. Approximate. */
    val size: Int get() = buffer.size

    /**
     * Returns a best-effort snapshot without acquiring the mutex.
     *
     * **Only call this from a crash handler or other last-resort path** where the
     * process is about to terminate and a coroutine context is not available.
     * Under normal circumstances always prefer the suspending [snapshot].
     *
     * A concurrent [add] may produce a slightly inconsistent list (e.g. a single
     * event appearing twice or not at all), but for crash reporting this is an
     * acceptable trade-off over losing all event history.
     */
    fun snapshotUnsafe(): List<InteractionEvent> = buffer.toList()

    private fun prune(nowMs: Long) {
        val cutoffMs = nowMs - maxDurationMs
        while (buffer.isNotEmpty() && buffer.first().timestampMs < cutoffMs) {
            buffer.removeFirst()
        }
        while (buffer.size > maxEvents) {
            buffer.removeFirst()
        }
    }
}
