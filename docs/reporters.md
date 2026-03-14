# Reporters

## Built-in reporters

### LogcatReporter

Prints a structured timeline block to Logcat (Android) or stdout (iOS/Desktop). Useful during development.

```kotlin
TraceyConfig(reporters = listOf(LogcatReporter()))
```

Example output:

```
D/Tracey: ══ Tracey Replay ══════════════════════
          Session  : a1b2c3d4-e5f6-7890-abcd-ef1234567890
          App      : 1.0.0
          Device   : Pixel 8 (Android)
          OS       : 14
          Duration : 8430ms
          Events   : 7
          ══ Timeline (local time) ═════════════════
          14:32:01.000  SCREEN     HomeScreen
          14:32:01.850  CLICK      HomeScreen > SearchBar
          14:32:02.310  CLICK      HomeScreen > ProductCard[0]
          14:32:03.100  SCREEN     DetailScreen
          14:32:04.200  SCROLL     DetailScreen > LazyColumn (Δx=0.0, Δy=-340.5)
          14:32:05.800  CLICK      DetailScreen > AddToCartButton
          14:32:06.500  LOG        Added item to cart: SKU-1234
```

On a crash, the **next** launch replays the pre-crash buffer automatically:

```
D/Tracey: 💥 ══ CRASH REPLAY FROM PREVIOUS SESSION ══ 💥
          💥 Crash    : NullPointerException at CheckoutViewModel.kt:42
          ...
          14:32:06.500  💥 CRASH   NullPointerException at CheckoutViewModel.kt:42
```

### FileReporter

Saves the `ReplayPayload` as a JSON file to platform storage.

```kotlin
TraceyConfig(reporters = listOf(FileReporter()))
```

---

## Event type reference

| Prefix | Event |
|--------|-------|
| `CLICK` | Tap / click |
| `LONG PRESS` | Long press (includes hold duration) |
| `SCROLL` | Scroll (Δx / Δy delta) |
| `SWIPE` | Swipe (start → end coordinates) |
| `PINCH` | Pinch-to-zoom (zoom multiplier) |
| `SCREEN` | Screen view / navigation |
| `LOG` | Custom breadcrumb |
| `FOREGROUND` / `BACKGROUND` | App lifecycle |
| `💥 CRASH` | Crash marker (crash replays only) |

---

## Custom reporters

Implement `TraceyReporter` to route sessions to any destination:

```kotlin
class CrashlyticsReporter : TraceyReporter {
    override suspend fun onReplayReady(payload: ReplayPayload) {
        val crashlytics = FirebaseCrashlytics.getInstance()
        payload.events.forEach { event ->
            crashlytics.log(event.path)
        }
        if (payload.isCrashPayload) {
            crashlytics.recordException(Exception(payload.crashReason))
        }
    }
}
```

Then register it in your config:

```kotlin
TraceyConfig(
    reporters = listOf(
        LogcatReporter(),        // keep for development
        CrashlyticsReporter(),   // add for production
    )
)
```

Multiple reporters run concurrently via coroutines — a failure in one does not affect the others.

---

## Full examples

- [Firebase Crashlytics](reporters/firebase-crashlytics.md)
- [Sentry](reporters/sentry.md)
- [Slack webhook](reporters/slack.md)
