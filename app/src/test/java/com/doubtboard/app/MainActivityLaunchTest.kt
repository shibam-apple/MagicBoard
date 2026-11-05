package com.doubtboard.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.doubtboard.app.ui.ROOT_TAG
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** M0 "done when": the empty Compose screen launches. Runs on the JVM via Robolectric. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MainActivityLaunchTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun launchesEmptyScreen() {
        compose.onNodeWithTag(ROOT_TAG).assertIsDisplayed()
        compose.onNodeWithText("Doubt Board").assertIsDisplayed()
    }
}
