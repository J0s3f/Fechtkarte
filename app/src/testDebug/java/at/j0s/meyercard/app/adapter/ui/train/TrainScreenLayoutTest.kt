package at.j0s.meyercard.app.adapter.ui.train

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

/**
 * Found via real-device QA testing, M-01: in a viewport wider than it is tall, [TrainScreen]'s card
 * shrank to a small thumbnail instead of using the available space — a plain `Column` always
 * stacked the card above the buttons, and [at.j0s.meyercard.app.adapter.ui.CardArea] derives the
 * card's width from whatever *height* it's given, which the two button rows ate into. A fixed
 * (not device-orientation-based) container size, rather than `@Config(qualifiers = "land")`,
 * makes the "wider than tall" condition explicit and independent of Robolectric's default
 * window dimensions.
 */
@RunWith(AndroidJUnit4::class)
class TrainScreenLayoutTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context get() = ApplicationProvider.getApplicationContext<Context>()

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

    @Test
    fun `the generated card fills most of the available height in a wide viewport`() {
        composeTestRule.setContent {
            Box(modifier = Modifier.size(600.dp, 300.dp)) {
                TrainScreen(card = card, onGenerate = {}, onConfigure = {}, onSavePng = {}, onSavePdf = {}, onShare = {})
            }
        }

        // 200dp out of ~268dp actually available (300dp container minus 16dp padding on each
        // side): comfortably above what a Column-forced layout could ever reach here (the card's
        // own height was capped by whatever sliver of vertical space survived two button rows,
        // well under half this container's height), comfortably below the theoretical maximum,
        // so this doesn't pin an exact pixel value.
        composeTestRule.onNodeWithContentDescription(card.contentDescription(context.resources)).assertHeightIsAtLeast(200.dp)
    }
}
