package com.himanshoe.tracey

import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TraceyResetTest {

    @AfterTest
    fun tearDown() = runTest { Tracey.resetForTest() }

    @Test
    fun installSetsIsInstalledTrue() {
        Tracey.install(TraceyConfig(enabled = false))
        assertTrue(Tracey.isInstalled)
    }

    @Test
    fun resetClearsIsInstalled() = runTest {
        Tracey.install(TraceyConfig(enabled = false))
        Tracey.resetForTest()
        assertFalse(Tracey.isInstalled)
    }

    @Test
    fun resetClearsBuffer() = runTest {
        Tracey.install(TraceyConfig(enabled = true))
        Tracey.log("breadcrumb before reset")
        Tracey.resetForTest()
        assertEquals(0, Tracey.buffer.size)
    }

    @Test
    fun resetRestoresDefaultConfig() = runTest {
        Tracey.install(TraceyConfig(bufferDurationSeconds = 99, maxEvents = 7))
        Tracey.resetForTest()
        assertEquals(30, Tracey.config.bufferDurationSeconds)
        assertEquals(500, Tracey.config.maxEvents)
    }

    @Test
    fun installAfterResetPicksUpNewConfig() = runTest {
        Tracey.install(TraceyConfig(bufferDurationSeconds = 10, enabled = false))
        Tracey.resetForTest()
        Tracey.install(TraceyConfig(bufferDurationSeconds = 45, enabled = false))
        assertEquals(45, Tracey.config.bufferDurationSeconds)
    }
}
