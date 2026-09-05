package at.j0s.meyercard.app.adapter.ui.library

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import at.j0s.meyercard.app.R
import at.j0s.meyercard.app.adapter.ui.displayName
import at.j0s.meyercard.app.domain.Action
import at.j0s.meyercard.app.domain.CardId
import at.j0s.meyercard.app.domain.CardOrigin
import at.j0s.meyercard.app.domain.CardPalette
import at.j0s.meyercard.app.domain.Direction
import at.j0s.meyercard.app.domain.Hand
import at.j0s.meyercard.app.domain.HistoricalDrill
import at.j0s.meyercard.app.domain.Instruction
import at.j0s.meyercard.app.domain.MeyerCard
import at.j0s.meyercard.app.domain.Radius
import at.j0s.meyercard.app.domain.Slot
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

/**
 * Every existing Library test either renders the default Drills tab
 * ([LibraryScreenScreenshotTest]) or checks the empty-filter state without ever navigating
 * anywhere else ([LibraryEmptyFilterTest]). A coverage pass turned up that nothing had ever
 * clicked the Techniques tab, a browse button, a filter chip, or "toggle hand" — the Techniques
 * tab specifically (`LibraryScreen.kt`'s own `TechniquesTab`) had never been rendered by any
 * test at all. Semantic-tree assertions, not screenshots: this is about which state a click
 * produces, not layout.
 */
@RunWith(AndroidJUnit4::class)
class LibraryInteractionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context get() = ApplicationProvider.getApplicationContext<Context>()

    private fun drill(number: Int, actionCount: Int): HistoricalDrill {
        fun card(hand: Hand, id: Long) = MeyerCard(
            id = CardId(id),
            actions = Direction.entries.take(actionCount).mapIndexed { i, direction -> Action(i + 1, Slot(direction, Radius.OUTER), isThrust = false) },
            hand = hand,
            palette = CardPalette.default(hand),
            origin = CardOrigin.Generated(Instant.EPOCH),
        )
        return HistoricalDrill(number, card(Hand.RIGHT, number.toLong()), card(Hand.LEFT, number + 100L))
    }

    // Two different action counts, deliberately: selecting one via the filter chip has to
    // actually narrow the visible drills to prove the filter works, not just that the chip
    // itself got selected.
    private val drills = listOf(drill(1, actionCount = 1), drill(2, actionCount = 1), drill(3, actionCount = 2), drill(4, actionCount = 2))

    private fun techniqueCard(id: Long, instruction: Instruction) = MeyerCard(
        id = CardId(id),
        actions = listOf(Action(1, Slot(Direction.N, Radius.OUTER), isThrust = false)),
        hand = Hand.NEUTRAL,
        palette = CardPalette.default(Hand.NEUTRAL),
        instruction = instruction,
        origin = CardOrigin.Generated(Instant.EPOCH),
    )

    private val techniqueCards = listOf(
        techniqueCard(1, Instruction.DOUBLE_FEINT),
        techniqueCard(2, Instruction.MOULINET),
    )

    private fun setScreen() {
        composeTestRule.setContent { LibraryScreen(drills = drills, techniqueCards = techniqueCards) }
    }

    private fun drillPosition(index: Int, of: Int = drills.size) = context.getString(R.string.library_drill_position, index, of)

    @Test
    fun `browse buttons move through the drills`() {
        setScreen()

        composeTestRule.onNodeWithContentDescription(context.getString(R.string.library_nav_next)).performClick()
        composeTestRule.onNodeWithText(drillPosition(2)).assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription(context.getString(R.string.library_nav_last)).performClick()
        composeTestRule.onNodeWithText(drillPosition(4)).assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription(context.getString(R.string.library_nav_first)).performClick()
        composeTestRule.onNodeWithText(drillPosition(1)).assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription(context.getString(R.string.library_nav_forward_ten)).performClick()
        composeTestRule.onNodeWithText(drillPosition(4)).assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription(context.getString(R.string.library_nav_back_ten)).performClick()
        composeTestRule.onNodeWithText(drillPosition(1)).assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription(context.getString(R.string.library_nav_previous)).performClick()
        composeTestRule.onNodeWithText(drillPosition(1)).assertIsDisplayed()

        // Not asserted against a specific position (that's what a fixed seed's for, elsewhere) —
        // just that clicking it doesn't crash and something is still showing.
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.library_nav_random)).performClick()
    }

    @Test
    fun `toggle hand switches the label between right and left`() {
        setScreen()
        val right = " " + context.getString(R.string.library_hand, context.getString(R.string.library_hand_right))
        val left = " " + context.getString(R.string.library_hand, context.getString(R.string.library_hand_left))

        composeTestRule.onNodeWithText(right).assertIsDisplayed()
        composeTestRule.onNodeWithText(right).performClick()
        composeTestRule.onNodeWithText(left).assertIsDisplayed()
    }

    @Test
    fun `selecting an action-count filter narrows the drills shown`() {
        setScreen()
        val oneAction = context.resources.getQuantityString(R.plurals.library_filter_actions, 1, 1)

        composeTestRule.onNodeWithText(oneAction).performClick()

        // Drills 1 and 2 have one action each; narrowing to them means "of 2", not "of 4".
        composeTestRule.onNodeWithText(drillPosition(1, of = 2)).assertIsDisplayed()
    }

    @Test
    fun `the Techniques tab renders its own cards, filter and browse controls`() {
        setScreen()
        composeTestRule.onNodeWithText(context.getString(R.string.library_tab_techniques)).performClick()

        fun cardPosition(index: Int, of: Int) = context.getString(R.string.library_card_position, index, of)
        composeTestRule.onNodeWithText(cardPosition(1, of = 2)).assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription(context.getString(R.string.library_nav_next)).performClick()
        composeTestRule.onNodeWithText(cardPosition(2, of = 2)).assertIsDisplayed()

        // Narrows the visible cards to just the one DOUBLE_FEINT technique -- "of 1", not "of 2".
        val doubleFeint = Instruction.DOUBLE_FEINT.displayName(context.resources)
        composeTestRule.onNodeWithText(doubleFeint).performClick()
        composeTestRule.onNodeWithText(cardPosition(1, of = 1)).assertIsDisplayed()

        composeTestRule.onNodeWithText(context.getString(R.string.library_filter_all)).performClick()
        composeTestRule.onNodeWithText(cardPosition(1, of = 2)).assertIsDisplayed()
    }
}
