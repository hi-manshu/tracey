# Tracey

A Kotlin Multiplatform (KMP) / Compose Multiplatform SDK for recording, replaying, and reporting user interactions — clicks, scrolls, swipes, gestures, lifecycle events, breadcrumbs, and crashes — on Android and iOS.

## Platform support

| Platform | Gestures | Lifecycle | Screenshot | Crash | Composable paths |
|----------|----------|-----------|------------|-------|-----------------|
| Android  | ✅       | ✅        | ✅         | ✅    | Full semantics tree |
| iOS      | ✅       | ✅        | ✅         | ✅    | Screen-prefixed coordinates¹ |

¹ On iOS the Compose semantics tree is not accessible without internal APIs. Tracey uses the current screen name (set via `TraceyScreen` or `Tracey.screen()`) as a path prefix, producing paths like `"HomeScreen > Screen[x=123,y=456]"`. Full named-node paths are available on Android.

---

## Features

- **Zero-instrumentation gesture recording** — one `Modifier` at the root intercepts all clicks, scrolls, swipes, long presses, and pinches.
- **Lifecycle tracking** — app foreground/background and screen-level events recorded automatically.
- **Custom breadcrumbs** — log arbitrary messages from ViewModels, repositories, and network layers.
- **Screenshot on capture** — a PNG snapshot of the screen is attached to every `ReplayPayload`.
- **Crash recovery** — the last ring-buffer snapshot is saved to disk on crash and replayed on next launch.
- **Configurable ring buffer** — retain the last N seconds or last N events, whichever comes first.
- **Privacy redaction** — mark composables with `testTag` to silently drop their events.
- **Pluggable reporters** — route captured data anywhere (Logcat, files, Slack, webhook, etc.).
- **Compose UI test export** — generate a runnable Compose UI test from any captured session.

---

## Modules

| Artifact | Description |
|----------|-------------|
| `com.himanshoe:tracey` | Core SDK — gesture recording, lifecycle, crash recovery, exporters |
| `com.himanshoe:tracey-navigation` | Compose Multiplatform Navigation integration — automatic screen tracking |

---

## Installation

Add to your shared module's `build.gradle.kts`:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("com.himanshoe:tracey:<version>")

            // Optional — only if you use Compose Multiplatform Navigation
            implementation("com.himanshoe:tracey-navigation:<version>")
        }
    }
}
```

> No additional Android `ContentProvider` or iOS `AppDelegate` wiring is required — Tracey bootstraps itself automatically on Android via a `ContentProvider`, and the iOS lifecycle observer is installed when you call `Tracey.install()`.

---

## Quick start

### 1 — Wrap your root composable

Use `rememberTraceyConfig` to configure Tracey and pass it to `TraceyHost`. No separate install call needed.

```kotlin
// Android — MainActivity.setContent { }
setContent {
    TraceyHost(
        traceyConfig = rememberTraceyConfig(
            showOverlay = BuildConfig.DEBUG,   // overlay in debug only
            reporters   = listOf(LogcatReporter()),
        )
    ) {
        MyApp()
    }
}
```

### 1b — Non-Compose entry points (optional)

To catch crashes before the first composition (e.g. during app startup), call `Tracey.install()` with a `TraceyConfig`. `TraceyHost` detects this and skips re-installation.

```kotlin
// Android — Application.onCreate()
Tracey.install(TraceyConfig(reporters = listOf(LogcatReporter())))

// iOS — application(_:didFinishLaunchingWithOptions:)
Tracey.install(TraceyConfig(reporters = listOf(LogcatReporter())))
```

### 2 — Track screens

**Option A — Compose Multiplatform Navigation** (`tracey-navigation` module)

Swap `rememberNavController()` for `rememberTraceyNavController()`. Screen changes are then tracked automatically from your nav graph routes:

```kotlin
val navController = rememberTraceyNavController()

NavHost(navController, startDestination = "HomeScreen") {
    composable("HomeScreen")   { HomeScreen() }
    composable("ProfileScreen") { ProfileScreen() }
    composable("CheckoutScreen") { CheckoutScreen() }
}
```

Or attach to an existing `NavController`:

```kotlin
val navController = rememberNavController()
navController.trackWithTracey()
```

**Option B — Manual `TraceyScreen`** (no navigation library)

Place `TraceyScreen` at the top of each screen composable. On iOS this also sets the path prefix so gestures appear as `"HomeScreen > Screen[x=N,y=M]"`.

```kotlin
@Composable
fun HomeScreen() {
    TraceyScreen("HomeScreen")
    // ... your content
}
```

### 3 — Capture

```kotlin
// From a coroutine, LaunchedEffect, ViewModel, or shake handler:
Tracey.capture()

// Export as a Compose UI test:
val testCode: String = Tracey.captureAndExportTest()

// Save to platform storage:
val result: Result<String> = Tracey.captureAndExportFile()
```

---

## Configuration reference

All config lives in `TraceyConfig`. In Compose, create it via `rememberTraceyConfig` — it's stable across recompositions. For non-Compose entry points, pass `TraceyConfig` directly to `Tracey.install()`.

| Parameter | Default | Description |
|-----------|---------|-------------|
| `enabled` | `true` | Master switch — `false` is a complete no-op |
| `showOverlay` | `false` | Floating debug panel. Pass `BuildConfig.DEBUG` to show in debug only |
| `bufferDurationSeconds` | `30` | Seconds of history to keep in the ring buffer |
| `maxEvents` | `500` | Hard cap on stored event count |
| `reporters` | `[]` | Where captured data goes |
| `redactedTags` | `[]` | `testTag` values whose events are silently dropped |
| `trackLifecycle` | `true` | Auto-record foreground/background and screen transitions |
| `generateHtmlReport` | `false` | Auto-save HTML report on every `capture()` call |
| `sessionIdProvider` | `UUID` | Lambda to tie the session to your own analytics ID |

```kotlin
TraceyHost(
    traceyConfig = rememberTraceyConfig(
        enabled               = BuildConfig.DEBUG,
        showOverlay           = BuildConfig.DEBUG,
        bufferDurationSeconds = 60,
        maxEvents             = 1000,
        reporters             = listOf(LogcatReporter()),
        redactedTags          = listOf("PasswordField", "CreditCardInput"),
        sessionIdProvider     = { myAnalytics.sessionId },
    )
) {
    MyApp()
}
```

---

## Lifecycle tracking

When `trackLifecycle = true` (default), Tracey records these events automatically:

| Event | When |
|-------|------|
| `AppForeground` | App became active/visible |
| `AppBackground` | App moved to background |
| `ScreenView` | `TraceyScreen` composable entered, or `Tracey.screen()` called |

### Automatic screen tracking with `tracey-navigation`

```kotlin
// One change — swap rememberNavController() for rememberTraceyNavController()
val navController = rememberTraceyNavController()
NavHost(navController, startDestination = "HomeScreen") { ... }
```

Or attach tracking to an existing controller:

```kotlin
val navController = rememberNavController()
navController.trackWithTracey()
```

### Manual screen tracking

```kotlin
// Composable
TraceyScreen("HomeScreen")

// From anywhere
Tracey.screen("SettingsScreen")

// From a nav destination changed listener
Tracey.route(destination.route ?: return)
```

---

## Custom breadcrumbs

Log arbitrary messages from anywhere — no coroutine required:

```kotlin
// In a ViewModel
fun addToCart(sku: String) {
    Tracey.log("Cart: added $sku")
    repository.addItem(sku)
}

// In a network interceptor
Tracey.log("API error 503 on /checkout")

// In a navigation observer
navController.addOnDestinationChangedListener { _, dest, _ ->
    Tracey.log("Nav → ${dest.route}")
}
```

Breadcrumbs appear inline in the timeline between gesture events:

```
00:12.340  SCREEN     CheckoutScreen
00:14.120  CLK        PlaceOrderButton
00:14.130  LOG        Cart: placed order for SKU-1234
00:14.900  LOG        API error 503 on /checkout
00:15.000  💥 CRASH   NullPointerException at OrderRepository.kt:42
```

---

## Privacy & redaction

Mark sensitive composables with a `testTag` and add it to `redactedTags`. Events on those composables are silently dropped — nothing is recorded, and no placeholder appears in the timeline.

```kotlin
// In your composable:
TextField(
    value = password,
    onValueChange = { password = it },
    modifier = Modifier.testTag("PasswordField"),
)

// In your config:
TraceyConfig(redactedTags = listOf("PasswordField", "CreditCardNumber"))
```

---

## Reporters

### Built-in reporters

| Reporter | Description |
|----------|-------------|
| `LogcatReporter` | Prints the timeline to Logcat / stdout. Debug builds only. |

### Implementing a custom reporter

```kotlin
class SlackReporter(private val webhookUrl: String) : TraceyReporter {
    override suspend fun report(payload: ReplayPayload) {
        val body = buildString {
            appendLine("*Tracey capture* — session `${payload.sessionId}`")
            appendLine("App: ${payload.appVersion}  Device: ${payload.deviceInfo.model}")
            appendLine("```")
            appendLine(payload.timeline)
            appendLine("```")
        }
        // POST body to webhookUrl
    }
}

// Register it:
Tracey.install(
    TraceyConfig(reporters = listOf(SlackReporter(webhookUrl = "https://hooks.slack.com/...")))
)
```

### `ReplayPayload` fields

| Field | Type | Description |
|-------|------|-------------|
| `sessionId` | `String` | Unique session identifier |
| `appVersion` | `String` | App version string |
| `capturedAtMs` | `Long` | Epoch ms of capture |
| `durationMs` | `Long` | Duration from first event to capture |
| `crashReason` | `String?` | Non-null when recovered from a crash |
| `deviceInfo` | `DeviceInfo` | Platform, model, OS, screen, locale |
| `events` | `List<InteractionEvent>` | All events in the ring buffer |
| `timeline` | `String` | Human-readable formatted event log |
| `screenshotPng` | `ByteArray?` | PNG snapshot at capture time (in-memory only) |

---

## Event types

All events implement `InteractionEvent` and are serialisable to JSON.

| Type | Fields | Source |
|------|--------|--------|
| `Click` | `x`, `y`, `path` | Pointer input |
| `LongPress` | `x`, `y`, `holdDurationMs`, `path` | Pointer input |
| `Scroll` | `x`, `y`, `deltaX`, `deltaY`, `velocity*`, `path` | Pointer input |
| `Swipe` | `startX/Y`, `endX/Y`, `velocity*`, `path` | Pointer input |
| `Pinch` | `centroidX/Y`, `zoomDelta`, `rotationDelta`, `path` | Pointer input |
| `AppForeground` | — | Lifecycle observer |
| `AppBackground` | — | Lifecycle observer |
| `ScreenView` | `screenName` | `TraceyScreen` / `Tracey.screen()` / `Tracey.route()` / `rememberTraceyNavController()` |
| `Breadcrumb` | `message` | `Tracey.log()` |

### Path resolution by platform

| Platform | Path format | Example |
|----------|-------------|---------|
| Android | Full semantics breadcrumb | `"HomeScreen > LazyColumn > ProductCard[3] > AddToCartButton"` |
| iOS | Screen-prefixed coordinate | `"HomeScreen > Screen[x=540,y=960]"` |

On iOS, call `TraceyScreen("ScreenName")` (or `Tracey.screen("ScreenName")`) at each screen entry point to get meaningful prefixes. Without it, paths fall back to bare coordinates: `"Screen[x=540,y=960]"`.

---

## Debug overlay

The floating overlay is shown when `showOverlay = true` on `TraceyHost`. Pass `BuildConfig.DEBUG` to keep it out of production.

| Control | Action |
|---------|--------|
| ⏺ red button | Trigger `Tracey.capture()` and fire all reporters |
| ≡ blue button | Toggle the live event log panel |
| Drag either button | Reposition the control cluster |

The overlay renders colour-coded gesture trails that fade after 2 seconds, and a scrollable live event log showing the 50 most recent events.

---

## Architecture

```
TraceyHost(enabled, showOverlay, bufferDurationSeconds, maxEvents, reporters, …)
  └── RecordingEngine (Modifier.pointerInput)
        └── RingBuffer (thread-safe, bounded)
  └── TraceyOverlay (only when showOverlay = true)
        └── OverlayState (live event feed)

Tracey (singleton)
  ├── install(TraceyConfig)  → non-Compose entry point (Application.onCreate, etc.)
  ├── capture()              → snapshots buffer → ReplayPayload → reporters
  ├── log(message)           → Breadcrumb → buffer + overlay
  ├── screen(name)           → ScreenView → buffer + overlay + iOS path prefix
  ├── route(name)            → ScreenView via nav graph route (used by tracey-navigation)
  ├── captureAndExportTest() → Compose UI test as a Kotlin string
  └── captureAndExportFile() → JSON to platform storage

tracey-navigation
  ├── rememberTraceyNavController() → NavHostController with automatic route tracking
  └── NavController.trackWithTracey() → attach tracking to an existing NavController

Platform (expect/actual)
  ├── Android — ContentProvider auto-init, ActivityLifecycleCallbacks, full semantics tree
  └── iOS     — NSNotificationCenter, screen-name-prefixed coordinate paths
```

---

## License

```
Copyright 2024 Himanshu Hoe

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
