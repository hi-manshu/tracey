package com.himanshoe.tracey.export

import com.himanshoe.tracey.appBackground
import com.himanshoe.tracey.appForeground
import com.himanshoe.tracey.breadcrumb
import com.himanshoe.tracey.fakePayload
import com.himanshoe.tracey.longPress
import com.himanshoe.tracey.pinch
import com.himanshoe.tracey.screenView
import com.himanshoe.tracey.scroll
import com.himanshoe.tracey.swipe
import com.himanshoe.tracey.click
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

class ReplayHtmlExporterTest {

    private fun export(vararg events: com.himanshoe.tracey.model.InteractionEvent, crashReason: String? = null) =
        ReplayHtmlExporter.export(fakePayload(events = events.toList(), crashReason = crashReason))

    @Test
    fun outputIsValidHtmlDocument() {
        val html = export(click())
        assertContains(html, "<!DOCTYPE html>")
        assertContains(html, "<html")
        assertContains(html, "</html>")
        assertContains(html, "<head>")
        assertContains(html, "<body>")
    }

    @Test
    fun titleContainsSessionIdPrefix() {
        val html = ReplayHtmlExporter.export(fakePayload(sessionId = "abcdef1234567890"))
        assertContains(html, "abcdef12")
    }

    @Test
    fun normalPayloadShowsReplayBadge() {
        val html = export(click())
        assertContains(html, "badge-ok")
        assertContains(html, "✅ REPLAY")
        assertFalse(html.contains("badge-crash"))
    }

    @Test
    fun crashPayloadShowsCrashBadge() {
        val html = export(tap(), crashReason = "NullPointerException at Foo.kt:10")
        assertContains(html, "badge-crash")
        assertContains(html, "💥 CRASH REPORT")
        assertContains(html, "NullPointerException at Foo.kt:10")
    }

    @Test
    fun metaGridContainsDeviceAndPlatformInfo() {
        val html = export(click())
        assertContains(html, "Android")
        assertContains(html, "Pixel 8")
        assertContains(html, "1.0.0")
    }

    @Test
    fun clickEventProducesClickChip() {
        val html = export(click())
        assertContains(html, "chip-click")
        assertContains(html, ">CLK<")
    }

    @Test
    fun longPressProducesHoldChip() {
        val html = export(longPress())
        assertContains(html, "chip-long")
        assertContains(html, ">HOLD<")
    }

    @Test
    fun scrollProducesScrollChip() {
        val html = export(scroll())
        assertContains(html, "chip-scroll")
        assertContains(html, ">SCR<")
    }

    @Test
    fun swipeProducesSwipeChip() {
        val html = export(swipe())
        assertContains(html, "chip-swipe")
        assertContains(html, ">SWP<")
    }

    @Test
    fun pinchProducesPinchChip() {
        val html = export(pinch())
        assertContains(html, "chip-pinch")
        assertContains(html, ">PCH<")
    }

    @Test
    fun breadcrumbProducesLogChip() {
        val html = export(breadcrumb(message = "Cart: added SKU-1234"))
        assertContains(html, "chip-log")
        assertContains(html, ">LOG<")
        assertContains(html, "Cart: added SKU-1234")
    }

    @Test
    fun lifecycleEventsProduceScreenCards() {
        val html = export(appForeground(), appBackground())
        assertContains(html, "Foreground")
        assertContains(html, "Background")
        assertContains(html, "card-lifecycle")
    }

    @Test
    fun screenViewStartsNewCard() {
        val html = export(screenView(screenName = "CheckoutScreen"))
        assertContains(html, "CheckoutScreen")
        assertContains(html, "card-title")
    }

    @Test
    fun flowSectionPresent() {
        val html = export(click())
        assertContains(html, "User Flow")
        assertContains(html, "flow-scroll")
    }

    @Test
    fun fullTimelineSectionPresent() {
        val html = ReplayHtmlExporter.export(fakePayload(timeline = "00:01.000  TAP  Button"))
        assertContains(html, "Full Timeline")
        assertContains(html, "00:01.000  TAP  Button")
    }

    @Test
    fun screenshotSectionAbsentWhenNoScreenshot() {
        val html = export(click())
        assertFalse(html.contains("Screenshot at Capture"))
    }

    @Test
    fun htmlCharsInCrashReasonAreEscaped() {
        val html = export(crashReason = "<script>alert('xss')</script>")
        assertFalse(html.contains("<script>"))
        assertContains(html, "&lt;script&gt;")
    }

    @Test
    fun arrowSeparatorBetweenCards() {
        val html = export(screenView(screenName = "Home"), screenView(screenName = "Profile"))
        assertContains(html, """class="arrow"›""")
    }

    @Test
    fun outputContainsInlineCss() {
        val html = export(click())
        assertContains(html, "<style>")
        assertContains(html, "font-family")
    }

    @Test
    fun noExternalDependenciesInOutput() {
        val html = export(click())
        assertFalse(html.contains("src=\"http"))
        assertFalse(html.contains("href=\"http"))
        assertFalse(html.contains("<link "))
        assertFalse(html.contains("<script src"))
    }
}
