package com.himanshoe.tracey.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * Platform and hardware metadata captured at the moment Tracey installs.
 *
 * Every [ReplayPayload] carries one [DeviceInfo] so consumers can correlate
 * replays to specific devices, OS versions, and screen dimensions without
 * needing a separate device database.
 *
 * @property platform       Human-readable platform name: `"Android"`, `"iOS"`,
 *                          `"Desktop"`, or `"Web"`.
 * @property model          Device model name (e.g. `"Samsung Galaxy S23"`,
 *                          `"iPhone 15 Pro"`, `"macOS"`).
 * @property osVersion      Operating system version string.
 * @property screenWidthPx  Physical screen width in pixels.
 * @property screenHeightPx Physical screen height in pixels.
 * @property density        Logical screen density (dp scaling factor).
 * @property appVersion     The host app's version name (e.g. `"2.4.1"`).
 * @property appPackage     The host app's package / bundle identifier.
 * @property locale         Active locale tag (e.g. `"en-US"`).
 */
@Immutable
@Serializable
data class DeviceInfo(
    val platform: String,
    val model: String,
    val osVersion: String,
    val screenWidthPx: Int,
    val screenHeightPx: Int,
    val density: Float,
    val appVersion: String,
    val appPackage: String,
    val locale: String,
)
