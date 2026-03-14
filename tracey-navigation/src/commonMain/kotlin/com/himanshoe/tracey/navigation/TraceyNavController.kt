package com.himanshoe.tracey.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.himanshoe.tracey.Tracey

/**
 * Creates and remembers a [NavHostController] that automatically records a
 * [com.himanshoe.tracey.model.InteractionEvent.ScreenView] event — and updates
 * the iOS path prefix — whenever the active navigation destination changes.
 *
 * Drop-in replacement for [rememberNavController]:
 * ```kotlin
 * val navController = rememberTraceyNavController()
 *
 * NavHost(navController, startDestination = "HomeScreen") {
 *     composable("HomeScreen") { HomeScreen() }
 *     composable("ProfileScreen") { ProfileScreen() }
 *     composable("SettingsScreen") { SettingsScreen() }
 * }
 * ```
 *
 * The screen name recorded is the destination's `route` string. Use descriptive
 * route names so the Tracey timeline is human-readable:
 * - Good: `"HomeScreen"`, `"CheckoutScreen"`, `"OrderDetailScreen"`
 * - Less useful: `"a"`, `"screen1"`, `"frag_home"`
 *
 * If Tracey is not installed, [Tracey.route] is a no-op — no events are recorded
 * and no crash will occur.
 *
 * @return A [NavHostController] wired up for automatic Tracey screen tracking.
 * @see trackWithTracey
 */
@Composable
fun rememberTraceyNavController(): NavHostController {
    val navController = rememberNavController()
    navController.trackWithTracey()
    return navController
}

/**
 * Attaches a Tracey screen-tracking listener to an existing [NavController].
 *
 * Use this when you already hold a [NavController] and cannot use
 * [rememberTraceyNavController]:
 * ```kotlin
 * val navController = rememberNavController()
 * navController.trackWithTracey()
 *
 * NavHost(navController, startDestination = "HomeScreen") { ... }
 * ```
 *
 * The listener is automatically removed when the calling composable leaves the
 * composition, so there is no risk of leaking the listener.
 *
 * On iOS this also keeps [com.himanshoe.tracey.recording.SemanticPathResolver]'s
 * screen name prefix up to date, so gesture paths read as
 * `"ProfileScreen > Screen[x=N,y=M]"` rather than bare coordinates.
 */
@Composable
fun NavController.trackWithTracey() {
    DisposableEffect(this) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            val route = destination.route ?: return@OnDestinationChangedListener
            Tracey.route(route)
        }
        addOnDestinationChangedListener(listener)
        onDispose { removeOnDestinationChangedListener(listener) }
    }
}
