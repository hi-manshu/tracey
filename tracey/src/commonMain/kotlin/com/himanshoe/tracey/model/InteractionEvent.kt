package com.himanshoe.tracey.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A single recorded interaction captured by the Tracey recording engine.
 *
 * Every event carries:
 * - [timestampMs] — epoch millisecond of the frame in which the gesture was detected.
 * - [path] — human-readable composable breadcrumb built from the semantics tree,
 *   e.g. `"HomeScreen > LazyColumn > ProductCard[3] > AddToCartButton"`.
 *
 * The sealed hierarchy covers every pointer gesture Compose surfaces:
 * [Click], [LongPress], [Scroll], [Swipe], [Pinch].
 */
@Serializable
sealed interface InteractionEvent {

    /** Epoch-millisecond timestamp of the frame in which this event was recorded. */
    val timestampMs: Long

    /**
     * Composable breadcrumb trail resolved by walking the semantics tree at the
     * event coordinates. Uses `testTag` values and semantic content descriptions.
     * Falls back to coordinate notation when the tree is unavailable.
     */
    val path: String

    /**
     * A single pointer-down / pointer-up with no significant movement.
     *
     * @property x Horizontal coordinate in dp relative to the root composable.
     * @property y Vertical coordinate in dp relative to the root composable.
     */
    @Immutable
    @Serializable
    @SerialName("click")
    data class Click(
        override val timestampMs: Long,
        override val path: String,
        val x: Float,
        val y: Float,
    ) : InteractionEvent

    /**
     * A pointer held in place past the long-press threshold.
     *
     * @property x Horizontal coordinate in dp.
     * @property y Vertical coordinate in dp.
     * @property holdDurationMs How long the pointer was held before this event fired.
     */
    @Immutable
    @Serializable
    @SerialName("long_press")
    data class LongPress(
        override val timestampMs: Long,
        override val path: String,
        val x: Float,
        val y: Float,
        val holdDurationMs: Long,
    ) : InteractionEvent

    /**
     * A scroll gesture — continuous delta movement from a single pointer.
     *
     * @property x Horizontal anchor of the scroll in dp.
     * @property y Vertical anchor of the scroll in dp.
     * @property deltaX Horizontal scroll delta in dp (positive = right).
     * @property deltaY Vertical scroll delta in dp (positive = down).
     * @property velocityX Horizontal fling velocity in dp/s at gesture end.
     * @property velocityY Vertical fling velocity in dp/s at gesture end.
     */
    @Immutable
    @Serializable
    @SerialName("scroll")
    data class Scroll(
        override val timestampMs: Long,
        override val path: String,
        val x: Float,
        val y: Float,
        val deltaX: Float,
        val deltaY: Float,
        val velocityX: Float,
        val velocityY: Float,
    ) : InteractionEvent

    /**
     * A directional swipe — a pointer that moved significantly and lifted.
     *
     * @property startX Starting horizontal coordinate in dp.
     * @property startY Starting vertical coordinate in dp.
     * @property endX Ending horizontal coordinate in dp.
     * @property endY Ending vertical coordinate in dp.
     * @property velocityX Horizontal velocity at pointer lift in dp/s.
     * @property velocityY Vertical velocity at pointer lift in dp/s.
     */
    @Immutable
    @Serializable
    @SerialName("swipe")
    data class Swipe(
        override val timestampMs: Long,
        override val path: String,
        val startX: Float,
        val startY: Float,
        val endX: Float,
        val endY: Float,
        val velocityX: Float,
        val velocityY: Float,
    ) : InteractionEvent

    /**
     * A two-finger pinch or spread gesture.
     *
     * @property centroidX Horizontal midpoint between the two pointers in dp.
     * @property centroidY Vertical midpoint between the two pointers in dp.
     * @property zoomDelta Scale factor change during this event (>1 spread, <1 pinch).
     * @property rotationDelta Rotation change in degrees (clockwise positive).
     */
    @Immutable
    @Serializable
    @SerialName("pinch")
    data class Pinch(
        override val timestampMs: Long,
        override val path: String,
        val centroidX: Float,
        val centroidY: Float,
        val zoomDelta: Float,
        val rotationDelta: Float,
    ) : InteractionEvent

    /**
     * The host application transitioned from background to foreground.
     *
     * Emitted automatically when [com.himanshoe.tracey.TraceyConfig.trackLifecycle]
     * is `true`. Powered by `ActivityLifecycleCallbacks` on Android,
     * `UIApplicationDidBecomeActiveNotification` on iOS, and
     * `document.visibilitychange` on WasmJs.
     */
    @Immutable
    @Serializable
    @SerialName("app_foreground")
    data class AppForeground(
        override val timestampMs: Long,
        override val path: String = "App",
    ) : InteractionEvent

    /**
     * The host application moved to the background.
     *
     * Emitted automatically when [com.himanshoe.tracey.TraceyConfig.trackLifecycle]
     * is `true`.
     */
    @Immutable
    @Serializable
    @SerialName("app_background")
    data class AppBackground(
        override val timestampMs: Long,
        override val path: String = "App",
    ) : InteractionEvent

    /**
     * A named screen became active — either through navigation or an
     * activity/scene resume.
     *
     * Emitted automatically for Android `Activity` transitions when
     * [com.himanshoe.tracey.TraceyConfig.trackLifecycle] is `true`, and
     * on demand via [com.himanshoe.tracey.Tracey.screen] or the
     * `TraceyScreen` composable.
     *
     * @property screenName Human-readable screen identifier, e.g. `"HomeScreen"`.
     */
    @Immutable
    @Serializable
    @SerialName("screen_view")
    data class ScreenView(
        override val timestampMs: Long,
        override val path: String,
        val screenName: String,
    ) : InteractionEvent

    /**
     * A developer-defined breadcrumb log entry.
     *
     * Emit breadcrumbs via [com.himanshoe.tracey.Tracey.log] from anywhere —
     * ViewModels, repositories, network layers, etc. Breadcrumbs appear inline
     * in the timeline between gesture events, giving essential context for crash
     * reports and replays.
     *
     * ```kotlin
     * Tracey.log("Cart item added: SKU-1234")
     * Tracey.log("API error 503 on /checkout")
     * ```
     *
     * @property message The breadcrumb text. Keep it short and actionable.
     */
    @Immutable
    @Serializable
    @SerialName("breadcrumb")
    data class Breadcrumb(
        override val timestampMs: Long,
        override val path: String = "Manual",
        val message: String,
    ) : InteractionEvent
}
