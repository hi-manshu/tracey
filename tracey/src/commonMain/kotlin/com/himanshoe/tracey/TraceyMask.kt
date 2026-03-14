package com.himanshoe.tracey

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import com.himanshoe.tracey.privacy.TraceyMaskRegistry

/**
 * Marks this composable as sensitive, replacing its screen region with a solid
 * [maskColor] rectangle in any screenshot captured by Tracey.
 *
 * The composable renders normally on screen — only the screenshot is affected.
 * Use this on fields that contain PII such as passwords, payment details,
 * personal identifiers, or health data.
 *
 * ```kotlin
 * TextField(
 *     value = cardNumber,
 *     onValueChange = { cardNumber = it },
 *     modifier = Modifier
 *         .fillMaxWidth()
 *         .traceyMask(),
 * )
 * ```
 *
 * Multiple masked composables are supported simultaneously. Each one
 * independently tracks its own position as the layout changes (e.g. after
 * scroll or recomposition) and is automatically unregistered when it leaves
 * the composition.
 *
 * @param maskColor The fill color painted over the redacted region in the
 *                  screenshot. Defaults to [Color.Black].
 */
fun Modifier.traceyMask(maskColor: Color = Color.Black): Modifier = composed {
    val key = remember { Any() }

    DisposableEffect(key) {
        onDispose { TraceyMaskRegistry.unregister(key) }
    }

    onGloballyPositioned { coords ->
        val pos = coords.positionInRoot()
        val size = coords.size
        TraceyMaskRegistry.register(
            key   = key,
            rect  = Rect(pos.x, pos.y, pos.x + size.width, pos.y + size.height),
            color = maskColor,
        )
    }
}
