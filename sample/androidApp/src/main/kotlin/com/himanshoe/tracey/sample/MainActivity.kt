package com.himanshoe.tracey.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme

/**
 * No TraceyHost here — Tracey is installed in [SampleApplication].
 *
 * Gesture recording (clicks, scrolls, swipes) is unavailable in this flow.
 * Screen views, breadcrumbs, lifecycle events, and crash recovery all work.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                SampleApp()
            }
        }
    }
}
