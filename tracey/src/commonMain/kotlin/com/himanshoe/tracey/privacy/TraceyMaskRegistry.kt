package com.himanshoe.tracey.privacy

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color

internal data class MaskedRegion(val rect: Rect, val color: Color)

/**
 * Tracks composables marked with [com.himanshoe.tracey.traceyMask].
 *
 * Entries are keyed by a stable [Any] identity created per composable instance
 * so that sibling masks don't overwrite each other, and so that each mask is
 * removed cleanly when its composable leaves the composition.
 *
 * All access happens on the main thread (onGloballyPositioned + Dispatchers.Main
 * during screenshot capture), so no synchronisation is required.
 */
internal object TraceyMaskRegistry {

    private val regions: MutableMap<Any, MaskedRegion> = LinkedHashMap()

    fun register(key: Any, rect: Rect, color: Color) {
        regions[key] = MaskedRegion(rect, color)
    }

    fun unregister(key: Any) {
        regions.remove(key)
    }

    fun snapshot(): List<MaskedRegion> = regions.values.toList()
}
