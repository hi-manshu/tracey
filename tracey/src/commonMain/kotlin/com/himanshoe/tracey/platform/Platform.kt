package com.himanshoe.tracey.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import com.himanshoe.tracey.model.DeviceInfo
import com.himanshoe.tracey.model.InteractionEvent
import com.himanshoe.tracey.recording.SemanticPathResolver

/**
 * Returns platform-specific device and hardware metadata.
 * Each target (Android, iOS, Desktop, WasmJs) provides its own actual.
 */
internal expect fun getDeviceInfo(): DeviceInfo

/**
 * Returns the current time as epoch milliseconds.
 * Backed by the platform clock on each target.
 */
internal expect fun currentEpochMillis(): Long

/**
 * Generates a random UUID string in canonical `xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx` format.
 */
internal expect fun generateUUID(): String

/**
 * Emits a log line through the platform's native logging channel.
 * Android uses `android.util.Log`, Desktop and iOS use stdout, WasmJs uses `console.log`.
 *
 * @param tag   Short label identifying the source of the log.
 * @param message The message to emit.
 */
internal expect fun platformLog(tag: String, message: String)

/**
 * Persists [json] to durable, crash-safe storage under [key].
 * On Android this is a private file in `filesDir`; on Desktop a file in the
 * user home directory; on iOS the app's Documents directory; on WasmJs `localStorage`.
 *
 * Called synchronously from the uncaught-exception handler so the implementation
 * must be fast and must not throw.
 */
internal expect fun saveToDisk(key: String, json: String)

/**
 * Reads a previously persisted value from durable storage, or returns null
 * if no value exists for [key].
 */
internal expect fun loadFromDisk(key: String): String?

/**
 * Removes the value stored under [key] from durable storage.
 */
internal expect fun deleteFromDisk(key: String)

/**
 * Installs a platform-level uncaught-exception handler that invokes [onCrash]
 * with the offending [Throwable] before the process terminates.
 *
 * The implementation must chain to any previously installed handler so that
 * crash reporters (Crashlytics, Sentry, etc.) continue to function normally.
 *
 * @param onCrash Callback invoked synchronously on the crashing thread.
 *                Must complete quickly — the process is dying.
 */
internal expect fun installUncaughtExceptionHandler(onCrash: (Throwable) -> Unit)

/**
 * Returns a human-readable version string for the host application,
 * e.g. `"2.4.1"`. Returns `"unknown"` when the version cannot be determined.
 */
internal expect fun appVersion(): String

/**
 * Returns the host application's package name or bundle identifier.
 */
internal expect fun appPackage(): String

/**
 * Attaches the platform semantics owner to [resolver] for composable path resolution,
 * and detaches it when the composition is disposed.
 *
 * On JVM/Native targets this uses [LocalComposeOwner]; on WasmJs it is a no-op
 * because the semantics tree is not accessible from common code.
 */
@androidx.compose.runtime.Composable
internal expect fun AttachSemanticsOwner(resolver: SemanticPathResolver)

/**
 * Encodes [imageBitmap] to a PNG [ByteArray].
 * Returns null when encoding is not supported on the current platform.
 */
internal expect fun encodePng(imageBitmap: ImageBitmap): ByteArray?

/**
 * Installs a platform-level observer that fires [onEvent] whenever the app
 * foregrounds, backgrounds, or a new screen becomes active.
 *
 * Platform behaviour:
 * - **Android** — `Application.ActivityLifecycleCallbacks`; emits [AppForeground]
 *   when the first activity resumes, [AppBackground] when the last pauses, and
 *   [ScreenView] on every `onActivityResumed` call.
 * - **iOS** — `NSNotificationCenter`; emits [AppForeground]/[AppBackground] via
 *   `UIApplicationDidBecomeActiveNotification` / `UIApplicationDidEnterBackgroundNotification`.
 * - **WasmJs** — `document.visibilitychange`; emits [AppForeground]/[AppBackground].
 * - **Desktop** — no-op; use [com.himanshoe.tracey.Tracey.log] or
 *   [com.himanshoe.tracey.Tracey.screen] manually.
 *
 * Called once from [com.himanshoe.tracey.Tracey.install] when
 * [com.himanshoe.tracey.TraceyConfig.trackLifecycle] is `true`.
 *
 * [AppForeground]: com.himanshoe.tracey.model.InteractionEvent.AppForeground
 * [AppBackground]: com.himanshoe.tracey.model.InteractionEvent.AppBackground
 * [ScreenView]: com.himanshoe.tracey.model.InteractionEvent.ScreenView
 */
internal expect fun installLifecycleObserver(onEvent: (InteractionEvent) -> Unit)
