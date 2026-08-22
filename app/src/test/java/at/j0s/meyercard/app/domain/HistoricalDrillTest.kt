package at.j0s.meyercard.app.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

private fun card(id: Int, hand: Hand) = MeyerCard(
    id = CardId(id.toLong()),
    actions = listOf(Action(1, Slot(Direction.N, Radius.OUTER), isThrust = false)),
    hand = hand,
    palette = CardPalette.default(hand),
    origin = CardOrigin.Historical(number = id),
)

class HistoricalDrillTest {

    @Test
    @DisplayName("card(RIGHT) and card(LEFT) return the matching card")
    fun `card returns the matching hand`() {
        val drill = HistoricalDrill(1, card(1, Hand.RIGHT), card(45, Hand.LEFT))
        assertEquals(Hand.RIGHT, drill.card(Hand.RIGHT).hand)
        assertEquals(Hand.LEFT, drill.card(Hand.LEFT).hand)
    }

    @Test
    @DisplayName("card(NEUTRAL) is rejected - a historical drill has no neutral rendering")
    fun `card NEUTRAL is rejected`() {
        val drill = HistoricalDrill(1, card(1, Hand.RIGHT), card(45, Hand.LEFT))
        assertThrows(IllegalStateException::class.java) { drill.card(Hand.NEUTRAL) }
    }

    @Test
    @DisplayName("a right-hand card that isn't actually RIGHT is rejected")
    fun `a mismatched right-hand card is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            HistoricalDrill(1, card(1, Hand.LEFT), card(45, Hand.LEFT))
        }
    }

    @Test
    @DisplayName("a number outside 1..44 is rejected")
    fun `a number outside 1 to 44 is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            HistoricalDrill(45, card(1, Hand.RIGHT), card(45, Hand.LEFT))
        }
    }
}
