package com.himanshoe.tracey.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.himanshoe.tracey.model.InteractionEvent
import com.himanshoe.tracey.overlay.GestureTrailPainter.drawTrails
import com.himanshoe.tracey.platform.currentEpochMillis
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * Full-screen debug overlay that renders gesture trails and a draggable control panel.
 *
 * Attach this composable above your app's root content inside [com.himanshoe.tracey.TraceyHost].
 * It is shown only when [com.himanshoe.tracey.TraceyConfig.showOverlay] is true (default: same
 * as `BuildConfig.DEBUG` equivalent per platform).
 *
 * Features:
 * - **Gesture trails** — bezier curves fade over 2 seconds, colour-coded by type.
 * - **Click ripples** — purple ripple at click coordinates.
 * - **Live event log** — scrollable panel showing the most recent interactions.
 * - **Capture button** — triggers [com.himanshoe.tracey.Tracey.capture] and fires all reporters.
 * - **Draggable** — the control button can be repositioned by dragging.
 *
 * @param state         The [OverlayState] owned by [com.himanshoe.tracey.TraceyHost].
 * @param onCapture     Called when the user taps the capture button.
 * @param onTogglePanel Called when the user taps the log-panel toggle.
 */
@Composable
internal fun TraceyOverlay(
    state: OverlayState,
    onCapture: () -> Unit,
    onTogglePanel: () -> Unit,
) {
    var nowMs by remember { mutableLongStateOf(currentEpochMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(16L)
            nowMs = currentEpochMillis()
            state.pruneExpiredTrails(nowMs)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        if (state.isCanvasVisible) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawTrails(state.gestureTrails, nowMs)
            }
        }

        AnimatedVisibility(
            visible = state.isPanelVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp),
        ) {
            LiveEventPanel(events = state.liveEvents)
        }

        DraggableCaptureButton(
            state = state,
            onCapture = onCapture,
            onTogglePanel = onTogglePanel,
        )
    }
}

@Composable
private fun DraggableCaptureButton(
    state: OverlayState,
    onCapture: () -> Unit,
    onTogglePanel: () -> Unit,
) {
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    state.captureButtonOffset.x.roundToInt(),
                    state.captureButtonOffset.y.roundToInt(),
                )
            }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    state.captureButtonOffset = state.captureButtonOffset + dragAmount
                }
            },
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            OverlayFab(
                onClick = onCapture,
                backgroundColor = Color(0xFFE53935),
                contentDescription = "Capture replay",
            ) {
                if (state.isCapturing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(text = "⏺", fontSize = 18.sp, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OverlayFab(
                onClick = onTogglePanel,
                backgroundColor = Color(0xFF1565C0),
                contentDescription = "Toggle log panel",
            ) {
                Text(text = if (state.isPanelVisible) "✕" else "≡", fontSize = 16.sp, color = Color.White)
            }
        }
    }
}

@Composable
private fun OverlayFab(
    onClick: () -> Unit,
    backgroundColor: Color,
    contentDescription: String,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(backgroundColor.copy(alpha = 0.90f))
            .border(width = 1.dp, color = Color.White.copy(alpha = 0.3f), shape = CircleShape)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {},
                    onDragEnd = {},
                    onDragCancel = {},
                    onDrag = { _, _ -> },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        IconButton(onClick = onClick) { content() }
    }
}

@Composable
private fun LiveEventPanel(events: List<InteractionEvent>) {
    val listState = rememberLazyListState()

    LaunchedEffect(events.size) {
        if (events.isNotEmpty()) listState.animateScrollToItem(events.size - 1)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(220.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color.Black.copy(alpha = 0.85f),
        tonalElevation = 8.dp,
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = "Tracey — Live Events",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            LazyColumn(state = listState) {
                items(events) { event ->
                    EventRow(event)
                }
            }
        }
    }
}

@Composable
private fun EventRow(event: InteractionEvent) {
    val (label, color) = when (event) {
        is InteractionEvent.Click -> "CLK  ${event.path.leaf()}" to Color(0xFFCE93D8)
        is InteractionEvent.LongPress -> "HOLD ${event.path.leaf()}" to Color(0xFFFFCC02)
        is InteractionEvent.Scroll -> "SCR  ${event.path.leaf()}" to Color(0xFF64B5F6)
        is InteractionEvent.Swipe -> "SWP  ${event.path.leaf()}" to Color(0xFF80CBC4)
        is InteractionEvent.Pinch -> "PCH  ${event.path.leaf()}" to Color(0xFFF48FB1)
        is InteractionEvent.AppForeground -> "FORE App" to Color(0xFF66BB6A)
        is InteractionEvent.AppBackground -> "BACK App" to Color(0xFFEF9A9A)
        is InteractionEvent.ScreenView -> "SCN  ${event.screenName.take(28)}" to Color(0xFF80DEEA)
        is InteractionEvent.Breadcrumb -> "LOG  ${event.message.take(28)}" to Color(0xFFFFD54F)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = color,
            fontSize = 11.sp,
            maxLines = 1,
        )
    }
}

private fun String.leaf(): String = split(" > ").lastOrNull()?.take(32) ?: this
