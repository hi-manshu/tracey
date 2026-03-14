package com.himanshoe.tracey.buffer

import com.himanshoe.tracey.click
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RingBufferTest {

    private fun buffer(maxDurationMs: Long = 60_000L, maxEvents: Int = 100) =
        RingBuffer(maxDurationMs = maxDurationMs, maxEvents = maxEvents)

    @Test
    fun addAndSnapshotReturnsEventsOldestFirst() = runTest {
        val buf = buffer()
        buf.add(click(timestampMs = 1_000L))
        buf.add(click(timestampMs = 2_000L))
        buf.add(click(timestampMs = 3_000L))

        val snapshot = buf.snapshot()

        assertEquals(3, snapshot.size)
        assertEquals(1_000L, snapshot[0].timestampMs)
        assertEquals(3_000L, snapshot[2].timestampMs)
    }

    @Test
    fun snapshotOnEmptyBufferReturnsEmptyList() = runTest {
        val snapshot = buffer().snapshot()
        assertTrue(snapshot.isEmpty())
    }

    @Test
    fun evictsByCountWhenMaxEventsExceeded() = runTest {
        val buf = buffer(maxEvents = 3)
        buf.add(click(timestampMs = 1_000L))
        buf.add(click(timestampMs = 2_000L))
        buf.add(click(timestampMs = 3_000L))
        buf.add(click(timestampMs = 4_000L))

        val snapshot = buf.snapshot()

        assertEquals(3, snapshot.size)
        assertEquals(2_000L, snapshot[0].timestampMs)
        assertEquals(4_000L, snapshot[2].timestampMs)
    }

    @Test
    fun evictsByTimeWhenMaxDurationExceeded() = runTest {
        val buf = buffer(maxDurationMs = 5_000L)
        buf.add(click(timestampMs = 1_000L))
        buf.add(click(timestampMs = 3_000L))
        buf.add(click(timestampMs = 10_000L))

        val snapshot = buf.snapshot()

        assertEquals(1, snapshot.size)
        assertEquals(10_000L, snapshot[0].timestampMs)
    }

    @Test
    fun clearEmptiesBuffer() = runTest {
        val buf = buffer()
        buf.add(click(timestampMs = 1_000L))
        buf.add(click(timestampMs = 2_000L))
        buf.clear()

        assertTrue(buf.snapshot().isEmpty())
    }

    @Test
    fun sizeReflectsCurrentEventCount() = runTest {
        val buf = buffer()
        assertEquals(0, buf.size)
        buf.add(click(timestampMs = 1_000L))
        buf.add(click(timestampMs = 2_000L))
        assertEquals(2, buf.size)
    }

    @Test
    fun configureUpdatesLimitsLazily() = runTest {
        val buf = buffer(maxEvents = 100)
        repeat(10) { buf.add(click(timestampMs = it.toLong() * 1_000L)) }

        buf.configure(maxDurationMs = 60_000L, maxEvents = 3)
        buf.add(click(timestampMs = 100_000L))

        val snapshot = buf.snapshot()
        assertEquals(3, snapshot.size)
    }

    @Test
    fun snapshotIsImmutableCopyNotLiveReference() = runTest {
        val buf = buffer()
        buf.add(click(timestampMs = 1_000L))

        val snapshot = buf.snapshot()
        buf.add(click(timestampMs = 2_000L))

        assertEquals(1, snapshot.size)
    }

    @Test
    fun bufferRetainsMostRecentEventsWhenCountLimitHit() = runTest {
        val buf = buffer(maxEvents = 2)
        buf.add(click(timestampMs = 1_000L, path = "First"))
        buf.add(click(timestampMs = 2_000L, path = "Second"))
        buf.add(click(timestampMs = 3_000L, path = "Third"))

        val snapshot = buf.snapshot()

        assertEquals(2, snapshot.size)
        assertEquals("Second", snapshot[0].path)
        assertEquals("Third", snapshot[1].path)
    }
}
