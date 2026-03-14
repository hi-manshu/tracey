package com.himanshoe.tracey

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.layer.GraphicsLayer
import com.himanshoe.tracey.buffer.RingBuffer
import com.himanshoe.tracey.crash.CrashHandler
import com.himanshoe.tracey.export.TestCaseExporter
import com.himanshoe.tracey.model.DeviceInfo
import com.himanshoe.tracey.model.InteractionEvent
import com.himanshoe.tracey.model.ReplayPayload
import com.himanshoe.tracey.platform.currentEpochMillis
import com.himanshoe.tracey.platform.encodePng
import com.himanshoe.tracey.platform.getDeviceInfo
import com.himanshoe.tracey.platform.installLifecycleObserver
import com.himanshoe.tracey.export.ReplayHtmlExporter
import com.himanshoe.tracey.platform.saveToDisk
import com.himanshoe.tracey.privacy.TraceyMaskRegistry
import com.himanshoe.tracey.recording.RecordingEngine
import com.himanshoe.tracey.recording.SemanticPathResolver
import com.himanshoe.tracey.reporter.ReporterDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.concurrent.Volatile

/**
 * The main entry point for the Tracey SDK.
 *
 * **Setup — call once at application startup:**
 * ```kotlin
 * // Android — Application.onCreate()
 * // Desktop — before application { }
 * // iOS     — application(_:didFinishLaunchingWithOptions:)
 * // WasmJs  — before renderComposable { }
 * Tracey.install(TraceyConfig(reporters = listOf(LogcatReporter())))
 * ```
 *
 * **Recording — wrap your root composable:**
 * ```kotlin
 * setContent {
 *     TraceyHost {
 *         MyApp()
 *     }
 * }
 * ```
 *
 * **Manual capture — call from anywhere:**
 * ```kotlin
 * // From a coroutine, LaunchedEffect, ViewModel, shake handler, etc.
 * Tracey.capture()
 * Tracey.captureAndExportTest()
 * Tracey.captureAndExportFile()
 * ```
 *
 * Tracey is a singleton. All state is encapsulated here and accessed only
 * through the public API surface. Internal components ([RingBuffer],
 * [RecordingEngine], [CrashHandler], [ReporterDispatcher]) are package-private
 * and not part of the public API.
 */
object Tracey {

    @Volatile
    internal var config: TraceyConfig = TraceyConfig()
        private set

    @Volatile
    internal var isInstalled: Boolean = false
        private set

    internal val buffer = RingBuffer()
    internal val pathResolver = SemanticPathResolver()
    internal val recordingEngine = RecordingEngine(buffer, pathResolver)
    internal val reporterDispatcher = ReporterDispatcher()
    internal val crashHandler = CrashHandler()

    @Volatile
    internal var screenshotLayer: GraphicsLayer? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val buildPayloadMutex = Mutex()

    /**
     * Resets all singleton state back to defaults. **For use in tests only.**
     *
     * Call this in `@BeforeTest` / `@AfterTest` to prevent state leaking between
     * test cases that call [install]:
     *
     * ```kotlin
     * @AfterTest
     * fun tearDown() = runTest { Tracey.resetForTest() }
     * ```
     */
    internal suspend fun resetForTest() {
        config = TraceyConfig()
        isInstalled = false
        buffer.clear()
        screenshotLayer = null
        recordingEngine.enabled = false
        recordingEngine.onEventRecorded = null
        recordingEngine.redactedTags = emptyList()
        reporterDispatcher.setReporters(emptyList())
    }

    /**
     * Installs Tracey with the given [config].
     *
     * Safe to call multiple times — subsequent calls update the config and
     * re-register reporters without duplicating crash handlers.
     *
     * This function is synchronous and returns immediately. The crash handler
     * installation and pending replay check are lightweight operations.
     *
     * @param config The configuration to apply. Defaults to [TraceyConfig].
     */
    fun install(config: TraceyConfig = TraceyConfig()) {
        this.config = config
        isInstalled = true

        buffer.configure(
            maxDurationMs = config.bufferDurationSeconds * 1000L,
            maxEvents = config.maxEvents,
        )
        recordingEngine.enabled = config.enabled
        recordingEngine.redactedTags = config.redactedTags
        reporterDispatcher.setReporters(config.reporters)

        if (!config.enabled) return

        crashHandler.install { crashReason ->
            buildPayloadBlocking(crashReason)
        }

        crashHandler.checkAndDispatchPendingReplay(reporterDispatcher)

        if (config.trackLifecycle) {
            installLifecycleObserver { event -> recordingEngine.record(event) }
        }
    }

    /**
     * Records a developer-defined breadcrumb that appears inline in the timeline.
     *
     * Call from anywhere — a `ViewModel`, a repository, a network interceptor, or
     * a click handler. No coroutine required.
     *
     * ```kotlin
     * Tracey.log("User tapped checkout")
     * Tracey.log("API error 503 on /orders")
     * ```
     *
     * @param message Short, actionable description of what happened.
     */
    fun log(message: String) {
        recordingEngine.record(
            InteractionEvent.Breadcrumb(
                timestampMs = currentEpochMillis(),
                message = message,
            )
        )
    }

    /**
     * Records that a named screen became active.
     *
     * Call this at the top of each screen composable, or from a navigation
     * observer. Prefer the `TraceyScreen` composable for Compose Navigation.
     *
     * ```kotlin
     * LaunchedEffect(Unit) { Tracey.screen("HomeScreen") }
     * ```
     *
     * @param name Human-readable screen name, e.g. `"HomeScreen"`.
     */
    fun screen(name: String) {
        pathResolver.currentScreenName = name
        recordingEngine.record(
            InteractionEvent.ScreenView(
                timestampMs = currentEpochMillis(),
                path = name,
                screenName = name,
            )
        )
    }

    /**
     * Records that a navigation route became active.
     *
     * Prefer this over [screen] when integrating with a navigation library —
     * it signals that the name comes from a nav graph route rather than a
     * hand-written label. The underlying recording is identical.
     *
     * Called automatically by `tracey-navigation`'s `rememberTraceyNavController()`
     * and `NavController.trackWithTracey()`.
     *
     * ```kotlin
     * // Manual usage (prefer the tracey-navigation module instead):
     * navController.addOnDestinationChangedListener { _, dest, _ ->
     *     Tracey.route(dest.route ?: return@addOnDestinationChangedListener)
     * }
     * ```
     *
     * @param name The nav graph route string, e.g. `"HomeScreen"` or `"profile/{userId}"`.
     */
    fun route(name: String) {
        pathResolver.currentScreenName = name
        recordingEngine.record(
            InteractionEvent.ScreenView(
                timestampMs = currentEpochMillis(),
                path = name,
                screenName = name,
            )
        )
    }

    /**
     * Snapshots the current ring buffer, builds a [ReplayPayload], and delivers
     * it to every registered [com.himanshoe.tracey.reporter.TraceyReporter].
     *
     * The returned [ReplayPayload] has [ReplayPayload.isCrashPayload] = `false`
     * and [ReplayPayload.crashReason] = `null`.
     *
     * Call this from any coroutine context — a `LaunchedEffect`, a `ViewModel`,
     * a shake gesture handler, a debug menu action, or a background service.
     *
     * @param dispatcher The dispatcher on which reporter calls are made.
     *                   Defaults to [Dispatchers.Default].
     * @return The assembled [ReplayPayload].
     */
    suspend fun capture(
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
    ): ReplayPayload = buildPayloadMutex.withLock {
        val payload = buildPayload(crashReason = null)
        reporterDispatcher.dispatch(payload)
        if (config.generateHtmlReport) {
            runCatching {
                val html = ReplayHtmlExporter.export(payload)
                val key = "tracey_replay_${payload.sessionId}.html"
                saveToDisk(key, html)
            }
        }
        payload
    }

    /**
     * Performs a capture and additionally generates a Compose UI test as a
     * Kotlin source string via [TestCaseExporter].
     *
     * The test string is returned directly. Write it to a file, copy it to the
     * clipboard, attach it to a GitHub issue, or paste it into your test suite.
     *
     * @return A complete, runnable Compose UI test as a Kotlin string.
     */
    suspend fun captureAndExportTest(): String {
        val payload = capture()
        return TestCaseExporter.export(payload)
    }

    /**
     * Performs a capture and writes the [ReplayPayload] JSON to durable storage.
     *
     * The storage location is platform-specific:
     * - **Android** — `<filesDir>/tracey/<sessionId>.json`
     * - **iOS** — `<Documents>/tracey/<sessionId>.json`
     * - **Desktop** — `<userHome>/.tracey/<sessionId>.json`
     * - **WasmJs** — `localStorage` key `tracey_<sessionId>`
     *
     * @return [Result.success] with the storage key on success, or
     *         [Result.failure] with the underlying exception on failure.
     */
    suspend fun captureAndExportFile(): Result<String> {
        val payload = capture()
        val key = "tracey_replay_${payload.sessionId}"
        return runCatching {
            saveToDisk(key, payload.toJson())
            key
        }
    }

    /**
     * Performs a capture and generates a self-contained HTML report.
     *
     * The report contains a **visual user flow diagram** (screen cards with
     * event chips), the screenshot taken at capture time (if available), and
     * the full text timeline — all in a single file with no external dependencies.
     *
     * Write the returned string to a `.html` file or share it directly:
     * ```kotlin
     * val html = Tracey.captureAndExportHtml()
     * File(context.cacheDir, "replay.html").writeText(html)
     * ```
     *
     * @return A complete, self-contained HTML document as a [String].
     */
    suspend fun captureAndExportHtml(): String {
        val payload = capture()
        return ReplayHtmlExporter.export(payload)
    }

    internal suspend fun buildPayload(crashReason: String?): ReplayPayload {
        val events = buffer.snapshot()
        val nowMs = currentEpochMillis()
        val firstMs = events.firstOrNull()?.timestampMs ?: nowMs
        val device = getDeviceInfo()
        val screenshotPng = withContext(Dispatchers.Main) {
            runCatching {
                screenshotLayer?.toImageBitmap()
                    ?.let { applyMasks(it) }
                    ?.let { encodePng(it) }
            }.getOrNull()
        }

        return ReplayPayload(
            sessionId = config.sessionIdProvider(),
            appVersion = device.appVersion,
            capturedAtMs = nowMs,
            durationMs = nowMs - firstMs,
            crashReason = crashReason,
            deviceInfo = device,
            events = events,
            timeline = buildTimeline(events, crashReason, firstMs),
            screenshotPng = screenshotPng,
        )
    }

    private fun applyMasks(source: ImageBitmap): ImageBitmap {
        val regions = TraceyMaskRegistry.snapshot()
        if (regions.isEmpty()) return source
        val result = ImageBitmap(source.width, source.height)
        val canvas = Canvas(result)
        canvas.drawImage(source, Offset.Zero, Paint())
        regions.forEach { (rect, color) ->
            canvas.drawRect(rect, Paint().apply { this.color = color })
        }
        return result
    }

    private fun buildPayloadBlocking(crashReason: String): ReplayPayload {
        val events = runCatching { buffer.snapshotUnsafe() }.getOrElse { emptyList() }
        val nowMs = currentEpochMillis()
        val firstMs = events.firstOrNull()?.timestampMs ?: nowMs
        val device = runCatching { getDeviceInfo() }.getOrElse {
            DeviceInfo(
                platform = "Unknown",
                model = "Unknown",
                osVersion = "Unknown",
                screenWidthPx = 0,
                screenHeightPx = 0,
                density = 1f,
                appVersion = "Unknown",
                appPackage = "Unknown",
                locale = "en",
            )
        }

        return ReplayPayload(
            sessionId = runCatching { config.sessionIdProvider() }.getOrElse { "crash-session" },
            appVersion = device.appVersion,
            capturedAtMs = nowMs,
            durationMs = nowMs - firstMs,
            crashReason = crashReason,
            deviceInfo = device,
            events = events,
            timeline = buildTimeline(events, crashReason, firstMs),
        )
    }

    private fun buildTimeline(
        events: List<InteractionEvent>,
        crashReason: String?,
        baseMs: Long,
    ): String = buildString {
        events.forEach { event ->
            val relMs = event.timestampMs - baseMs
            val time = formatTime(relMs)
            val line = when (event) {
                is InteractionEvent.Click ->
                    "$time  CLK        ${event.path}"
                is InteractionEvent.LongPress ->
                    "$time  LONG PRESS ${event.path} (${event.holdDurationMs}ms)"
                is InteractionEvent.Scroll ->
                    "$time  SCROLL     ${event.path} (Δx=${event.deltaX.fmt()}, Δy=${event.deltaY.fmt()})"
                is InteractionEvent.Swipe ->
                    "$time  SWIPE      ${event.path} (${event.startX.fmt()},${event.startY.fmt()} → ${event.endX.fmt()},${event.endY.fmt()})"
                is InteractionEvent.Pinch ->
                    "$time  PINCH      ${event.path} (zoom=${event.zoomDelta.fmt()}×)"
                is InteractionEvent.AppForeground ->
                    "$time  FOREGROUND App came to foreground"
                is InteractionEvent.AppBackground ->
                    "$time  BACKGROUND App went to background"
                is InteractionEvent.ScreenView ->
                    "$time  SCREEN     ${event.screenName}"
                is InteractionEvent.Breadcrumb ->
                    "$time  LOG        ${event.message}"

            }
            appendLine(line)
        }
        if (crashReason != null) {
            val nowMs = currentEpochMillis()
            val relMs = nowMs - baseMs
            appendLine("${formatTime(relMs)}  💥 CRASH   $crashReason")
        }
    }

    private fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val millis = ms % 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}.${millis.toString().padStart(3, '0')}"
    }

    private fun Float.fmt(): String {
        val intPart = toInt()
        val fracPart = ((kotlin.math.abs(this) * 10).toInt() % 10)
        return "$intPart.$fracPart"
    }
}
