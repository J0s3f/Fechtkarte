package at.j0s.meyercard.app.adapter.ui

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import at.j0s.meyercard.app.adapter.ui.learn.LearnScreen
import at.j0s.meyercard.app.adapter.ui.train.TrainScreen
import at.j0s.meyercard.app.domain.Action
import at.j0s.meyercard.app.domain.CardId
import at.j0s.meyercard.app.domain.CardOrigin
import at.j0s.meyercard.app.domain.CardPalette
import at.j0s.meyercard.app.domain.Direction
import at.j0s.meyercard.app.domain.Hand
import at.j0s.meyercard.app.domain.MeyerCard
import at.j0s.meyercard.app.domain.Radius
import at.j0s.meyercard.app.domain.Slot
import com.github.takahirom.roborazzi.RoborazziRule
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant

/**
 * T9.2: store-listing screenshots, captured through Robolectric/Roborazzi at a realistic phone
 * resolution rather than the small fixture size the regression-test goldens use — there's no
 * emulator available in this environment (the same reason Roborazzi exists at all, T2.4), so
 * this is the only way to produce a screenshot of the real, running UI rather than a mockup.
 * `w412dp-h915dp` matches a common modern phone (roughly Pixel-class), well inside Play
 * Console's screenshot aspect-ratio requirement (9:16 to 16:9).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w412dp-h915dp")
class PlayStoreScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val roborazziRule = RoborazziRule(
        composeRule = composeTestRule,
        captureRoot = composeTestRule.onRoot(),
    )

    private val card = MeyerCard(
        id = CardId(1L),
        actions = listOf(
            Action(1, Slot(Direction.NW, Radius.OUTER), isThrust = false),
            Action(2, Slot(Direction.E, Radius.OUTER), isThrust = true),
            Action(3, Slot(Direction.S, Radius.OUTER), isThrust = false),
            Action(4, Slot(Direction.SW, Radius.INNER), isThrust = false),
        ),
        hand = Hand.RIGHT,
        palette = CardPalette.default(Hand.RIGHT),
        origin = CardOrigin.Generated(Instant.EPOCH),
    )

    @Test
    fun trainScreen() {
        composeTestRule.setContent {
            TrainScreen(
                card = card,
                onGenerate = {},
                onConfigure = {},
                onSavePng = {},
                onSavePdf = {},
                onShare = {},
            )
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun learnScreen() {
        composeTestRule.setContent { LearnScreen(onNoticesClick = {}) }
        composeTestRule.onRoot().captureRoboImage()
    }
}
