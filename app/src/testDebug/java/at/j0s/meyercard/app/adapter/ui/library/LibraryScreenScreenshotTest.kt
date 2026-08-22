package at.j0s.meyercard.app.adapter.ui.library

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import at.j0s.meyercard.app.domain.Action
import at.j0s.meyercard.app.domain.CardId
import at.j0s.meyercard.app.domain.CardOrigin
import at.j0s.meyercard.app.domain.CardPalette
import at.j0s.meyercard.app.domain.Direction
import at.j0s.meyercard.app.domain.Hand
import at.j0s.meyercard.app.domain.HistoricalDrill
import at.j0s.meyercard.app.domain.MeyerCard
import at.j0s.meyercard.app.domain.Radius
import at.j0s.meyercard.app.domain.Slot
import com.github.takahirom.roborazzi.RoborazziRule
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import java.time.Instant

/**
 * Robolectric/Roborazzi, debug-only -- same reasoning as the other screenshot tests (T2.4). Had
 * no screenshot coverage at all before this file; added alongside the `TabRow` ->
 * `PrimaryTabRow` migration (T9.19) since the tab strip this test captures is exactly what
 * changed.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LibraryScreenScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val roborazziRule = RoborazziRule(
        composeRule = composeTestRule,
        captureRoot = composeTestRule.onRoot(),
    )

    private fun card(hand: Hand, id: Long) = MeyerCard(
        id = CardId(id),
        actions = listOf(Action(1, Slot(Direction.N, Radius.OUTER), isThrust = false)),
        hand = hand,
        palette = CardPalette.default(hand),
        origin = CardOrigin.Generated(Instant.EPOCH),
    )

    private val drills = listOf(
        HistoricalDrill(number = 1, rightHandCard = card(Hand.RIGHT, 1), leftHandCard = card(Hand.LEFT, 2)),
        HistoricalDrill(number = 2, rightHandCard = card(Hand.RIGHT, 3), leftHandCard = card(Hand.LEFT, 4)),
    )

    private val techniqueCards = listOf(card(Hand.RIGHT, 5), card(Hand.RIGHT, 6))

    @Test
    fun drillsTabIsShownByDefaultWithPrimaryTabRow() {
        composeTestRule.setContent { LibraryScreen(drills = drills, techniqueCards = techniqueCards) }
        composeTestRule.onRoot().captureRoboImage()
    }
}
