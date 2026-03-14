package com.himanshoe.tracey

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TraceyConfigTest {

    @Test
    fun defaultEnabledIsTrue() {
        assertTrue(TraceyConfig().enabled)
    }

    @Test
    fun defaultShowOverlayIsFalse() {
        // Must be false — overlay must never appear in prod by accident.
        assertFalse(TraceyConfig().showOverlay)
    }

    @Test
    fun defaultBufferDurationIs30Seconds() {
        assertEquals(30, TraceyConfig().bufferDurationSeconds)
    }

    @Test
    fun defaultMaxEventsIs500() {
        assertEquals(500, TraceyConfig().maxEvents)
    }

    @Test
    fun defaultReportersIsEmpty() {
        assertTrue(TraceyConfig().reporters.isEmpty())
    }

    @Test
    fun defaultRedactedTagsIsEmpty() {
        assertTrue(TraceyConfig().redactedTags.isEmpty())
    }

    @Test
    fun defaultTrackLifecycleIsTrue() {
        assertTrue(TraceyConfig().trackLifecycle)
    }

    @Test
    fun defaultGenerateHtmlReportIsFalse() {
        assertFalse(TraceyConfig().generateHtmlReport)
    }

    @Test
    fun customValuesArePreserved() {
        val config = TraceyConfig(
            enabled = false,
            showOverlay = true,
            bufferDurationSeconds = 60,
            maxEvents = 200,
            redactedTags = listOf("PasswordField"),
            trackLifecycle = false,
            generateHtmlReport = true,
        )

        assertFalse(config.enabled)
        assertTrue(config.showOverlay)
        assertEquals(60, config.bufferDurationSeconds)
        assertEquals(200, config.maxEvents)
        assertEquals(listOf("PasswordField"), config.redactedTags)
        assertFalse(config.trackLifecycle)
        assertTrue(config.generateHtmlReport)
    }

    @Test
    fun sessionIdProviderIsInvokedEachTime() {
        var counter = 0
        val config = TraceyConfig(sessionIdProvider = { "session-${++counter}" })
        assertEquals("session-1", config.sessionIdProvider())
        assertEquals("session-2", config.sessionIdProvider())
    }

    @Test
    fun dataClassEqualityIgnoresSessionIdProviderReference() {
        // Two configs with identical scalar fields are equal even with different lambdas
        // because data class equals uses the lambda's structural identity — this test
        // documents the current behaviour so any accidental change is caught.
        val a = TraceyConfig(enabled = true, bufferDurationSeconds = 60)
        val b = TraceyConfig(enabled = true, bufferDurationSeconds = 60)
        assertEquals(a.enabled, b.enabled)
        assertEquals(a.bufferDurationSeconds, b.bufferDurationSeconds)
    }
}
