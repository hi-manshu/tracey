package com.himanshoe.tracey.export

import com.himanshoe.tracey.model.InteractionEvent
import com.himanshoe.tracey.model.ReplayPayload
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Generates a self-contained HTML report from a [ReplayPayload].
 *
 * The report includes:
 * - A **user flow diagram** — horizontally scrollable screen cards connected by
 *   arrows, each card showing the events that occurred on that screen.
 * - A **screenshot** — the PNG snapshot attached at capture time (if available).
 * - The **full text timeline** — the same string produced by [Tracey.capture].
 *
 * The output is a single `.html` file with no external dependencies. Open it
 * directly in any browser.
 *
 * Obtain via [com.himanshoe.tracey.Tracey.captureAndExportHtml].
 */
internal object ReplayHtmlExporter {

    @OptIn(ExperimentalEncodingApi::class)
    fun export(payload: ReplayPayload): String {
        val screenshotSrc = payload.screenshotPng
            ?.let { "data:image/png;base64,${Base64.encode(it)}" }
        val flow = buildScreenFlow(payload.events)
        val isCrash = payload.crashReason != null

        return buildString {
            appendLine("<!DOCTYPE html>")
            appendLine("<html lang=\"en\">")
            appendLine("<head>")
            appendLine("""  <meta charset="utf-8">""")
            appendLine("""  <meta name="viewport" content="width=device-width,initial-scale=1">""")
            appendLine("  <title>Tracey — ${payload.sessionId.take(8)}</title>")
            appendLine("  <style>${CSS.trimIndent()}</style>")
            appendLine("</head>")
            appendLine("<body>")

            // ── Header ────────────────────────────────────────────────────────
            appendLine("<header>")
            appendLine("""  <div class="badge ${if (isCrash) "badge-crash" else "badge-ok"}">""")
            appendLine(if (isCrash) "  💥 CRASH REPORT" else "  ✅ REPLAY")
            appendLine("  </div>")
            appendLine("  <h1>Tracey Replay</h1>")
            appendLine("""  <div class="meta-grid">""")
            meta("Session", payload.sessionId.take(16) + "…")
            meta("App", payload.appVersion)
            meta("Platform", payload.deviceInfo.platform)
            meta("Device", payload.deviceInfo.model)
            meta("OS", payload.deviceInfo.osVersion)
            meta("Duration", "${payload.durationMs / 1000}s (${payload.events.size} events)")
            appendLine("  </div>")
            if (isCrash) {
                appendLine("""  <div class="crash-box">💥 ${payload.crashReason.orEmpty().escHtml()}</div>""")
            }
            appendLine("</header>")

            // ── User Flow ─────────────────────────────────────────────────────
            appendLine("<section>")
            appendLine("""  <h2>User Flow</h2>""")
            appendLine("""  <div class="flow-scroll"><div class="flow">""")
            flow.forEachIndexed { i, seg ->
                append(seg.toHtml(isLast = i == flow.lastIndex, isCrashSession = isCrash))
                if (i < flow.lastIndex) appendLine("""    <div class="arrow">›</div>""")
            }
            appendLine("  </div></div>")
            appendLine("</section>")

            // ── Screenshot ────────────────────────────────────────────────────
            if (screenshotSrc != null) {
                appendLine("<section>")
                appendLine("""  <h2>Screenshot at Capture</h2>""")
                appendLine("""  <img class="screenshot" src="$screenshotSrc" alt="App screenshot">""")
                appendLine("</section>")
            }

            // ── Full Timeline ─────────────────────────────────────────────────
            appendLine("<section>")
            appendLine("""  <h2>Full Timeline</h2>""")
            appendLine("""  <pre class="timeline">${payload.timeline.escHtml()}</pre>""")
            appendLine("</section>")

            appendLine("</body></html>")
        }
    }

    // ── Screen flow model ─────────────────────────────────────────────────────

    private data class ScreenSegment(
        val name: String,
        val kind: Kind,
        val events: List<InteractionEvent>,
    ) {
        enum class Kind { Screen, Lifecycle, Unknown }

        fun toHtml(isLast: Boolean, isCrashSession: Boolean): String = buildString {
            val isLastCrash = isLast && isCrashSession
            val cardClass = when {
                isLastCrash -> "card card-crash"
                kind == Kind.Lifecycle -> "card card-lifecycle"
                else -> "card"
            }
            appendLine("    <div class=\"$cardClass\">")
            val displayName = if (isLastCrash) "💥 $name" else name
            appendLine("      <div class=\"card-title\">${displayName.escHtml()}</div>")
            val chips = events.mapNotNull { it.toChip() }
            if (chips.isNotEmpty()) {
                appendLine("      <div class=\"chip-row\">")
                chips.forEach { appendLine("        $it") }
                appendLine("      </div>")
            }
            appendLine("    </div>")
        }
    }

    private fun buildScreenFlow(events: List<InteractionEvent>): List<ScreenSegment> {
        val segments = mutableListOf<ScreenSegment>()
        var currentName = "Session Start"
        var currentKind = ScreenSegment.Kind.Unknown
        var currentEvents = mutableListOf<InteractionEvent>()

        fun flush() {
            if (currentEvents.isNotEmpty() || segments.isEmpty()) {
                segments += ScreenSegment(currentName, currentKind, currentEvents.toList())
                currentEvents = mutableListOf()
            }
        }

        for (event in events) {
            when (event) {
                is InteractionEvent.ScreenView -> {
                    flush()
                    currentName = event.screenName
                    currentKind = ScreenSegment.Kind.Screen
                }
                is InteractionEvent.AppForeground -> {
                    flush()
                    currentName = "Foreground"
                    currentKind = ScreenSegment.Kind.Lifecycle
                }
                is InteractionEvent.AppBackground -> {
                    flush()
                    currentName = "Background"
                    currentKind = ScreenSegment.Kind.Lifecycle
                }
                else -> currentEvents += event
            }
        }
        flush()
        return segments
    }

    // ── Event chips ───────────────────────────────────────────────────────────

    private fun InteractionEvent.toChip(): String? = when (this) {
        is InteractionEvent.Click ->
            """<span class="chip chip-click">CLK</span>"""
        is InteractionEvent.LongPress ->
            """<span class="chip chip-long">HOLD</span>"""
        is InteractionEvent.Scroll ->
            """<span class="chip chip-scroll">SCR</span>"""
        is InteractionEvent.Swipe ->
            """<span class="chip chip-swipe">SWP</span>"""
        is InteractionEvent.Pinch ->
            """<span class="chip chip-pinch">PCH</span>"""
        is InteractionEvent.Breadcrumb ->
            """<span class="chip chip-log" title="${message.escHtml()}">LOG</span>"""
        is InteractionEvent.AppForeground,
        is InteractionEvent.AppBackground,
        is InteractionEvent.ScreenView,
        -> null
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun StringBuilder.meta(label: String, value: String) {
        appendLine("""    <div class="meta-item"><span class="lbl">$label</span><span class="val">${value.escHtml()}</span></div>""")
    }

    private fun String.escHtml() =
        replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    // ── CSS ───────────────────────────────────────────────────────────────────

    private val CSS = """
        *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0 }
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
            background: #0d0f14; color: #e2e4ea; padding: 32px 24px;
            line-height: 1.5;
        }
        h1 { font-size: 22px; font-weight: 700; margin: 8px 0 16px }
        h2 {
            font-size: 11px; font-weight: 600; text-transform: uppercase;
            letter-spacing: .1em; color: #5a5f72; margin-bottom: 14px;
        }
        section { margin-bottom: 48px }
        header  { margin-bottom: 40px }

        /* Badge */
        .badge {
            display: inline-block; padding: 3px 10px; border-radius: 4px;
            font-size: 11px; font-weight: 700; letter-spacing: .05em; margin-bottom: 10px;
        }
        .badge-ok   { background: #0f2e1a; color: #4ade80 }
        .badge-crash { background: #2e0f0f; color: #f87171 }

        /* Meta grid */
        .meta-grid { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 4px }
        .meta-item {
            background: #181b25; border: 1px solid #252836;
            padding: 6px 12px; border-radius: 6px; font-size: 12px;
        }
        .lbl { color: #5a5f72; margin-right: 6px }
        .val { color: #c8ccd8; font-weight: 500 }

        /* Crash box */
        .crash-box {
            margin-top: 14px; padding: 10px 14px; border-radius: 6px;
            background: #1e0a0a; border-left: 3px solid #ef4444;
            color: #fca5a5; font-size: 12px; font-family: 'SFMono-Regular', Consolas, monospace;
        }

        /* Flow */
        .flow-scroll { overflow-x: auto; padding-bottom: 12px }
        .flow { display: flex; align-items: flex-start; gap: 0; min-width: max-content; padding: 4px 0 }
        .arrow {
            color: #3a3f52; font-size: 22px; padding: 0 6px;
            align-self: center; margin-top: -10px; user-select: none; flex-shrink: 0;
        }

        /* Cards */
        .card {
            background: #181b25; border: 1px solid #252836;
            border-radius: 10px; padding: 12px 14px;
            min-width: 110px; max-width: 170px;
        }
        .card-lifecycle {
            background: #101820; border-color: #1e3040;
        }
        .card-crash {
            background: #1e0a0a; border-color: #7f1d1d;
        }
        .card-title {
            font-size: 12px; font-weight: 600; color: #93c5fd;
            margin-bottom: 8px; word-break: break-word; line-height: 1.3;
        }
        .card-lifecycle .card-title { color: #86efac; font-size: 11px }
        .card-crash     .card-title { color: #fca5a5 }

        /* Chips */
        .chip-row { display: flex; flex-wrap: wrap; gap: 4px }
        .chip {
            display: inline-block; padding: 2px 6px; border-radius: 4px;
            font-size: 10px; font-weight: 700; letter-spacing: .04em; cursor: default;
        }
        .chip-click  { background: #2d1a4a; color: #ce93d8 }
        .chip-long   { background: #2e2100; color: #fbbf24 }
        .chip-scroll { background: #0a2040; color: #64b5f6 }
        .chip-swipe  { background: #082820; color: #80cbc4 }
        .chip-pinch  { background: #2e1028; color: #f48fb1 }
        .chip-log    { background: #28240a; color: #fcd34d }

        /* Screenshot */
        .screenshot {
            display: block; max-width: 280px; border-radius: 10px;
            border: 1px solid #252836; box-shadow: 0 4px 24px #00000060;
        }

        /* Timeline */
        .timeline {
            background: #181b25; border: 1px solid #252836;
            padding: 16px 20px; border-radius: 10px;
            font-size: 12px; font-family: 'SFMono-Regular', Consolas, monospace;
            color: #8891aa; line-height: 1.8; overflow-x: auto; white-space: pre;
        }
    """
}
