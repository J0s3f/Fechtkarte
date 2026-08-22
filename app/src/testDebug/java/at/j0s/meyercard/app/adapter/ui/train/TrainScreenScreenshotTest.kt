package at.j0s.meyercard.app.adapter.ui.train

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import at.j0s.meyercard.app.domain.Action
import at.j0s.meyercard.app.domain.CardId
import at.j0s.meyercard.app.domain.CardOrigin
import at.j0s.meyercard.app.domain.CardPalette
import at.j0s.meyercard.app.domain.Direction
import at.j0s.meyercard.app.domain.Hand
import at.j0s.meyercard.app.domain.MeyerCard
import at.j0s.meyercard.app.domain.Radius
import at.j0s.meyercard.app.domain.Slot
import androidx.compose.runtime.Composable
import com.github.takahirom.roborazzi.RoborazziRule
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant

/**
 * Robolectric/Roborazzi, debug-only — same reasoning as
 * [at.j0s.meyercard.app.adapter.ui.MeyerSquareCardScreenshotTest]. Unlike that class, this
 * one exercises the whole screen, not just the card in isolation — the class this project's
 * documented that matters (T3.3's ActionBar bug: a component that renders correctly alone can
 * still overlap its siblings once composed into a real screen).
 *
 * `@Config(qualifiers = "land")` is the point of this file (T7.4): [TrainScreen]'s card used
 * to be a plain `fillMaxWidth()` with no scroll wrapper, so in landscape — where a full-width
 * card wants to be far *taller* than the available height, given the card's own portrait-ish
 * aspect ratio — it would have pushed the Generate/Configure/Save/Share buttons off-screen
 * entirely, with nothing to scroll to reach them. Fixed by routing the card through
 * [at.j0s.meyercard.app.adapter.ui.CardArea], the same fix already in place for the Library
 * screen's own version of this bug.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TrainScreenScreenshotTest {

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
            Action(1, Slot(Direction.N, Radius.OUTER), isThrust = false),
            Action(2, Slot(Direction.SE, Radius.INNER), isThrust = true),
        ),
        hand = Hand.RIGHT,
        palette = CardPalette.default(Hand.RIGHT),
        origin = CardOrigin.Generated(Instant.EPOCH),
    )

    // The English golden below was the only one that existed, and English happens to have the
    // shortest labels of the three languages — so a fixed Row looked fine here while French
    // pushed Share off-screen entirely on a real device. These two capture the same screen in
    // the languages that actually stress the layout.
    @Composable
    private fun screen() = TrainScreen(
        card = card,
        onGenerate = {},
        onConfigure = {},
        onSavePng = {},
        onSavePdf = {},
        onShare = {},
    )

    @Test
    @Config(qualifiers = "de")
    fun allButtonsFitInGerman() {
        composeTestRule.setContent { screen() }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "fr")
    fun allButtonsFitInFrench() {
        composeTestRule.setContent { screen() }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "land")
    fun cardAndAllButtonsFitInLandscape() {
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
}
