package com.himanshoe.tracey

import com.himanshoe.tracey.model.DeviceInfo
import com.himanshoe.tracey.model.InteractionEvent
import com.himanshoe.tracey.model.ReplayPayload

internal val fakeDevice = DeviceInfo(
    platform = "Android",
    model = "Pixel 8",
    osVersion = "14",
    screenWidthPx = 1080,
    screenHeightPx = 2400,
    density = 2.6f,
    appVersion = "1.0.0",
    appPackage = "com.example.app",
    locale = "en-US",
)

internal fun click(
    timestampMs: Long = 1_000L,
    path: String = "HomeScreen > Button",
    x: Float = 100f,
    y: Float = 200f,
) = InteractionEvent.Click(timestampMs = timestampMs, path = path, x = x, y = y)

internal fun longPress(
    timestampMs: Long = 1_000L,
    path: String = "HomeScreen > Card",
    x: Float = 100f,
    y: Float = 200f,
    holdDurationMs: Long = 600L,
) = InteractionEvent.LongPress(
    timestampMs = timestampMs, path = path, x = x, y = y, holdDurationMs = holdDurationMs,
)

internal fun scroll(
    timestampMs: Long = 1_000L,
    path: String = "HomeScreen > LazyColumn",
    deltaX: Float = 0f,
    deltaY: Float = -150f,
) = InteractionEvent.Scroll(
    timestampMs = timestampMs, path = path,
    x = 540f, y = 960f, deltaX = deltaX, deltaY = deltaY,
    velocityX = 0f, velocityY = -800f,
)

internal fun swipe(
    timestampMs: Long = 1_000L,
    path: String = "HomeScreen",
) = InteractionEvent.Swipe(
    timestampMs = timestampMs, path = path,
    startX = 100f, startY = 500f, endX = 800f, endY = 500f,
    velocityX = 1200f, velocityY = 0f,
)

internal fun pinch(
    timestampMs: Long = 1_000L,
    path: String = "MapScreen",
) = InteractionEvent.Pinch(
    timestampMs = timestampMs, path = path,
    centroidX = 540f, centroidY = 960f, zoomDelta = 1.5f, rotationDelta = 0f,
)

internal fun breadcrumb(
    timestampMs: Long = 1_000L,
    message: String = "Cart: added SKU-1234",
) = InteractionEvent.Breadcrumb(timestampMs = timestampMs, message = message)

internal fun screenView(
    timestampMs: Long = 1_000L,
    screenName: String = "HomeScreen",
) = InteractionEvent.ScreenView(
    timestampMs = timestampMs, path = screenName, screenName = screenName,
)

internal fun appForeground(timestampMs: Long = 1_000L) =
    InteractionEvent.AppForeground(timestampMs = timestampMs)

internal fun appBackground(timestampMs: Long = 1_000L) =
    InteractionEvent.AppBackground(timestampMs = timestampMs)

internal fun fakePayload(
    sessionId: String = "test-session-123",
    events: List<InteractionEvent> = emptyList(),
    crashReason: String? = null,
    timeline: String = "",
    capturedAtMs: Long = 10_000L,
    durationMs: Long = 5_000L,
) = ReplayPayload(
    sessionId = sessionId,
    appVersion = "1.0.0",
    capturedAtMs = capturedAtMs,
    durationMs = durationMs,
    crashReason = crashReason,
    deviceInfo = fakeDevice,
    events = events,
    timeline = timeline,
)
