package at.j0s.meyercard.app.adapter.ui

import at.j0s.meyercard.app.domain.Action
import at.j0s.meyercard.app.domain.CardId
import at.j0s.meyercard.app.domain.CardOrigin
import at.j0s.meyercard.app.domain.CardPalette
import at.j0s.meyercard.app.domain.Direction
import at.j0s.meyercard.app.domain.Hand
import at.j0s.meyercard.app.domain.MeyerCard
import at.j0s.meyercard.app.domain.Radius
import at.j0s.meyercard.app.domain.Slot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * [encodeTrainCard]/[decodeTrainCard] are what let Train's active card survive an actual
 * activity recreation (a language switch, in particular — see their own doc comment), not just
 * ordinary navigation. Tested directly as plain functions, no Robolectric/Compose needed:
 * they're already Compose-independent by design, precisely so this doesn't need to be.
 */
class FechtkarteAppTrainCardSaverTest {

    private fun card(palette: CardPalette = CardPalette.MADDER) = MeyerCard(
        id = CardId(1L),
        actions = listOf(
            Action(1, Slot(Direction.N, Radius.OUTER), isThrust = false),
            Action(2, Slot(Direction.SE, Radius.INNER), isThrust = true),
        ),
        hand = Hand.LEFT,
        palette = palette,
        origin = CardOrigin.Generated(Instant.EPOCH),
    )

    @Test
    @DisplayName("a null card encodes to an empty string and decodes back to null")
    fun `null round trips to null`() {
        assertEquals("", encodeTrainCard(null))
        assertNull(decodeTrainCard(""))
    }

    @Test
    @DisplayName("a card's actions and palette survive an encode/decode round trip exactly")
    fun `actions and palette round trip exactly`() {
        val original = card()
        val restored = decodeTrainCard(encodeTrainCard(original))

        assertEquals(original.actions, restored?.actions)
        assertEquals(original.palette, restored?.palette)
    }

    @Test
    @DisplayName("a different palette produces a different encoding")
    fun `different palette encodes differently`() {
        val woad = encodeTrainCard(card(CardPalette.WOAD))
        val madder = encodeTrainCard(card(CardPalette.MADDER))

        assertNotEquals(woad, madder)
    }
}
