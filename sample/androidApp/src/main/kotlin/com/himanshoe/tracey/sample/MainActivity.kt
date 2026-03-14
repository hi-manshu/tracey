package com.himanshoe.tracey.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.himanshoe.tracey.TraceyHost
import com.himanshoe.tracey.rememberTraceyConfig
import com.himanshoe.tracey.reporter.builtin.LogcatReporter

/**
 * Entry point activity for the Tracey sample app.
 *
 * The only Tracey-specific call here is wrapping [setContent] with [TraceyHost].
 * Everything inside is recorded automatically.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                TraceyHost(
                    traceyConfig = rememberTraceyConfig(
                        showOverlay = false,
                        reporters = listOf(LogcatReporter()),
                    )
                ) {
                    SampleApp()
                }
            }
        }
    }
}
