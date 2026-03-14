# Sentry Reporter

This example shows how to attach Tracey's interaction timeline to Sentry as breadcrumbs and event tags.

## Setup

Add the Sentry Android SDK dependency to your app module, then implement `TraceyReporter`:

```kotlin
import com.himanshoe.tracey.model.InteractionEvent
import com.himanshoe.tracey.model.ReplayPayload
import com.himanshoe.tracey.reporter.TraceyReporter
import io.sentry.Breadcrumb
import io.sentry.Sentry
import io.sentry.SentryEvent
import io.sentry.SentryLevel
import io.sentry.protocol.Message

class SentryReporter : TraceyReporter {

    override suspend fun onReplayReady(payload: ReplayPayload) {
        // Add session metadata as Sentry tags
        Sentry.setTag("tracey.session_id", payload.sessionId)
        Sentry.setTag("tracey.app_version", payload.appVersion)

        // Push each event as a Sentry breadcrumb
        payload.events.forEach { event ->
            val breadcrumb = Breadcrumb().apply {
                type = event.sentryType()
                message = event.path
                level = SentryLevel.INFO
                setData("timestamp_ms", event.timestampMs.toString())
            }
            Sentry.addBreadcrumb(breadcrumb)
        }

        // Capture as a Sentry event when triggered by a crash replay
        if (payload.isCrashPayload && payload.crashReason != null) {
            val event = SentryEvent().apply {
                message = Message().apply { formatted = payload.crashReason }
                level = SentryLevel.FATAL
                setTag("tracey.crash_replay", "true")
            }
            Sentry.captureEvent(event)
        }
    }

    private fun InteractionEvent.sentryType(): String = when (this) {
        is InteractionEvent.Click, is InteractionEvent.LongPress -> "user"
        is InteractionEvent.Scroll, is InteractionEvent.Swipe, is InteractionEvent.Pinch -> "user"
        is InteractionEvent.ScreenView -> "navigation"
        is InteractionEvent.AppForeground, is InteractionEvent.AppBackground -> "app"
        is InteractionEvent.Breadcrumb -> "default"
    }
}
```

## Register

```kotlin
Tracey.install(
    TraceyConfig(
        reporters = listOf(
            LogcatReporter(),  // development
            SentryReporter(),  // production
        )
    )
)
```

## What you get in Sentry

- **Tags**: `tracey.session_id` and `tracey.app_version` appear on every issue.
- **Breadcrumbs**: the full interaction path up to the crash, in order, visible in the Sentry issue detail view.
- **Crash replays**: replayed as a separate `FATAL` Sentry event on the next launch.
