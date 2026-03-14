# Firebase Crashlytics Reporter

This example shows how to send Tracey's interaction timeline to Firebase Crashlytics as custom log lines and keys, so you see the user's exact journey when a crash report arrives.

## Setup

Add the Firebase Crashlytics dependency to your app module, then implement `TraceyReporter`:

```kotlin
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.himanshoe.tracey.model.ReplayPayload
import com.himanshoe.tracey.reporter.TraceyReporter

class FirebaseCrashlyticsReporter : TraceyReporter {

    override suspend fun onReplayReady(payload: ReplayPayload) {
        val crashlytics = FirebaseCrashlytics.getInstance()

        // Attach session metadata as custom keys
        crashlytics.setCustomKey("tracey_session_id", payload.sessionId)
        crashlytics.setCustomKey("tracey_duration_ms", payload.durationMs)
        crashlytics.setCustomKey("tracey_event_count", payload.events.size)

        // Log each event as a breadcrumb line
        payload.events.forEach { event ->
            crashlytics.log(event.path)
        }

        // If this was triggered by a crash, record the exception
        if (payload.isCrashPayload && payload.crashReason != null) {
            crashlytics.recordException(Exception(payload.crashReason))
        }
    }
}
```

## Register

```kotlin
Tracey.install(
    TraceyConfig(
        reporters = listOf(
            LogcatReporter(),           // development
            FirebaseCrashlyticsReporter(), // production
        )
    )
)
```

## What you get in Crashlytics

Each Crashlytics report will include:
- **Custom keys**: `tracey_session_id`, `tracey_duration_ms`, `tracey_event_count`
- **Log lines**: the full interaction path for every event, in order (e.g. `HomeScreen > ProductList > ProductCard[2]`)

On the next launch after a crash, Tracey replays the pre-crash payload and `FirebaseCrashlyticsReporter.onReplayReady` is called with `payload.isCrashPayload == true`, so the replay is recorded as a non-fatal exception tied to the crash session.
