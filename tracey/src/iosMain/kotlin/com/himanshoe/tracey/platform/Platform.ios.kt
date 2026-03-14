package com.himanshoe.tracey.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import com.himanshoe.tracey.model.DeviceInfo
import com.himanshoe.tracey.model.InteractionEvent
import com.himanshoe.tracey.recording.SemanticPathResolver
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.refTo
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import platform.posix.memcpy
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGBitmapContextCreateImage
import platform.CoreGraphics.CGColorSpaceCreateDeviceRGB
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.NSDate
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSLocale
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUUID
import platform.Foundation.NSUserDomainMask
import platform.Foundation.currentLocale
import platform.Foundation.dataWithBytes
import platform.Foundation.languageCode
import platform.Foundation.timeIntervalSince1970
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import platform.UIKit.UIDevice
import platform.UIKit.UIImage
import platform.UIKit.UIImagePNGRepresentation
import platform.UIKit.UIScreen

@OptIn(ExperimentalForeignApi::class)
internal actual fun getDeviceInfo(): DeviceInfo {
    val device = UIDevice.currentDevice
    val screen = UIScreen.mainScreen
    val scale = screen.scale
    val bounds = screen.bounds

    return DeviceInfo(
        platform = "iOS",
        model = device.model,
        osVersion = "${device.systemName} ${device.systemVersion}",
        screenWidthPx = (bounds.useContents { size.width } * scale).toInt(),
        screenHeightPx = (bounds.useContents { size.height } * scale).toInt(),
        density = scale.toFloat(),
        appVersion = appVersion(),
        appPackage = appPackage(),
        locale = NSLocale.currentLocale.languageCode ?: "en",
    )
}

internal actual fun currentEpochMillis(): Long =
    (NSDate().timeIntervalSince1970 * 1000).toLong()

internal actual fun generateUUID(): String =
    NSUUID().UUIDString()

internal actual fun platformLog(tag: String, message: String) {
    println("[$tag] $message")
}

@OptIn(ExperimentalForeignApi::class)
internal actual fun saveToDisk(key: String, json: String) {
    val path = traceyDir() ?: return
    val filePath = "$path/$key.json"
    json.encodeToByteArray().let { bytes ->
        NSFileManager.defaultManager.createFileAtPath(
            path = filePath,
            contents = bytes.toNSData(),
            attributes = null,
        )
    }
}

@OptIn(ExperimentalForeignApi::class)
internal actual fun loadFromDisk(key: String): String? {
    val path = traceyDir() ?: return null
    val filePath = "$path/$key.json"
    return runCatching {
        NSFileManager.defaultManager.contentsAtPath(filePath)
            ?.toByteArray()
            ?.decodeToString()
    }.getOrNull()
}

@OptIn(ExperimentalForeignApi::class)
internal actual fun deleteFromDisk(key: String) {
    val path = traceyDir() ?: return
    val filePath = "$path/$key.json"
    runCatching {
        NSFileManager.defaultManager.removeItemAtPath(filePath, null)
    }
}

@OptIn(kotlin.experimental.ExperimentalNativeApi::class)
internal actual fun installUncaughtExceptionHandler(onCrash: (Throwable) -> Unit) {
    val previous = getUnhandledExceptionHook()
    setUnhandledExceptionHook { throwable ->
        runCatching { onCrash(throwable) }
        previous?.invoke(throwable)
    }
}

internal actual fun appVersion(): String = runCatching {
    NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: "unknown"
}.getOrElse { "unknown" }

internal actual fun appPackage(): String = runCatching {
    NSBundle.mainBundle.bundleIdentifier ?: "unknown"
}.getOrElse { "unknown" }

@OptIn(ExperimentalForeignApi::class)
private fun traceyDir(): String? = runCatching {
    val paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
    val documents = paths.firstOrNull() as? String ?: return null
    val dir = "$documents/tracey"
    NSFileManager.defaultManager.createDirectoryAtPath(dir, true, null, null)
    dir
}.getOrNull()

/**
 * `toPixelMap()` copies pixel data to the Kotlin heap via Compose's common API.
 * We then pin that buffer in native memory (`memScoped`), hand it to a CGBitmapContext
 * (CoreGraphics reads it in-place without copying), and snapshot it into a CGImage
 * before the scope — and therefore the native pin — is released.
 * UIImagePNGRepresentation compresses the CGImage to PNG in memory.
 *
 * This involves an O(w×h) pixel loop on the Kotlin side; acceptable for debug captures.
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalForeignApi::class)
internal actual fun encodePng(imageBitmap: ImageBitmap): ByteArray? = runCatching {
    val pixelMap = imageBitmap.toPixelMap()
    val width = pixelMap.width
    val height = pixelMap.height
    val bytesPerRow = width * 4
    val rawBytes = ByteArray(height * bytesPerRow)
    for (y in 0 until height) {
        for (x in 0 until width) {
            val color = pixelMap[x, y]
            val i = (y * width + x) * 4
            rawBytes[i]     = (color.red   * 255f + 0.5f).toInt().coerceIn(0, 255).toByte()
            rawBytes[i + 1] = (color.green * 255f + 0.5f).toInt().coerceIn(0, 255).toByte()
            rawBytes[i + 2] = (color.blue  * 255f + 0.5f).toInt().coerceIn(0, 255).toByte()
            rawBytes[i + 3] = (color.alpha * 255f + 0.5f).toInt().coerceIn(0, 255).toByte()
        }
    }
    memScoped {
        val colorSpace = CGColorSpaceCreateDeviceRGB()
        val context = if (colorSpace != null) CGBitmapContextCreate(
            data = rawBytes.refTo(0),
            width = width.toULong(),
            height = height.toULong(),
            bitsPerComponent = 8u,
            bytesPerRow = bytesPerRow.toULong(),
            space = colorSpace,
            bitmapInfo = 1u, // kCGImageAlphaPremultipliedLast
        ) else null
        val cgImage = context?.let { CGBitmapContextCreateImage(it) }
        val uiImage = cgImage?.let { UIImage.imageWithCGImage(it) }
        uiImage?.let { UIImagePNGRepresentation(it)?.toByteArray() }
    }
}.getOrNull()

internal actual fun installLifecycleObserver(onEvent: (InteractionEvent) -> Unit) {
    NSNotificationCenter.defaultCenter.apply {
        addObserverForName(UIApplicationDidBecomeActiveNotification, null, NSOperationQueue.mainQueue) { _ ->
            onEvent(InteractionEvent.AppForeground(currentEpochMillis()))
        }
        addObserverForName(UIApplicationDidEnterBackgroundNotification, null, NSOperationQueue.mainQueue) { _ ->
            onEvent(InteractionEvent.AppBackground(currentEpochMillis()))
        }
    }
}

/**
 * iOS does not expose the Compose semantics tree at the Kotlin/Native layer without
 * internal API access. Path resolution instead uses the current screen name set by
 * [com.himanshoe.tracey.Tracey.screen] (or the [com.himanshoe.tracey.TraceyScreen]
 * composable), producing paths like `"HomeScreen > Screen[x=123,y=456]"`.
 *
 * No semantics owner attachment is needed — [SemanticPathResolver.currentScreenName]
 * is kept up to date by [com.himanshoe.tracey.Tracey.screen].
 */
@Composable
internal actual fun AttachSemanticsOwner(resolver: SemanticPathResolver) = Unit

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData =
    usePinned { NSData.dataWithBytes(it.addressOf(0), length = this.size.toULong()) }

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = this.length.toInt()
    if (size == 0) return ByteArray(0)
    val result = ByteArray(size)
    val src = this.bytes ?: return result
    result.usePinned { memcpy(it.addressOf(0), src, this.length) }
    return result
}
