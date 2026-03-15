package com.himanshoe.tracey.platform

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.SemanticsOwner
import com.himanshoe.tracey.model.DeviceInfo
import com.himanshoe.tracey.model.InteractionEvent
import com.himanshoe.tracey.recording.SemanticPathResolver
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Locale
import java.util.UUID

/**
 * Android-specific holder for the application [Context].
 * Populated automatically by [TraceyInitProvider] before any application
 * code runs, so callers never need to pass a context manually.
 */
internal object AndroidContext {
    lateinit var app: Context
    val isInitialized get() = ::app.isInitialized
}

internal actual fun getDeviceInfo(): DeviceInfo {
    val ctx = AndroidContext.app
    val packageInfo = runCatching {
        ctx.packageManager.getPackageInfo(ctx.packageName, 0)
    }.getOrNull()

    val displayMetrics = ctx.resources.displayMetrics

    return DeviceInfo(
        platform = "Android",
        model = "${Build.MANUFACTURER} ${Build.MODEL}",
        osVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
        screenWidthPx = displayMetrics.widthPixels,
        screenHeightPx = displayMetrics.heightPixels,
        density = displayMetrics.density,
        appVersion = packageInfo?.versionName ?: "unknown",
        appPackage = ctx.packageName,
        locale = Locale.getDefault().toLanguageTag(),
    )
}

internal actual fun currentEpochMillis(): Long = System.currentTimeMillis()

internal actual fun generateUUID(): String = UUID.randomUUID().toString()

internal actual fun platformLog(tag: String, message: String) {
    Log.d(tag, message)
}

internal actual fun saveToDisk(key: String, json: String) {
    if (!AndroidContext.isInitialized) return
    val dir = File(AndroidContext.app.filesDir, "tracey").apply { mkdirs() }
    File(dir, "$key.json").writeText(json)
}

internal actual fun loadFromDisk(key: String): String? {
    if (!AndroidContext.isInitialized) return null
    val file = File(AndroidContext.app.filesDir, "tracey/$key.json")
    return if (file.exists()) file.readText() else null
}

internal actual fun deleteFromDisk(key: String) {
    if (!AndroidContext.isInitialized) return
    File(AndroidContext.app.filesDir, "tracey/$key.json").delete()
}

internal actual fun installUncaughtExceptionHandler(onCrash: (Throwable) -> Unit) {
    val previous = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        runCatching { onCrash(throwable) }
        previous?.uncaughtException(thread, throwable)
    }
}

internal actual fun appVersion(): String {
    if (!AndroidContext.isInitialized) return "unknown"
    return runCatching {
        AndroidContext.app.packageManager
            .getPackageInfo(AndroidContext.app.packageName, 0)
            .versionName ?: "unknown"
    }.getOrElse { "unknown" }
}

internal actual fun appPackage(): String {
    return if (AndroidContext.isInitialized) AndroidContext.app.packageName else "unknown"
}

@Composable
internal actual fun AttachSemanticsOwner(resolver: SemanticPathResolver) {
    val view = LocalView.current
    DisposableEffect(view) {
        val semanticsOwner = runCatching {
            view.javaClass.getMethod("getSemanticsOwner").invoke(view) as? SemanticsOwner
        }.getOrNull()
        semanticsOwner?.let { resolver.attach(it) }
        onDispose { resolver.detach() }
    }
}

internal actual fun encodePng(imageBitmap: ImageBitmap): ByteArray? = runCatching {
    val stream = ByteArrayOutputStream()
    imageBitmap.asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
    stream.toByteArray()
}.getOrNull()

internal actual fun installLifecycleObserver(onEvent: (InteractionEvent) -> Unit) {
    val app = AndroidContext.app as? android.app.Application ?: return
    var resumedCount = 0
    app.registerActivityLifecycleCallbacks(object : android.app.Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(a: android.app.Activity, b: android.os.Bundle?) = Unit
        override fun onActivityStarted(a: android.app.Activity) = Unit
        override fun onActivityResumed(a: android.app.Activity) {
            // Only track foreground/background transitions. Composable screen names
            // are captured via TraceyScreen() or Tracey.screen() instead, since
            // Activity class names are meaningless in single-Activity Compose apps.
            if (resumedCount == 0) onEvent(InteractionEvent.AppForeground(currentEpochMillis()))
            resumedCount++
        }
        override fun onActivityPaused(a: android.app.Activity) {
            resumedCount = maxOf(0, resumedCount - 1)
            if (resumedCount == 0) onEvent(InteractionEvent.AppBackground(currentEpochMillis()))
        }
        override fun onActivityStopped(a: android.app.Activity) = Unit
        override fun onActivitySaveInstanceState(a: android.app.Activity, out: android.os.Bundle) = Unit
        override fun onActivityDestroyed(a: android.app.Activity) = Unit
    })
}
