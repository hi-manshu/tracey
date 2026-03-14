package com.himanshoe.tracey

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import com.himanshoe.tracey.overlay.TraceyOverlay
import com.himanshoe.tracey.overlay.OverlayState
import com.himanshoe.tracey.platform.AttachSemanticsOwner
import kotlinx.coroutines.launch

/**
 * The root recording composable for Tracey.
 *
 * Wrap your app's root composable with [TraceyHost] to activate recording.
 * Configure it via [rememberTraceyConfig] — no separate [Tracey.install] call needed.
 *
 * ```kotlin
 * TraceyHost(
 *     traceyConfig = rememberTraceyConfig(
 *         showOverlay = BuildConfig.DEBUG,
 *         reporters   = listOf(LogcatReporter()),
 *     )
 * ) {
 *     MyApp()
 * }
 * ```
 *
 * If [Tracey.install] was already called before the first composition (e.g. from
 * `Application.onCreate`) the supplied [traceyConfig] is ignored and the previously
 * installed config takes effect.
 *
 * [TraceyHost] does three things:
 * 1. Attaches the recording [Modifier] at the outermost layout level.
 * 2. Wires the semantics owner so events get composable breadcrumb paths.
 * 3. Renders [TraceyOverlay] on top of content when [TraceyConfig.showOverlay] is `true`.
 *
 * When [TraceyConfig.enabled] is `false` this composable is a transparent passthrough.
 *
 * @param traceyConfig SDK configuration. Use [rememberTraceyConfig] to create it.
 *                     Defaults to [rememberTraceyConfig] with all defaults applied.
 * @param modifier     Optional [Modifier] applied to the host container.
 * @param content      Your app's root composable.
 */
@Composable
fun TraceyHost(
    traceyConfig: TraceyConfig = rememberTraceyConfig(),
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val config = remember(traceyConfig) {
        if (!Tracey.isInstalled) Tracey.install(traceyConfig)
        Tracey.config
    }

    if (!config.enabled) {
        Box(modifier = modifier) { content() }
        return
    }

    val overlayState = remember { OverlayState() }
    val scope = rememberCoroutineScope()
    val graphicsLayer = rememberGraphicsLayer()

    AttachSemanticsOwner(Tracey.pathResolver)

    DisposableEffect(overlayState) {
        Tracey.recordingEngine.onEventRecorded = overlayState::addEvent
        onDispose { Tracey.recordingEngine.onEventRecorded = null }
    }

    DisposableEffect(graphicsLayer) {
        Tracey.screenshotLayer = graphicsLayer
        onDispose { Tracey.screenshotLayer = null }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithContent {
                graphicsLayer.record { this@drawWithContent.drawContent() }
                drawLayer(graphicsLayer)
            }
            .then(Tracey.recordingEngine.modifier()),
    ) {
        content()

        if (config.showOverlay) {
            TraceyOverlay(
                state = overlayState,
                onCapture = {
                    scope.launch {
                        overlayState.isCapturing = true
                        runCatching { Tracey.capture() }
                        overlayState.isCapturing = false
                    }
                },
                onTogglePanel = {
                    overlayState.isPanelVisible = !overlayState.isPanelVisible
                },
            )
        }
    }

    LaunchedEffect(Unit) {
        Tracey.recordingEngine.enabled = config.enabled
    }
}
