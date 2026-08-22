package at.j0s.meyercard.app.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.random.Random

class CardContentCodeTest {

    private fun card(
        actions: List<Action>,
        palette: CardPalette = CardPalette.WOAD,
        hand: Hand = Hand.RIGHT,
        origin: CardOrigin = CardOrigin.Generated(Instant.EPOCH),
    ) = MeyerCard(id = CardId(1L), actions = actions, hand = hand, palette = palette, origin = origin)

    private val someActions = listOf(
        Action(1, Slot(Direction.N, Radius.OUTER), isThrust = false),
        Action(2, Slot(Direction.SE, Radius.INNER), isThrust = true),
    )

    @Test
    @DisplayName("decoding a card's own code reconstructs its exact palette and actions")
    fun `round trip reconstructs palette and actions exactly`() {
        val original = card(someActions, palette = CardPalette.MADDER)
        val (decodedPalette, decodedActions) = decodeCardContent(original.contentCode())
        assertEquals(CardPalette.MADDER, decodedPalette)
        assertEquals(someActions, decodedActions)
    }

    @Test
    @DisplayName("round trip holds for every direction, both radii, thrust on and off, and 1..8 actions")
    fun `round trip holds across the full generator slot space`() {
        repeat(500) { seed ->
            val random = Random(seed)
            val actionCount = random.nextInt(1, 9)
            val slots = Slot.GENERATOR_SLOTS.shuffled(random).take(actionCount)
            val actions = slots.mapIndexed { index, slot -> Action(index + 1, slot, isThrust = random.nextBoolean()) }
            val palette = CardPalette.entries.random(random)
            val original = card(actions, palette = palette)

            val (decodedPalette, decodedActions) = decodeCardContent(original.contentCode())
            assertEquals(palette, decodedPalette, "seed $seed: palette")
            assertEquals(actions, decodedActions, "seed $seed: actions")
        }
    }

    @Test
    @DisplayName("the same actions and palette always produce the same code, regardless of generation time")
    fun `identical content codes identically`() {
        val first = card(someActions, origin = CardOrigin.Generated(Instant.EPOCH))
        val second = card(someActions, origin = CardOrigin.Generated(Instant.EPOCH.plusSeconds(60)))
        assertEquals(first.contentCode(), second.contentCode())
    }

    @Test
    @DisplayName("a different palette produces a different code")
    fun `different palette produces a different code`() {
        val woad = card(someActions, palette = CardPalette.WOAD)
        val iris = card(someActions, palette = CardPalette.IRIS)
        assertNotEquals(woad.contentCode(), iris.contentCode())
    }

    @Test
    @DisplayName("action order in the list doesn't matter, only sequence number does")
    fun `list order is irrelevant, sequence number is what counts`() {
        val inOrder = card(someActions)
        val reversed = card(someActions.reversed())
        assertEquals(inOrder.contentCode(), reversed.contentCode())
    }

    @Test
    @DisplayName("the code uses only filename-safe, human-unambiguous characters")
    fun `code is filename-safe`() {
        val code = card(someActions).contentCode()
        assertTrue(code.all { it in "0123456789ABCDEFGHJKMNPQRSTVWXYZ" }, "unexpected characters in '$code'")
    }

    @Test
    @DisplayName("an eight-action card's code is still noticeably shorter than a 16-character hash would be")
    fun `code stays short even at the maximum action count`() {
        val slots = Slot.GENERATOR_SLOTS.take(8)
        val actions = slots.mapIndexed { index, slot -> Action(index + 1, slot, isThrust = false) }
        val code = card(actions).contentCode()
        assertTrue(code.length <= 10, "expected at most 10 characters, got ${code.length}: $code")
    }
}
