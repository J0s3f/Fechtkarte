package at.j0s.meyercard.app.adapter.ui.train

import android.content.Context
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import at.j0s.meyercard.app.adapter.ui.contentDescription
import at.j0s.meyercard.app.domain.Action
import at.j0s.meyercard.app.domain.CardId
import at.j0s.meyercard.app.domain.CardOrigin
import at.j0s.meyercard.app.domain.CardPalette
import at.j0s.meyercard.app.domain.Direction
import at.j0s.meyercard.app.domain.Hand
import at.j0s.meyercard.app.domain.MeyerCard
import at.j0s.meyercard.app.domain.Radius
import at.j0s.meyercard.app.domain.Slot
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

/**
 * Tap-to-generate (T5.4) itself had never been exercised by any test before this — only inferred
 * from [TrainScreen]'s own `.clickable(onClick = onGenerate)`. A real-device QA report (M-03)
 * found testers surprised by a card silently replacing itself on a single tap; the fix is a
 * `tapToGenerateEnabled` flag (mirroring the existing `shakeToGenerateEnabled` one), not removing
 * the feature, so both the still-on-by-default behaviour and the new opt-out need covering.
 */
@RunWith(AndroidJUnit4::class)
class TrainScreenInteractionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context get() = ApplicationProvider.getApplicationContext<Context>()

    private val card = MeyerCard(
        id = CardId(1L),
        actions = listOf(Action(1, Slot(Direction.N, Radius.OUTER), isThrust = false)),
        hand = Hand.RIGHT,
        palette = CardPalette.default(Hand.RIGHT),
        origin = CardOrigin.Generated(Instant.EPOCH),
    )

    private var generateCount = 0

    private fun setScreen(tapToGenerateEnabled: Boolean) {
        composeTestRule.setContent {
            TrainScreen(
                card = card,
                onGenerate = { generateCount++ },
                onConfigure = {},
                onSavePng = {},
                onSavePdf = {},
                onShare = {},
                tapToGenerateEnabled = tapToGenerateEnabled,
            )
        }
    }

    @Test
    fun `tapping the card regenerates it when tap to generate is enabled`() {
        setScreen(tapToGenerateEnabled = true)
        composeTestRule.onNodeWithContentDescription(card.contentDescription(context.resources)).performClick()
        assertEquals(1, generateCount)
    }

    @Test
    fun `tapping the card does nothing when tap to generate is disabled`() {
        setScreen(tapToGenerateEnabled = false)
        composeTestRule.onNodeWithContentDescription(card.contentDescription(context.resources)).performClick()
        assertEquals(0, generateCount)
    }
}
