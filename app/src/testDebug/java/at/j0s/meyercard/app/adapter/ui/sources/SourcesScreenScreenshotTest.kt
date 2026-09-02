package at.j0s.meyercard.app.adapter.ui.sources

import android.content.Context
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import at.j0s.meyercard.app.R
import com.github.takahirom.roborazzi.RoborazziRule
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/**
 * Robolectric/Roborazzi, debug-only — same reasoning as the other screenshot tests (T2.4). Loads
 * the real bundled scan asset via [readSourcesScanAsset] rather than a stand-in bitmap, so this
 * exercises the actual asset-read path, not just the layout.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SourcesScreenScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val roborazziRule = RoborazziRule(
        composeRule = composeTestRule,
        captureRoot = composeTestRule.onRoot(),
    )

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun sourcesScreenShowsTheRedrawByDefault() {
        composeTestRule.setContent { SourcesScreen(scan = context.readSourcesScanAsset()) }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun sourcesScreenTogglesToTheScan() {
        composeTestRule.setContent { SourcesScreen(scan = context.readSourcesScanAsset()) }
        composeTestRule.onNodeWithText(context.getString(R.string.sources_view_scan)).performScrollTo().performClick()
        composeTestRule.onRoot().captureRoboImage()
    }
}
