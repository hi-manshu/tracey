package com.himanshoe.tracey.navigation3

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshotFlow
import com.himanshoe.tracey.Tracey
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Creates and remembers a Navigation 3 back stack that automatically records a
 * [com.himanshoe.tracey.model.InteractionEvent.ScreenView] event whenever the
 * top-of-stack key changes.
 *
 * Drop-in replacement for `remember { mutableStateListOf<Any>(...) }`:
 * ```kotlin
 * val backStack = rememberTraceyNavBackStack(Home)
 *
 * NavDisplay(
 *     backStack = backStack,
 *     onBack    = { backStack.removeLastOrNull() },
 *     entryProvider = entryProvider {
 *         entry<Home>          { HomeScreen() }
 *         entry<ProfileScreen> { ProfileScreen() }
 *     }
 * )
 * ```
 *
 * The screen name reported to Tracey is derived from [nameSelector], which defaults
 * to `key::class.simpleName`. This works well for data objects and data classes:
 * - `data object HomeScreen`            → recorded as `"HomeScreen"`
 * - `data class ProductDetail(val id: String)` → recorded as `"ProductDetail"`
 *
 * For fully qualified names or custom labels, supply your own [nameSelector]:
 * ```kotlin
 * val backStack = rememberTraceyNavBackStack(Home) { key ->
 *     when (key) {
 *         is Home          -> "Home Feed"
 *         is ProductDetail -> "Product — ${key.id}"
 *         else             -> key::class.simpleName ?: key.toString()
 *     }
 * }
 * ```
 *
 * @param initialKeys  The keys to pre-populate the back stack with.
 * @param nameSelector Converts a back stack key to a human-readable screen name.
 * @return A [SnapshotStateList] backed back stack wired up for Tracey screen tracking.
 * @see trackWithTracey
 */
@Composable
fun rememberTraceyNavBackStack(
    vararg initialKeys: Any,
    nameSelector: (Any) -> String = { key -> key::class.simpleName ?: key.toString() },
): SnapshotStateList<Any> {
    val backStack = remember { mutableStateListOf<Any>(*initialKeys) }
    backStack.trackWithTracey(nameSelector)
    return backStack
}

/**
 * Attaches a Tracey screen-tracking observer to an existing Navigation 3 back stack.
 *
 * Use this when you already hold a back stack and cannot use [rememberTraceyNavBackStack]:
 * ```kotlin
 * val backStack = remember { mutableStateListOf<Any>(Home) }
 * backStack.trackWithTracey()
 *
 * NavDisplay(backStack = backStack, ...)
 * ```
 *
 * The observer uses [snapshotFlow] to watch the top of the stack and fires a
 * [com.himanshoe.tracey.model.InteractionEvent.ScreenView] only when the top key
 * actually changes — duplicate pushes of the same key are ignored.
 *
 * The observer is cancelled automatically when the calling composable leaves the
 * composition.
 *
 * @param nameSelector Converts a back stack key to a human-readable screen name.
 */
@Composable
fun SnapshotStateList<Any>.trackWithTracey(
    nameSelector: (Any) -> String = { key -> key::class.simpleName ?: key.toString() },
) {
    LaunchedEffect(this) {
        snapshotFlow { lastOrNull() }
            .distinctUntilChanged()
            .collect { key ->
                if (key != null) Tracey.route(nameSelector(key))
            }
    }
}
