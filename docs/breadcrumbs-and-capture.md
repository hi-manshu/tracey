# Breadcrumbs and capture

## Breadcrumbs

Log arbitrary string events from anywhere — API errors, feature flag decisions, cart updates:

```kotlin
Tracey.log("Added item to cart: SKU-1234")
Tracey.log("Payment flow started")
Tracey.log("Feature flag: new_checkout_enabled=true")
```

Breadcrumbs appear in the replay timeline with a `LOG` prefix.

---

## Capture

### Fire all reporters

Delivers the current event buffer to every registered `TraceyReporter`:

```kotlin
Tracey.capture()
```

### Export as a Compose UI test

Returns the session as a runnable Kotlin test string — useful for reproducing bugs in automated tests:

```kotlin
val testCode: String = Tracey.captureAndExportTest()
```

### Export to disk as JSON

Saves the full `ReplayPayload` as a JSON file to platform storage and returns the file path:

```kotlin
val path: String? = Tracey.captureAndExportFile()
```

| Platform | Location |
|---|---|
| Android | `filesDir/tracey/replay_<sessionId>.json` |
| iOS | `Documents/tracey/replay_<sessionId>.json` |

### Export as HTML

Returns an interactive HTML report as a string (only populated when `generateHtmlReport = true` in `TraceyConfig`):

```kotlin
val html: String = Tracey.captureAndExportHtml()
```
