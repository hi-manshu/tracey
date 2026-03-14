package com.himanshoe.tracey

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect

/**
 * Records [screenName] as the currently active screen in the Tracey timeline.
 *
 * Place this composable at the top of each screen. It emits a
 * [com.himanshoe.tracey.model.InteractionEvent.ScreenView] when the composition
 * enters (or [screenName] changes) so the timeline always reflects the active screen
 * at the time of a capture or crash.
 *
 * ```kotlin
 * @Composable
 * fun HomeScreen() {
 *     TraceyScreen("HomeScreen")
 *     // ... content
 * }
 *
 * // Compose Navigation — in your NavHost destination block:
 * composable("profile/{userId}") { backStackEntry ->
 *     TraceyScreen("ProfileScreen")
 *     ProfileScreen(userId = backStackEntry.arguments?.getString("userId"))
 * }
 * ```
 *
 * If [Tracey.config] is disabled or Tracey is not installed this composable
 * is a no-op with zero overhead.
 *
 * @param screenName Human-readable screen identifier, e.g. `"HomeScreen"`.
 *   Prefer stable names that match your navigation graph route labels so events
 *   correlate cleanly across sessions.
 */
@Composable
fun TraceyScreen(screenName: String) {
    if (!Tracey.isInstalled || !Tracey.config.enabled) return
    DisposableEffect(screenName) {
        Tracey.screen(screenName)
        onDispose { }
    }
}
