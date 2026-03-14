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
import kotlin.test.assertTrue

class TestCaseExporterTest {

    private fun export(vararg events: com.himanshoe.tracey.model.InteractionEvent, crashReason: String? = null) =
        TestCaseExporter.export(fakePayload(events = events.toList(), crashReason = crashReason))

    @Test
    fun outputContainsPackageDeclaration() {
        assertContains(export(click()), "package com.example.test")
    }

    @Test
    fun outputContainsRequiredImports() {
        val code = export(click())
        assertContains(code, "import androidx.compose.ui.test.junit4.createComposeRule")
        assertContains(code, "import org.junit.Rule")
        assertContains(code, "import org.junit.Test")
    }

    @Test
    fun outputContainsDefaultClassAndFunctionNames() {
        val code = export(click())
        assertContains(code, "class TraceyGeneratedTest")
        assertContains(code, "fun replayInteractions()")
    }

    @Test
    fun customClassAndFunctionNamesAreUsed() {
        val code = TestCaseExporter.export(
            fakePayload(events = listOf(click())),
            testClassName = "MyTest",
            testFunctionName = "myReplay",
        )
        assertContains(code, "class MyTest")
        assertContains(code, "fun myReplay()")
    }

    @Test
    fun tapWithTestTagGeneratesOnNodeWithTag() {
        val code = export(click(path = "PlaceOrderButton"))
        assertContains(code, """onNodeWithTag("PlaceOrderButton")""")
        assertContains(code, "performClick()")
    }

    @Test
    fun tapWithoutTestTagGeneratesOnRootWithCoordinates() {
        val code = export(click(path = "Screen[0]", x = 540f, y = 960f))
        assertContains(code, "onRoot()")
        assertContains(code, "540.0f")
        assertContains(code, "960.0f")
    }

    @Test
    fun longPressWithTestTagGeneratesLongClick() {
        val code = export(longPress(path = "OrderSummaryCard"))
        assertContains(code, """onNodeWithTag("OrderSummaryCard")""")
        assertContains(code, "longClick()")
    }

    @Test
    fun scrollWithTestTagGeneratesPerformScrollBy() {
        val code = export(scroll(path = "LazyColumn", deltaX = 0f, deltaY = -200f))
        assertContains(code, """onNodeWithTag("LazyColumn")""")
        assertContains(code, "performScrollBy")
        assertContains(code, "-200.0f")
    }

    @Test
    fun swipeGeneratesPerformTouchInputSwipe() {
        val code = export(swipe(path = "HomeScreen"))
        assertContains(code, "performTouchInput")
        assertContains(code, "swipe(")
        assertContains(code, "start =")
        assertContains(code, "end   =")
    }

    @Test
    fun pinchGeneratesPerformTouchInputPinch() {
        val code = export(pinch(path = "MapScreen"))
        assertContains(code, "performTouchInput")
        assertContains(code, "pinch(")
        assertContains(code, "start0 =")
        assertContains(code, "end0   =")
    }

    @Test
    fun lifecycleEventsProduceNoTestAction() {
        val code = export(appForeground(), appBackground(), screenView(), breadcrumb())
        assertFalse(code.contains("performClick"))
        assertFalse(code.contains("performScrollBy"))
        assertFalse(code.contains("performTouchInput"))
    }

    @Test
    fun waitForIdleIsEmittedAfterEveryGestureEvent() {
        val code = export(click(), scroll(), swipe())
        val count = "waitForIdle".let { needle ->
            var n = 0; var idx = 0
            while (code.indexOf(needle, idx).also { idx = it } != -1) { n++; idx++ }
            n
        }
        assertTrue(count >= 3, "Expected at least 3 waitForIdle calls, got $count")
    }

    @Test
    fun crashPayloadIncludesCrashCommentInHeader() {
        val code = export(click(), crashReason = "NullPointerException at Foo.kt:42")
        assertContains(code, "// Crash: NullPointerException at Foo.kt:42")
    }

    @Test
    fun headerContainsSessionIdAndDeviceInfo() {
        val payload = fakePayload(sessionId = "my-session-id", events = listOf(click()))
        val code = TestCaseExporter.export(payload)
        assertContains(code, "// Session: my-session-id")
        assertContains(code, "// Device: Pixel 8 (Android)")
    }

    @Test
    fun emptyEventListProducesValidEmptyTest() {
        val code = export()
        assertContains(code, "fun replayInteractions()")
        assertFalse(code.contains("performClick"))
    }
}
