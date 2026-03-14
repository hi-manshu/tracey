# Tracey

A Kotlin Multiplatform SDK for recording user interactions — gestures, screen views, breadcrumbs, and crashes — and replaying them as a timeline on Android and iOS.

[![Maven Central](https://img.shields.io/maven-central/v/com.himanshoe/tracey?label=Maven%20Central)](https://central.sonatype.com/artifact/com.himanshoe/tracey)
[![Buy Me a Coffee](https://img.shields.io/badge/Buy%20Me%20a%20Coffee-support-yellow?logo=buy-me-a-coffee)](https://buymeacoffee.com/himanshoe)

## What it does

- Records clicks, scrolls, swipes, long presses, and pinches with zero instrumentation
- Tracks screen views and custom breadcrumbs from anywhere in your app
- Captures a ring-buffer snapshot on crash and replays it on next launch
- Routes captured sessions to any reporter (Logcat, Slack, webhook, etc.)

## Installation

Add the dependency to your shared module's `build.gradle.kts`:

```kotlin
commonMain.dependencies {
    implementation("com.himanshoe:tracey:0.1.0")
}
```

For automatic screen tracking with Compose Navigation, also add:

```kotlin
commonMain.dependencies {
    implementation("com.himanshoe:tracey:0.1.0")
    implementation("com.himanshoe:tracey-navigation:0.1.0")
    implementation("org.jetbrains.androidx.navigation:navigation-compose:<version>")
}
```

> `tracey-navigation` does not expose `navigation-compose` transitively — declare it explicitly.

## Setup

```kotlin
// Application.onCreate() or iOS AppDelegate
Tracey.install(
    TraceyConfig(
        reporters = listOf(LogcatReporter()),
    )
)
```

For full gesture recording, wrap your root composable with `TraceyHost`:

```kotlin
setContent {
    TraceyHost(traceyConfig = rememberTraceyConfig(reporters = listOf(LogcatReporter()))) {
        MyApp()
    }
}
```

## Screen tracking

**With Compose Navigation** (`tracey-navigation` module):

```kotlin
// Declare both dependencies — tracey-navigation does not expose them transitively
implementation("com.himanshoe:tracey-navigation:<version>")
implementation("org.jetbrains.androidx.navigation:navigation-compose:<version>")

// Then swap rememberNavController() for:
val navController = rememberTraceyNavController()
```

**Manual:**

```kotlin
Tracey.route("HomeScreen")   // from a nav listener
Tracey.screen("HomeScreen")  // from a composable or anywhere
TraceyScreen("HomeScreen")   // composable helper
```

## Breadcrumbs

```kotlin
Tracey.log("Added item to cart: SKU-1234")
```

## Capture

```kotlin
Tracey.capture()                  // fires all reporters
Tracey.captureAndExportTest()     // returns a Compose UI test as a Kotlin string
Tracey.captureAndExportFile()     // saves JSON to platform storage
```

## Privacy

Mark sensitive composables so their region is replaced with a solid rectangle in screenshots:

```kotlin
TextField(
    value = cardNumber,
    onValueChange = { cardNumber = it },
    modifier = Modifier
        .fillMaxWidth()
        .traceyMask(),           // black by default
)

// Custom colour
Text(ssn, modifier = Modifier.traceyMask(maskColor = Color.Red))
```

The composable renders normally on screen — only captured screenshots are affected.

## Custom reporters

Tracey ships with `LogcatReporter` for development. For production, implement `TraceyReporter` to send data anywhere:

```kotlin
class CrashlyticsReporter : TraceyReporter {
    override suspend fun onReplayReady(payload: ReplayPayload) {
        val crashlytics = // Any crashlytics
        payload.events.forEach { event ->
            crashlytics.log(event.path)
        }
        if (payload.isCrashPayload) {
            crashlytics.recordException(Exception(payload.crashReason))
        }
    }
}
```

See [`docs/reporters/`](docs/reporters/) for full examples — Firebase Crashlytics, Sentry, and Slack.

## Platforms

| Platform | Gestures | Lifecycle | Crash | Event paths |
|----------|:--------:|:---------:|:-----:|-------------|
| Android  | ✅       | ✅        | ✅    | Full composable path — `HomeScreen > AddToCartButton` |
| iOS      | ✅       | ✅        | ✅    | Screen + coordinates — `HomeScreen > Screen[x=150,y=300]` |

On iOS, Compose does not expose the semantics tree at the Kotlin/Native layer, so event paths use screen name and coordinates rather than composable names. Use `Tracey.screen()` or `TraceyScreen()` to at minimum capture the screen name in every path.

> Gesture recording requires `TraceyHost`. Without it, lifecycle events, screen views, breadcrumbs, and crash recovery still work — but clicks/scrolls/swipes are not captured.

## License

```
Copyright 2026 Himanshu Singh

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
