package at.j0s.meyercard.app.adapter.ui.notices

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RoborazziRule
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/** Robolectric/Roborazzi, debug-only — same reasoning as the other screenshot tests (T2.4). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class NoticesScreenScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val roborazziRule = RoborazziRule(
        composeRule = composeTestRule,
        captureRoot = composeTestRule.onRoot(),
    )

    @Test
    fun noticesScreenRendersDependenciesAndFontLicence() {
        composeTestRule.setContent {
            NoticesScreen(entries = RUNTIME_DEPENDENCY_NOTICES, fontLicenceText = "Sample OFL licence text.")
        }
        composeTestRule.onRoot().captureRoboImage()
    }
}
