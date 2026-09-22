package at.j0s.meyercard.app.adapter.ui.learn

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import at.j0s.meyercard.app.R
import com.github.takahirom.roborazzi.RoborazziRule
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Robolectric/Roborazzi, debug-only — same reasoning as the other screenshot tests (T2.4). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LearnScreenScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val roborazziRule = RoborazziRule(
        composeRule = composeTestRule,
        captureRoot = composeTestRule.onRoot(),
    )

    @Test
    fun learnScreenRendersCopyAndWorkedExample() {
        composeTestRule.setContent { LearnScreen(onNoticesClick = {}, onSourcesClick = {}) }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun extendedSectionsAndExampleAreReachableInEnglish() = verifyExtendedContent()

    @Test
    @Config(qualifiers = "de")
    fun extendedSectionsAndExampleAreReachableInGerman() = verifyExtendedContent()

    @Test
    @Config(qualifiers = "fr")
    fun extendedSectionsAndExampleAreReachableInFrench() = verifyExtendedContent()

    private fun verifyExtendedContent() {
        composeTestRule.setContent { LearnScreen(onNoticesClick = {}, onSourcesClick = {}) }
        val context = ApplicationProvider.getApplicationContext<Context>()
        listOf(
            R.string.learn_openings_body,
            R.string.learn_centre_body,
            R.string.learn_lines_compass,
            R.string.learn_lines_sequence,
            R.string.learn_lines_bridge,
            R.string.learn_lines_none,
            R.string.learn_worked_example_intro,
            R.string.learn_sources,
        ).forEach { resource ->
            composeTestRule.onNodeWithText(context.getString(resource)).performScrollTo().assertIsDisplayed()
        }
        composeTestRule.onRoot().captureRoboImage()
    }
}
