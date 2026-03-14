package com.himanshoe.tracey.overlay

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Draws gesture trails and tap ripples onto a Compose [DrawScope].
 *
 * Gesture types are colour-coded:
 * - **Click** → purple with a ripple circle
 * - **Long Press** → orange with a larger ripple
 * - **Scroll** → blue bezier curve
 * - **Swipe** → teal arrow curve
 * - **Pinch** → pink centroid indicator
 *
 * Trails fade linearly over [TRAIL_FADE_DURATION_MS] milliseconds using the
 * alpha derived from how old the newest point in each trail is. This keeps the
 * overlay readable without cluttering the screen with stale paths.
 *
 * All drawing is stateless — the painter reads from [OverlayState] values
 * passed in at each frame and produces no allocations beyond the [Path] objects
 * that Compose's Canvas already requires.
 */
internal object GestureTrailPainter {

    private const val TRAIL_FADE_DURATION_MS = 2_000L
    private const val STROKE_WIDTH = 6f
    private const val RIPPLE_RADIUS_CLICK = 28f
    private const val RIPPLE_RADIUS_LONG_PRESS = 44f

    private val colorClick = Color(0xFF9C27B0)
    private val colorLongPress = Color(0xFFFF9800)
    private val colorScroll = Color(0xFF2196F3)
    private val colorSwipe = Color(0xFF009688)
    private val colorPinch = Color(0xFFE91E63)

    /**
     * Draws all [trails] from [OverlayState] onto this [DrawScope].
     *
     * @param trails  The list of active gesture trails from [OverlayState.gestureTrails].
     * @param nowMs   Current epoch millisecond used to compute trail alpha.
     */
    fun DrawScope.drawTrails(trails: List<GestureTrail>, nowMs: Long) {
        trails.forEach { trail ->
            val newestMs = trail.points.lastOrNull()?.timestampMs ?: return@forEach
            val age = (nowMs - newestMs).coerceIn(0L, TRAIL_FADE_DURATION_MS)
            val alpha = 1f - (age.toFloat() / TRAIL_FADE_DURATION_MS)
            if (alpha <= 0f) return@forEach

            when (trail.type) {
                EventType.Click -> drawClickTrail(trail, alpha)
                EventType.LongPress -> drawLongPressTrail(trail, alpha)
                EventType.Scroll -> drawScrollTrail(trail, alpha)
                EventType.Swipe -> drawSwipeTrail(trail, alpha)
                EventType.Pinch -> drawPinchTrail(trail, alpha)
            }
        }
    }

    private fun DrawScope.drawClickTrail(trail: GestureTrail, alpha: Float) {
        val point = trail.points.lastOrNull() ?: return
        val color = colorClick.copy(alpha = alpha)
        drawCircle(color = color, radius = RIPPLE_RADIUS_CLICK, center = point.offset, style = Stroke(width = STROKE_WIDTH))
        drawCircle(color = color.copy(alpha = alpha * 0.2f), radius = RIPPLE_RADIUS_CLICK * 0.5f, center = point.offset)
    }

    private fun DrawScope.drawLongPressTrail(trail: GestureTrail, alpha: Float) {
        val point = trail.points.lastOrNull() ?: return
        val color = colorLongPress.copy(alpha = alpha)
        drawCircle(color = color, radius = RIPPLE_RADIUS_LONG_PRESS, center = point.offset, style = Stroke(width = STROKE_WIDTH))
        drawCircle(color = color, radius = RIPPLE_RADIUS_LONG_PRESS * 0.6f, center = point.offset, style = Stroke(width = STROKE_WIDTH * 0.5f))
        drawCircle(color = color.copy(alpha = alpha * 0.15f), radius = RIPPLE_RADIUS_LONG_PRESS, center = point.offset)
    }

    private fun DrawScope.drawScrollTrail(trail: GestureTrail, alpha: Float) {
        if (trail.points.size < 2) return
        val path = buildBezierPath(trail.points.map { it.offset })
        drawPath(
            path = path,
            color = colorScroll.copy(alpha = alpha),
            style = Stroke(
                width = STROKE_WIDTH,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
    }

    private fun DrawScope.drawSwipeTrail(trail: GestureTrail, alpha: Float) {
        val point = trail.points.firstOrNull() ?: return
        val end = point.endOffset ?: return
        val color = colorSwipe.copy(alpha = alpha)

        val path = Path().apply {
            moveTo(point.offset.x, point.offset.y)
            lineTo(end.x, end.y)
        }
        drawPath(path = path, color = color, style = Stroke(width = STROKE_WIDTH, cap = StrokeCap.Round))

        drawArrowHead(start = point.offset, end = end, color = color, alpha = alpha)
    }

    private fun DrawScope.drawPinchTrail(trail: GestureTrail, alpha: Float) {
        val point = trail.points.lastOrNull() ?: return
        val color = colorPinch.copy(alpha = alpha)
        val radius = 20f
        drawCircle(color = color, radius = radius, center = point.offset, style = Stroke(width = STROKE_WIDTH))
        drawLine(color = color, start = point.offset - Offset(radius, 0f), end = point.offset + Offset(radius, 0f), strokeWidth = STROKE_WIDTH * 0.5f)
        drawLine(color = color, start = point.offset - Offset(0f, radius), end = point.offset + Offset(0f, radius), strokeWidth = STROKE_WIDTH * 0.5f)
    }

    private fun DrawScope.drawArrowHead(start: Offset, end: Offset, color: Color, alpha: Float) {
        val direction = (end - start).let { d ->
            val len = kotlin.math.sqrt(d.x * d.x + d.y * d.y)
            if (len == 0f) return
            Offset(d.x / len, d.y / len)
        }
        val arrowSize = 20f
        val perp = Offset(-direction.y, direction.x)
        val tip = end
        val base1 = end - direction * arrowSize + perp * (arrowSize * 0.5f)
        val base2 = end - direction * arrowSize - perp * (arrowSize * 0.5f)

        val arrowPath = Path().apply {
            moveTo(tip.x, tip.y)
            lineTo(base1.x, base1.y)
            lineTo(base2.x, base2.y)
            close()
        }
        drawPath(path = arrowPath, color = color.copy(alpha = alpha))
    }

    private fun buildBezierPath(points: List<Offset>): Path {
        val path = Path()
        if (points.isEmpty()) return path
        path.moveTo(points.first().x, points.first().y)
        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val curr = points[i]
            val midX = (prev.x + curr.x) / 2f
            val midY = (prev.y + curr.y) / 2f
            path.quadraticBezierTo(prev.x, prev.y, midX, midY)
        }
        path.lineTo(points.last().x, points.last().y)
        return path
    }
}
