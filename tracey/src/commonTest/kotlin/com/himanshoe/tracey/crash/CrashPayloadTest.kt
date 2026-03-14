package com.himanshoe.tracey.crash

import com.himanshoe.tracey.Tracey
import com.himanshoe.tracey.TraceyConfig
import com.himanshoe.tracey.breadcrumb
import com.himanshoe.tracey.click
import com.himanshoe.tracey.model.InteractionEvent
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Verifies that the crash handler path — [Tracey.buildPayload] with a non-null
 * [crashReason] — produces a payload that contains the full event history from
 * the ring buffer, not an empty list.
 *
 * [Tracey.buildPayload] is the suspend counterpart of the private
 * `buildPayloadBlocking`. Both share the same timeline builder and event
 * sourcing logic; the blocking variant additionally uses [RingBuffer.snapshotUnsafe].
 * We test [buildPayload] here (accessible as `internal`) and cover
 * [RingBuffer.snapshotUnsafe] separately in [com.himanshoe.tracey.buffer.RingBufferTest].
 */
class CrashPayloadTest {

    @AfterTest
    fun tearDown() = runTest { Tracey.resetForTest() }

    @Test
    fun crashPayloadCarriesCorrectCrashReason() = runTest {
        Tracey.install(TraceyConfig(enabled = true, trackLifecycle = false))

        val payload = Tracey.buildPayload(crashReason = "IllegalStateException: queue is null")

        assertEquals("IllegalStateException: queue is null", payload.crashReason)
        assertTrue(payload.isCrashPayload)
    }

    @Test
    fun crashPayloadIncludesEventsFromBuffer() = runTest {
        Tracey.install(TraceyConfig(enabled = true, trackLifecycle = false))

        // Write directly to the buffer so there's no async lag
        Tracey.buffer.add(click(timestampMs = 1_000L, path = "HomeScreen > Button"))
        Tracey.buffer.add(breadcrumb(timestampMs = 2_000L, message = "api call started"))
        Tracey.buffer.add(click(timestampMs = 3_000L, path = "HomeScreen > Submit"))

        val payload = Tracey.buildPayload(crashReason = "NPE: boom")

        assertEquals(3, payload.events.size)
        assertEquals("HomeScreen > Button", (payload.events[0] as InteractionEvent.Click).path)
        assertEquals("api call started", (payload.events[1] as InteractionEvent.Breadcrumb).message)
    }

    @Test
    fun crashPayloadTimelineEndsWithCrashLine() = runTest {
        Tracey.install(TraceyConfig(enabled = true, trackLifecycle = false))
        Tracey.buffer.add(click(timestampMs = 1_000L, path = "HomeScreen"))

        val payload = Tracey.buildPayload(crashReason = "OutOfMemoryError: heap")

        assertTrue(
            payload.timeline.contains("CRASH"),
            "Timeline should contain CRASH marker",
        )
        assertTrue(
            payload.timeline.contains("OutOfMemoryError: heap"),
            "Timeline should include crash reason",
        )
    }

    @Test
    fun crashPayloadTimelineContainsEventsBeforeCrash() = runTest {
        Tracey.install(TraceyConfig(enabled = true, trackLifecycle = false))
        Tracey.buffer.add(click(timestampMs = 1_000L, path = "CheckoutScreen > PlaceOrder"))
        Tracey.buffer.add(breadcrumb(timestampMs = 2_000L, message = "order submitted"))

        val payload = Tracey.buildPayload(crashReason = "SocketTimeoutException")

        assertTrue(payload.timeline.contains("CLK"))
        assertTrue(payload.timeline.contains("CheckoutScreen > PlaceOrder"))
        assertTrue(payload.timeline.contains("order submitted"))
    }

    @Test
    fun crashPayloadWithEmptyBufferHasEmptyEvents() = runTest {
        Tracey.install(TraceyConfig(enabled = true, trackLifecycle = false))

        val payload = Tracey.buildPayload(crashReason = "NullPointerException")

        assertTrue(payload.events.isEmpty())
        assertNotNull(payload.crashReason)
    }

    @Test
    fun crashPayloadDurationSpansFirstEventToNow() = runTest {
        Tracey.install(TraceyConfig(enabled = true, trackLifecycle = false))
        Tracey.buffer.add(click(timestampMs = 1_000L))
        Tracey.buffer.add(click(timestampMs = 5_000L))

        val payload = Tracey.buildPayload(crashReason = "crash")

        // durationMs = capturedAtMs - firstEvent.timestampMs
        // capturedAtMs ≥ 5_000, firstEvent is at 1_000 → duration ≥ 4_000
        assertTrue(payload.durationMs >= 4_000L)
    }
}
