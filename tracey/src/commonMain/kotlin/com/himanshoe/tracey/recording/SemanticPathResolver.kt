package com.himanshoe.tracey.recording

import kotlin.concurrent.Volatile
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull

/**
 * Resolves the composable breadcrumb path for a pointer event at ([x], [y]).
 *
 * The path is built by walking the semantics tree provided by [SemanticsOwner]
 * and finding the deepest [SemanticsNode] whose bounding rectangle contains
 * the given coordinates. The ancestry chain from that node back to the root
 * is then formatted as a `>` separated string using `testTag` and
 * `contentDescription` values.
 *
 * Example output: `"HomeScreen > LazyColumn > ProductCard[3] > AddToCartButton"`
 *
 * When no node contains the coordinates, or when the semantics tree is
 * unavailable, the resolver falls back to `"Screen[x=${x.toInt()},y=${y.toInt()}]"`
 * so every event always has a non-empty path.
 *
 * The resolver holds a nullable reference to the [SemanticsOwner] that is set
 * by [TraceyRecordingEngine] once the host composable enters the composition.
 * It can be updated at any time without synchronisation concerns because reads
 * and writes of reference types are atomic on all supported JVM/Native/Wasm
 * targets.
 */
internal class SemanticPathResolver {

    @Volatile
    private var semanticsOwner: SemanticsOwner? = null

    /**
     * The name of the currently active screen, set by [com.himanshoe.tracey.Tracey.screen].
     * Used as a path prefix when the semantics tree is unavailable (e.g. on iOS),
     * producing paths like `"HomeScreen > Screen[x=123,y=456]"`.
     */
    @Volatile
    var currentScreenName: String = ""

    /** Called from inside [TraceyHost] once the composition owner is available. */
    fun attach(owner: SemanticsOwner) {
        semanticsOwner = owner
    }

    /** Called when [TraceyHost] leaves the composition. */
    fun detach() {
        semanticsOwner = null
    }

    /**
     * Returns the composable path at ([x], [y]) or a coordinate fallback string.
     */
    fun resolve(x: Float, y: Float): String {
        val root = semanticsOwner?.rootSemanticsNode
            ?: return coordinateFallback(x, y)

        val target = findDeepestNodeAt(root, x, y)
            ?: return coordinateFallback(x, y)

        return buildPath(root, target)
            .ifEmpty { coordinateFallback(x, y) }
    }

    private fun findDeepestNodeAt(node: SemanticsNode, x: Float, y: Float): SemanticsNode? {
        if (!node.boundsInRoot.contains(x, y)) return null
        for (child in node.children) {
            val hit = findDeepestNodeAt(child, x, y)
            if (hit != null) return hit
        }
        return node
    }

    /**
     * Builds the breadcrumb string by collecting every ancestor of [target] up
     * to (but not including) the invisible root node, then joining with ` > `.
     */
    private fun buildPath(root: SemanticsNode, target: SemanticsNode): String {
        val chain = mutableListOf<String>()
        collectAncestors(root, target, chain)
        return chain.joinToString(" > ")
    }

    private fun collectAncestors(
        current: SemanticsNode,
        target: SemanticsNode,
        chain: MutableList<String>,
    ): Boolean {
        val label = nodeLabel(current)
        if (current.id == target.id) {
            if (label.isNotEmpty()) chain.add(label)
            return true
        }
        for (child in current.children) {
            if (collectAncestors(child, target, chain)) {
                if (label.isNotEmpty()) chain.add(0, label)
                return true
            }
        }
        return false
    }

    /**
     * Extracts a human-readable label from a [SemanticsNode].
     * Priority: `testTag` → `contentDescription` → empty string (root / anonymous nodes).
     */
    private fun nodeLabel(node: SemanticsNode): String {
        val tag = node.config.getOrNull(SemanticsProperties.TestTag)
        if (!tag.isNullOrBlank()) return tag

        val description = node.config
            .getOrNull(SemanticsProperties.ContentDescription)
            ?.firstOrNull()
        if (!description.isNullOrBlank()) return description

        return ""
    }

    private fun coordinateFallback(x: Float, y: Float): String {
        val prefix = if (currentScreenName.isNotEmpty()) "$currentScreenName > " else ""
        return "${prefix}Screen[x=${x.toInt()},y=${y.toInt()}]"
    }
}

private fun androidx.compose.ui.geometry.Rect.contains(x: Float, y: Float): Boolean =
    x in left..right && y in top..bottom
