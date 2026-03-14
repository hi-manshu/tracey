package com.himanshoe.tracey.recording

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.util.VelocityTracker
import com.himanshoe.tracey.buffer.RingBuffer
import com.himanshoe.tracey.model.InteractionEvent
import com.himanshoe.tracey.platform.currentEpochMillis
import kotlin.concurrent.Volatile
import kotlin.math.sqrt
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Core recording engine for Tracey.
 *
 * [RecordingEngine] attaches a single `Modifier.pointerInput` at the root
 * composable level (via [TraceyHost][com.himanshoe.tracey.TraceyHost]) and classifies
 * every pointer gesture into the appropriate [InteractionEvent] subtype before
 * enqueuing it into the [RingBuffer].
 *
 * Design constraints met by this implementation:
 * - **Zero per-composable instrumentation** — one modifier at the root intercepts
 *   everything that reaches the composition.
 * - **Sub-half-millisecond overhead** — the modifier itself only enqueues a
 *   coroutine launch; no blocking work happens on the frame thread.
 * - **Thread safety** — all writes go through the [RingBuffer]'s internal [Mutex].
 *
 * @param buffer        The ring buffer that stores recorded events.
 * @param pathResolver  Resolves pointer coordinates to composable path strings.
 * @param dispatcher    Dispatcher used for buffer writes. Defaults to [Dispatchers.Default].
 */
@Stable
internal class RecordingEngine(
    private val buffer: RingBuffer,
    private val pathResolver: SemanticPathResolver,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val scope = CoroutineScope(dispatcher)
    private val mainScope = CoroutineScope(Dispatchers.Main.immediate)

    /** Set to false to suppress recording without uninstalling the modifier. */
    @Volatile
    var enabled: Boolean = true

    /**
     * Optional listener called on the main thread immediately after each event
     * is written to the buffer. Used by [TraceyOverlay] for live event display.
     */
    @Volatile
    var onEventRecorded: ((InteractionEvent) -> Unit)? = null

    /**
     * Composable paths that contain any of these tags are silently dropped.
     * Mirrors [com.himanshoe.tracey.TraceyConfig.redactedTags].
     */
    @Volatile
    var redactedTags: List<String> = emptyList()

    /**
     * Returns a [Modifier] that intercepts all pointer events on the composable
     * it is applied to. Attach this to the outermost composable in the tree.
     *
     * The modifier consumes no events — it observes passively so normal click
     * handlers, scroll handlers, etc. continue to work without modification.
     */
    fun modifier(): Modifier = Modifier.pointerInput(Unit) {
        awaitEachGesture {
            if (!enabled) return@awaitEachGesture

            val down = awaitFirstDown(requireUnconsumed = false)
            val downTime = currentEpochMillis()
            val downPosition = down.position
            val path = pathResolver.resolve(downPosition.x, downPosition.y)
            if (redactedTags.any { tag -> path.contains(tag) }) return@awaitEachGesture

            val velocityTracker = VelocityTracker()
            velocityTracker.addPosition(downTime, downPosition)

            var lastPosition = downPosition
            var totalMovement = Offset.Zero
            var isTwoFinger = false
            var zoomDelta = 1f
            var rotationDelta = 0f
            var centroid = downPosition

            do {
                val event = awaitPointerEvent()
                val eventTime = currentEpochMillis()

                if (event.changes.size > 1) isTwoFinger = true

                for (change in event.changes) {
                    if (change.positionChanged()) {
                        velocityTracker.addPosition(eventTime, change.position)
                    }
                }

                if (isTwoFinger) {
                    centroid = event.calculateCentroid()
                    zoomDelta *= event.calculateZoom()
                    rotationDelta += event.calculateRotation()
                    totalMovement += event.calculatePan()
                } else {
                    val currentChange = event.changes.firstOrNull() ?: continue
                    val delta = currentChange.position - lastPosition
                    totalMovement += delta

                    if (currentChange.positionChanged()) {
                        record(
                            InteractionEvent.Scroll(
                                timestampMs = eventTime,
                                path = path,
                                x = currentChange.position.x,
                                y = currentChange.position.y,
                                deltaX = delta.x,
                                deltaY = delta.y,
                                velocityX = 0f,
                                velocityY = 0f,
                            )
                        )
                        lastPosition = currentChange.position
                    }
                }

                if (event.type == PointerEventType.Release) break

            } while (event.changes.any { it.pressed })

            val velocity = velocityTracker.calculateVelocity()
            val totalDistance = sqrt(
                totalMovement.x * totalMovement.x + totalMovement.y * totalMovement.y
            )
            val holdDuration = currentEpochMillis() - downTime

            when {
                isTwoFinger -> record(
                    InteractionEvent.Pinch(
                        timestampMs = currentEpochMillis(),
                        path = path,
                        centroidX = centroid.x,
                        centroidY = centroid.y,
                        zoomDelta = zoomDelta,
                        rotationDelta = rotationDelta,
                    )
                )

                totalDistance > SWIPE_THRESHOLD -> record(
                    InteractionEvent.Swipe(
                        timestampMs = currentEpochMillis(),
                        path = path,
                        startX = downPosition.x,
                        startY = downPosition.y,
                        endX = lastPosition.x,
                        endY = lastPosition.y,
                        velocityX = velocity.x,
                        velocityY = velocity.y,
                    )
                )

                holdDuration > LONG_PRESS_THRESHOLD_MS && totalDistance < TAP_SLOP -> record(
                    InteractionEvent.LongPress(
                        timestampMs = currentEpochMillis(),
                        path = path,
                        x = downPosition.x,
                        y = downPosition.y,
                        holdDurationMs = holdDuration,
                    )
                )

                totalDistance < TAP_SLOP -> record(
                    InteractionEvent.Click(
                        timestampMs = downTime,
                        path = path,
                        x = downPosition.x,
                        y = downPosition.y,
                    )
                )
            }
        }
    }

    /**
     * Dispatches [event] directly into the ring buffer and notifies the overlay.
     *
     * Use this from [com.himanshoe.tracey.Tracey.log], [com.himanshoe.tracey.Tracey.screen],
     * and the lifecycle observer so all event sources share a single recording path.
     */
    internal fun record(event: InteractionEvent) {
        scope.launch { buffer.add(event) }
        onEventRecorded?.let { listener ->
            mainScope.launch { listener(event) }
        }
    }

    companion object {
        private const val SWIPE_THRESHOLD = 50f
        private const val TAP_SLOP = 12f
        private const val LONG_PRESS_THRESHOLD_MS = 500L
    }
}
