package at.j0s.meyercard.app.adapter.ui.configure

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import at.j0s.meyercard.app.R
import at.j0s.meyercard.app.domain.GenerationPreferences
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [ConfigureScreenScreenshotTest] renders the screen; nothing exercised the "up to N
 * actions/thrusts" plural text, only reached once `actionCountIsMaximum`/`thrustCountIsMaximum`
 * is true -- the screenshot tests' fixtures never set it.
 *
 * The language picker's chip click (`LanguagePicker`'s own `onClick`) is deliberately not tested
 * here: on this project's `compileSdk` (36, >= 33), `AppCompatDelegate.setApplicationLocales`
 * routes through the platform `LocaleManager`, and Robolectric's shadow for it didn't reflect the
 * change back through `getApplicationLocales()` within a single test -- a Robolectric/shadow gap,
 * not a real bug (confirmed working end-to-end on a real device by
 * [at.j0s.meyercard.app.adapter.ui.LanguagePersistenceInstrumentedTest]). Chasing it with
 * Robolectric-internal shadow reflection would be exactly the kind of brittle, disproportionate
 * test this project's own conventions warn against.
 */
@RunWith(AndroidJUnit4::class)
class ConfigureScreenInteractionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context get() = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `shows the up-to-N-actions plural text once action count is capped at a maximum`() {
        val preferences = GenerationPreferences(actionCount = 3, actionCountIsMaximum = true)
        composeTestRule.setContent {
            ConfigureScreen(state = ConfigureScreenState(preferences), onStateChange = {})
        }
        val expected = context.resources.getQuantityString(R.plurals.configure_actions_maximum, 3, 3)
        composeTestRule.onNodeWithText(expected).assertIsDisplayed()
    }

    @Test
    fun `shows the up-to-N-thrusts plural text once thrust count is capped at a maximum`() {
        val preferences = GenerationPreferences(thrustCount = 2, thrustCountIsMaximum = true)
        composeTestRule.setContent {
            ConfigureScreen(state = ConfigureScreenState(preferences), onStateChange = {})
        }
        val expected = context.resources.getQuantityString(R.plurals.configure_thrusts_maximum, 2, 2)
        composeTestRule.onNodeWithText(expected).assertIsDisplayed()
    }
}
