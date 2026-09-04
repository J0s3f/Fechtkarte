package at.j0s.meyercard.app.adapter.ui.library

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import at.j0s.meyercard.app.R
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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

/**
 * PrimeTestLab report M-02: selecting a filter combination with no matching drills left the
 * card-navigation toolbar (first/back-ten/previous/next/forward-ten/last/random) looking fully
 * enabled, with nothing for any of its buttons to actually navigate to. Semantic-tree
 * assertions, not a screenshot -- "enabled or not" isn't something a pixel golden checks well,
 * and every existing Library screenshot test already covers this screen's normal layout.
 */
@RunWith(AndroidJUnit4::class)
class LibraryEmptyFilterTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context get() = ApplicationProvider.getApplicationContext<Context>()

    private fun card(hand: Hand, id: Long) = MeyerCard(
        id = CardId(id),
        actions = listOf(Action(1, Slot(Direction.N, Radius.OUTER), isThrust = false)),
        hand = hand,
        palette = CardPalette.default(hand),
        origin = CardOrigin.Generated(Instant.EPOCH),
    )

    // Zero thrusts on every drill, deliberately -- selecting the "3 thrusts" filter chip below
    // then has no possible match, the same shape as the reported repro (3 actions, 3 thrusts).
    private val drills = listOf(HistoricalDrill(number = 1, rightHandCard = card(Hand.RIGHT, 1), leftHandCard = card(Hand.LEFT, 2)))

    @Test
    fun `navigation controls are enabled when a drill is showing`() {
        composeTestRule.setContent { LibraryScreen(drills = drills, techniqueCards = emptyList()) }
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.library_nav_next)).assertIsEnabled()
    }

    @Test
    fun `navigation controls are disabled when the filter matches nothing`() {
        composeTestRule.setContent { LibraryScreen(drills = drills, techniqueCards = emptyList()) }

        val threeThrusts = context.resources.getQuantityString(R.plurals.library_filter_thrusts, 3, 3)
        composeTestRule.onNodeWithText(threeThrusts).performClick()

        composeTestRule.onNodeWithText(context.getString(R.string.library_no_drills_match)).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.library_nav_first)).assertIsNotEnabled()
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.library_nav_back_ten)).assertIsNotEnabled()
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.library_nav_previous)).assertIsNotEnabled()
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.library_nav_next)).assertIsNotEnabled()
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.library_nav_forward_ten)).assertIsNotEnabled()
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.library_nav_last)).assertIsNotEnabled()
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.library_nav_random)).assertIsNotEnabled()
    }
}
