# Slack Reporter

This example shows how to POST Tracey's interaction timeline to a Slack channel via an incoming webhook. Useful for QA sessions — tap the capture button and the timeline immediately lands in Slack.

## Setup

Create an [incoming webhook](https://api.slack.com/messaging/webhooks) in your Slack workspace, then implement `TraceyReporter` using your preferred HTTP client (Ktor shown here):

```kotlin
import com.himanshoe.tracey.model.ReplayPayload
import com.himanshoe.tracey.reporter.TraceyReporter
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class SlackReporter(
    private val webhookUrl: String,
    private val httpClient: HttpClient,
) : TraceyReporter {

    override suspend fun onReplayReady(payload: ReplayPayload) {
        val emoji = if (payload.isCrashPayload) "💥" else "📋"
        val title = if (payload.isCrashPayload) {
            "$emoji CRASH REPLAY — ${payload.crashReason}"
        } else {
            "$emoji Tracey capture — session `${payload.sessionId}`"
        }

        val body = buildString {
            appendLine(title)
            appendLine("App: `${payload.appVersion}`  Device: `${payload.deviceInfo.model}` (${payload.deviceInfo.platform})")
            appendLine("Duration: ${payload.durationMs}ms  Events: ${payload.events.size}")
            appendLine("```")
            appendLine(payload.timeline)
            appendLine("```")
        }

        httpClient.post(webhookUrl) {
            contentType(ContentType.Application.Json)
            setBody("""{"text":${body.trimEnd().let { kotlinx.serialization.json.Json.encodeToString(it) }}}""")
        }
    }
}
```

## Register

```kotlin
val httpClient = HttpClient()

Tracey.install(
    TraceyConfig(
        reporters = listOf(
            LogcatReporter(),                          // development
            SlackReporter(                             // QA / staging
                webhookUrl = "https://hooks.slack.com/services/...",
                httpClient = httpClient,
            ),
        )
    )
)
```

## What you get in Slack

A message per capture containing:
- Session ID, app version, device model
- Duration and event count
- The full interaction timeline in a code block

On crash replay the message is prefixed with 💥 and the crash reason is shown in the title.
