package com.himanshoe.tracey.overlay

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import com.himanshoe.tracey.model.InteractionEvent

/**
 * Holds all mutable state consumed by [TraceyOverlay].
 *
 * Designed as a `@Stable` class so Compose only recomposes the parts of the
 * overlay that observe the specific state that changed.
 *
 * State is driven by the recording engine — events are pushed here via
 * [addEvent] immediately after being written to the ring buffer, giving the
 * overlay a live feed without a separate polling mechanism.
 */
@Stable
internal class OverlayState {

    /** Live feed of interaction events shown in the floating log panel. */
    val liveEvents = mutableStateListOf<InteractionEvent>()

    /** Trail points used to draw gesture curves on the canvas. */
    val gestureTrails = mutableStateListOf<GestureTrail>()

    /** Whether the floating log panel is visible. */
    var isPanelVisible by mutableStateOf(false)

    /** Whether the overlay canvas (trails + ripples) is visible. */
    var isCanvasVisible by mutableStateOf(true)

    /** Current position of the draggable capture button. */
    var captureButtonOffset by mutableStateOf(Offset(48f, 200f))

    /** True while a manual capture is in progress to show a spinner. */
    var isCapturing by mutableStateOf(false)

    private val maxLiveEvents = 50

    /**
     * Pushes [event] into the live log and updates gesture trail state.
     * Called from the recording engine on the main thread.
     */
    fun addEvent(event: InteractionEvent) {
        liveEvents.add(event)
        if (liveEvents.size > maxLiveEvents) liveEvents.removeAt(0)
        addTrailPoint(event)
    }

    private fun addTrailPoint(event: InteractionEvent) {
        val trailPoint = when (event) {
            is InteractionEvent.Click -> TrailPoint(
                offset = Offset(event.x, event.y),
                type = EventType.Click,
                timestampMs = event.timestampMs,
            )
            is InteractionEvent.LongPress -> TrailPoint(
                offset = Offset(event.x, event.y),
                type = EventType.LongPress,
                timestampMs = event.timestampMs,
            )
            is InteractionEvent.Scroll -> TrailPoint(
                offset = Offset(event.x, event.y),
                type = EventType.Scroll,
                timestampMs = event.timestampMs,
            )
            is InteractionEvent.Swipe -> TrailPoint(
                offset = Offset(event.startX, event.startY),
                type = EventType.Swipe,
                timestampMs = event.timestampMs,
                endOffset = Offset(event.endX, event.endY),
            )
            is InteractionEvent.Pinch -> TrailPoint(
                offset = Offset(event.centroidX, event.centroidY),
                type = EventType.Pinch,
                timestampMs = event.timestampMs,
            )
            // Non-pointer events — appear in the log panel but leave no canvas trail.
            is InteractionEvent.AppForeground,
            is InteractionEvent.AppBackground,
            is InteractionEvent.ScreenView,
            is InteractionEvent.Breadcrumb,
            -> return
        }

        val last = gestureTrails.lastOrNull()
        if (last != null && last.type == trailPoint.type && !trailPoint.type.startsNewTrail) {
            last.points.add(trailPoint)
        } else {
            gestureTrails.add(GestureTrail(type = trailPoint.type, points = mutableListOf(trailPoint)))
        }

        if (gestureTrails.size > 100) gestureTrails.removeAt(0)
    }

    /**
     * Removes gesture trails whose newest point is older than [thresholdMs] milliseconds
     * from [nowMs]. Called periodically from the overlay to expire faded trails.
     */
    fun pruneExpiredTrails(nowMs: Long, thresholdMs: Long = 2_000L) {
        gestureTrails.removeAll { trail ->
            val newest = trail.points.lastOrNull()?.timestampMs ?: 0L
            (nowMs - newest) > thresholdMs
        }
    }
}

/**
 * A sequence of [TrailPoint]s that together form one continuous gesture trail
 * on the overlay canvas.
 */
@Stable
internal data class GestureTrail(
    val type: EventType,
    val points: MutableList<TrailPoint>,
)

/**
 * A single point in a [GestureTrail].
 *
 * @property offset     Position in the composable coordinate space.
 * @property endOffset  For swipes, the lift position. Null for all other types.
 * @property timestampMs Epoch millisecond when this point was recorded.
 */
@Stable
internal data class TrailPoint(
    val offset: Offset,
    val type: EventType,
    val timestampMs: Long,
    val endOffset: Offset? = null,
)

/** The gesture classification used for colour-coding and trail grouping. */
internal enum class EventType(val startsNewTrail: Boolean) {
    Click(startsNewTrail = true),
    LongPress(startsNewTrail = true),
    Scroll(startsNewTrail = false),
    Swipe(startsNewTrail = true),
    Pinch(startsNewTrail = true),
}
