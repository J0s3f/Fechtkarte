package at.j0s.meyercard.app.adapter.ui.configure

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import at.j0s.meyercard.app.domain.GenerationPreferences
import at.j0s.meyercard.app.domain.MinimumAngularDistance
import com.github.takahirom.roborazzi.RoborazziRule
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/**
 * Robolectric/Roborazzi, debug-only — same reasoning as the other screenshot tests (T2.4). The
 * only screen that had none before this: unlike Train/Library/Learn/etc., a coverage pass turned
 * up [ConfigureScreen] at 0%, with no existing test exercising it at all — not even the
 * once-off render every other screen gets.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ConfigureScreenScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val roborazziRule = RoborazziRule(
        composeRule = composeTestRule,
        captureRoot = composeTestRule.onRoot(),
    )

    @Test
    fun configureScreenRendersWithDefaultPreferences() {
        composeTestRule.setContent {
            ConfigureScreen(state = ConfigureScreenState(GenerationPreferences()), onStateChange = {})
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun configureScreenRendersMinimumAngularDistanceStepsSlider() {
        // The steps Slider only renders when MinimumAngularDistance is among the enabled rules
        // (see ConfigureScreen.kt) — the default-preferences render above never reaches it.
        val preferences = GenerationPreferences(enabledRules = listOf(MinimumAngularDistance(2)))
        composeTestRule.setContent {
            ConfigureScreen(state = ConfigureScreenState(preferences), onStateChange = {})
        }
        composeTestRule.onRoot().captureRoboImage()
    }
}
