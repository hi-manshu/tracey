package com.himanshoe.tracey.model

import com.himanshoe.tracey.fakeDevice
import com.himanshoe.tracey.fakePayload
import com.himanshoe.tracey.click
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReplayPayloadTest {

    @Test
    fun isCrashPayloadFalseWhenCrashReasonIsNull() {
        assertFalse(fakePayload(crashReason = null).isCrashPayload)
    }

    @Test
    fun isCrashPayloadTrueWhenCrashReasonIsSet() {
        assertTrue(fakePayload(crashReason = "NullPointerException").isCrashPayload)
    }

    @Test
    fun toJsonProducesValidJsonString() {
        val json = fakePayload(sessionId = "abc-123").toJson()
        assertTrue(json.contains("abc-123"))
        assertTrue(json.startsWith("{"))
        assertTrue(json.endsWith("}"))
    }

    @Test
    fun fromJsonRoundtripsSessionId() {
        val original = fakePayload(sessionId = "round-trip-session")
        val json = original.toJson()
        val restored = ReplayPayload.fromJson(json)

        assertNotNull(restored)
        assertEquals("round-trip-session", restored.sessionId)
    }

    @Test
    fun fromJsonRoundtripsAllScalarFields() {
        val original = fakePayload(
            sessionId = "sess-42",
            capturedAtMs = 1_700_000_000_000L,
            durationMs = 18_000L,
            crashReason = "IllegalStateException",
        )
        val restored = ReplayPayload.fromJson(original.toJson())

        assertNotNull(restored)
        assertEquals(original.sessionId, restored.sessionId)
        assertEquals(original.capturedAtMs, restored.capturedAtMs)
        assertEquals(original.durationMs, restored.durationMs)
        assertEquals(original.crashReason, restored.crashReason)
        assertTrue(restored.isCrashPayload)
    }

    @Test
    fun fromJsonRoundtripsEvents() {
        val events = listOf(click(timestampMs = 5_000L, path = "HomeScreen > Button"))
        val original = fakePayload(events = events)
        val restored = ReplayPayload.fromJson(original.toJson())

        assertNotNull(restored)
        assertEquals(1, restored.events.size)
        val restoredClick = restored.events[0] as? InteractionEvent.Click
        assertNotNull(restoredClick)
        assertEquals(5_000L, restoredClick.timestampMs)
        assertEquals("HomeScreen > Button", restoredClick.path)
    }

    @Test
    fun fromJsonReturnsNullOnMalformedInput() {
        assertNull(ReplayPayload.fromJson("not-json"))
        assertNull(ReplayPayload.fromJson("{}"))
        assertNull(ReplayPayload.fromJson(""))
    }

    @Test
    fun screenshotPngExcludedFromJson() {
        val payload = ReplayPayload(
            sessionId = "s1",
            appVersion = "1.0",
            capturedAtMs = 1_000L,
            durationMs = 500L,
            crashReason = null,
            deviceInfo = fakeDevice,
            events = emptyList(),
            timeline = "",
            screenshotPng = byteArrayOf(1, 2, 3),
        )
        val json = payload.toJson()
        assertFalse(json.contains("screenshotPng"))

        val restored = ReplayPayload.fromJson(json)
        assertNotNull(restored)
        assertNull(restored.screenshotPng)
    }

    @Test
    fun fromJsonIgnoresUnknownKeys() {
        val json = """{"sessionId":"s1","appVersion":"1.0","capturedAtMs":1000,
            |"durationMs":500,"crashReason":null,"deviceInfo":{"platform":"Android",
            |"model":"Pixel","osVersion":"14","screenWidthPx":1080,"screenHeightPx":2400,
            |"density":2.6,"appVersion":"1.0","appPackage":"com.example","locale":"en-US"},
            |"events":[],"timeline":"","unknownFutureField":"value"}""".trimMargin()

        val payload = ReplayPayload.fromJson(json)
        assertNotNull(payload)
        assertEquals("s1", payload.sessionId)
    }
}
