# Setup

## Minimal setup

Call `Tracey.install` once, before any UI is shown — typically in `Application.onCreate()` on Android or the app entry point on iOS:

```kotlin
Tracey.install(
    TraceyConfig(
        reporters = listOf(LogcatReporter()),
    )
)
```

---

## Full gesture recording

Wrap your root composable with `TraceyHost` to enable gesture capture:

```kotlin
setContent {
    TraceyHost(traceyConfig = rememberTraceyConfig(reporters = listOf(LogcatReporter()))) {
        MyApp()
    }
}
```

> Without `TraceyHost`, lifecycle events, screen views, breadcrumbs, and crash recovery still work — gestures are not captured.

---

## TraceyConfig reference

```kotlin
TraceyConfig(
    enabled                = true,           // master on/off switch
    showOverlay            = false,          // debug gesture-trail overlay
    bufferDurationSeconds  = 60,             // max seconds of history kept
    maxEvents              = 500,            // max events in the ring buffer
    reporters              = listOf(LogcatReporter()),
    redactedTags           = emptyList(),    // testTags whose events are dropped
    trackLifecycle         = true,           // foreground/background events
    generateHtmlReport     = false,          // include HTML in ReplayPayload
    sessionIdProvider      = { UUID.randomUUID().toString() },
)
```

---

## rememberTraceyConfig

Use `rememberTraceyConfig` inside a composable to create and memoize a config:

```kotlin
val config = rememberTraceyConfig(
    reporters = listOf(LogcatReporter()),
    showOverlay = BuildConfig.DEBUG,
)
TraceyHost(traceyConfig = config) {
    MyApp()
}
```
