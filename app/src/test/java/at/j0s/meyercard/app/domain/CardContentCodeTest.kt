package at.j0s.meyercard.app.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.Instant
import kotlin.random.Random

/**
 * A fixed, known set of (card, line style) -> code pairs — [GOLDEN_CODES] — pins today's exact
 * encoding, both directions, independently of [contentCode]/[decodeCardContent] themselves. The
 * existing round-trip tests (`round trip holds across the full generator slot space`) only prove
 * `decode(encode(x)) == x`; they'd stay green even if encode and decode both changed the same way
 * at once, which is exactly the kind of accidental drift a code that's now embedded as permanent
 * PNG/PDF metadata (`ExportMetadata.kt`) can't afford — an already-exported file's code has to
 * keep meaning what it meant when it was written. These values were generated once from the real
 * implementation (not hand-computed) and are now the thing under test, not a derived expectation.
 */
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
    @DisplayName("decoding a card's own code reconstructs its exact palette, line style and actions")
    fun `round trip reconstructs palette, line style and actions exactly`() {
        val original = card(someActions, palette = CardPalette.MADDER)
        val (decodedPalette, decodedLineStyle, decodedActions) = decodeCardContent(original.contentCode(CardLineStyle.SEQUENCE))
        assertEquals(CardPalette.MADDER, decodedPalette)
        assertEquals(CardLineStyle.SEQUENCE, decodedLineStyle)
        assertEquals(someActions, decodedActions)
    }

    @Test
    @DisplayName("round trip holds for every direction, both radii, thrust on and off, both line styles, and 1..8 actions")
    fun `round trip holds across the full generator slot space`() {
        repeat(500) { seed ->
            val random = Random(seed)
            val actionCount = random.nextInt(1, 9)
            val slots = Slot.GENERATOR_SLOTS.shuffled(random).take(actionCount)
            val actions = slots.mapIndexed { index, slot -> Action(index + 1, slot, isThrust = random.nextBoolean()) }
            val palette = CardPalette.entries.random(random)
            val lineStyle = CardLineStyle.entries.random(random)
            val original = card(actions, palette = palette)

            val (decodedPalette, decodedLineStyle, decodedActions) = decodeCardContent(original.contentCode(lineStyle))
            assertEquals(palette, decodedPalette, "seed $seed: palette")
            assertEquals(lineStyle, decodedLineStyle, "seed $seed: line style")
            assertEquals(actions, decodedActions, "seed $seed: actions")
        }
    }

    @Test
    @DisplayName("the same actions, palette and line style always produce the same code, regardless of generation time")
    fun `identical content codes identically`() {
        val first = card(someActions, origin = CardOrigin.Generated(Instant.EPOCH))
        val second = card(someActions, origin = CardOrigin.Generated(Instant.EPOCH.plusSeconds(60)))
        assertEquals(first.contentCode(CardLineStyle.COMPASS), second.contentCode(CardLineStyle.COMPASS))
    }

    @Test
    @DisplayName("a different palette produces a different code")
    fun `different palette produces a different code`() {
        val woad = card(someActions, palette = CardPalette.WOAD)
        val iris = card(someActions, palette = CardPalette.IRIS)
        assertNotEquals(woad.contentCode(CardLineStyle.COMPASS), iris.contentCode(CardLineStyle.COMPASS))
    }

    @Test
    @DisplayName("a different line style produces a different code")
    fun `different line style produces a different code`() {
        val sameCard = card(someActions)
        assertNotEquals(sameCard.contentCode(CardLineStyle.COMPASS), sameCard.contentCode(CardLineStyle.SEQUENCE))
    }

    @Test
    @DisplayName("action order in the list doesn't matter, only sequence number does")
    fun `list order is irrelevant, sequence number is what counts`() {
        val inOrder = card(someActions)
        val reversed = card(someActions.reversed())
        assertEquals(inOrder.contentCode(CardLineStyle.COMPASS), reversed.contentCode(CardLineStyle.COMPASS))
    }

    @Test
    @DisplayName("the code uses only filename-safe, human-unambiguous characters")
    fun `code is filename-safe`() {
        val code = card(someActions).contentCode(CardLineStyle.COMPASS)
        assertTrue(code.all { it in "0123456789ABCDEFGHJKMNPQRSTVWXYZ" }, "unexpected characters in '$code'")
    }

    @Test
    @DisplayName("a code carrying a different version than this decoder understands is rejected, not silently misread")
    fun `unsupported version is rejected`() {
        // The version occupies the top 2 of the first character's 5 bits (MSB-first packing);
        // today's codes always encode version 0, so that character's alphabet index is always
        // 0..7 (the low 3 bits are the palette's own top bits). Adding 8 flips exactly the
        // version's low bit — 0 becomes 1 — without touching anything else the code carries.
        val alphabet = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"
        val code = card(someActions).contentCode(CardLineStyle.COMPASS)
        val firstCharIndex = alphabet.indexOf(code[0])
        val mutated = alphabet[firstCharIndex + 8] + code.substring(1)

        assertThrows(IllegalArgumentException::class.java) { decodeCardContent(mutated) }
    }

    @Test
    @DisplayName("an eight-action card's code is still noticeably shorter than a 16-character hash would be")
    fun `code stays short even at the maximum action count`() {
        val slots = Slot.GENERATOR_SLOTS.take(8)
        val actions = slots.mapIndexed { index, slot -> Action(index + 1, slot, isThrust = false) }
        val code = card(actions).contentCode(CardLineStyle.SEQUENCE)
        // 12, not 10 — see CardContentCode.kt's own VERSION_BITS comment for why adding the
        // version field cost 2 characters here specifically, not the free ride it is for most
        // action counts.
        assertTrue(code.length <= 12, "expected at most 12 characters, got ${code.length}: $code")
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("goldenCodes")
    @DisplayName("a known card and line style always produces its known, previously-recorded code")
    fun `encoding a known card produces its known code`(golden: GoldenCode) {
        assertEquals(golden.code, card(golden.actions, golden.palette, golden.hand).contentCode(golden.lineStyle), golden.label)
    }

    @Test
    @DisplayName("hand has no effect on the code — deliberately excluded, per this file's own doc comment — for right, left and neutral alike")
    fun `hand does not affect the code`() {
        val right = card(someActions, hand = Hand.RIGHT).contentCode(CardLineStyle.COMPASS)
        val left = card(someActions, hand = Hand.LEFT).contentCode(CardLineStyle.COMPASS)
        val neutral = card(someActions, hand = Hand.NEUTRAL).contentCode(CardLineStyle.COMPASS)
        assertEquals(right, left)
        assertEquals(right, neutral)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("goldenCodes")
    @DisplayName("a known code always decodes back to its known palette, line style and actions")
    fun `decoding a known code reconstructs its known card`(golden: GoldenCode) {
        val (palette, lineStyle, actions) = decodeCardContent(golden.code)
        assertEquals(golden.palette, palette, "${golden.label}: palette")
        assertEquals(golden.lineStyle, lineStyle, "${golden.label}: line style")
        assertEquals(golden.actions, actions, "${golden.label}: actions")
    }

    /** One fixed, known (card, line style) -> code pairing — see the class doc comment for why this exists. */
    data class GoldenCode(
        val label: String,
        val actions: List<Action>,
        val palette: CardPalette,
        val lineStyle: CardLineStyle,
        val code: String,
        // Deliberately varied across fixtures despite never affecting the code (see `hand does
        // not affect the code`) — proves that in the one place a golden code is actually
        // asserted end to end, not just in an isolated equality check.
        val hand: Hand = Hand.RIGHT,
    ) {
        override fun toString() = label
    }

    companion object {
        /**
         * Diverse by design, not random: every action count from the encoding's two costliest
         * cases (3 and 8 actions — see [CardContentCodeTest]'s and `CardContentCode.kt`'s own
         * `VERSION_BITS` comments for why those two specifically) down to the minimum (1), every
         * palette touched at least once, both line styles, one fixture on [Hand.LEFT] (the code
         * is identical either way — see `hand does not affect the code` — this just proves that
         * holds in the one place a golden value is actually asserted, not only in isolation),
         * and a mix of directions/radii/thrust flags rather than a single repeated pattern.
         * Generated once from the real implementation (`ScratchPrintCodesTest`, not committed)
         * and pinned here as data, not derived at test-run time — see the class doc comment.
         */
        @JvmStatic
        fun goldenCodes(): List<GoldenCode> = listOf(
            GoldenCode(
                label = "one action, WOAD, COMPASS",
                actions = listOf(Action(1, Slot(Direction.N, Radius.OUTER), isThrust = false)),
                palette = CardPalette.WOAD,
                lineStyle = CardLineStyle.COMPASS,
                code = "1020",
            ),
            GoldenCode(
                label = "two actions, IRIS, SEQUENCE, left hand",
                actions = listOf(
                    Action(1, Slot(Direction.SE, Radius.INNER), isThrust = true),
                    Action(2, Slot(Direction.W, Radius.OUTER), isThrust = false),
                ),
                palette = CardPalette.IRIS,
                lineStyle = CardLineStyle.SEQUENCE,
                code = "09DT0",
                hand = Hand.LEFT,
            ),
            GoldenCode(
                label = "three actions, VERDIGRIS, COMPASS",
                actions = listOf(
                    Action(1, Slot(Direction.NE, Radius.OUTER), isThrust = false),
                    Action(2, Slot(Direction.S, Radius.INNER), isThrust = true),
                    Action(3, Slot(Direction.NW, Radius.OUTER), isThrust = false),
                ),
                palette = CardPalette.VERDIGRIS,
                lineStyle = CardLineStyle.COMPASS,
                code = "226HY00",
            ),
            GoldenCode(
                label = "five actions, MOSS, SEQUENCE",
                actions = listOf(
                    Action(1, Slot(Direction.N, Radius.OUTER), isThrust = false),
                    Action(2, Slot(Direction.E, Radius.INNER), isThrust = true),
                    Action(3, Slot(Direction.S, Radius.OUTER), isThrust = false),
                    Action(4, Slot(Direction.W, Radius.INNER), isThrust = true),
                    Action(5, Slot(Direction.NW, Radius.OUTER), isThrust = false),
                ),
                palette = CardPalette.MOSS,
                lineStyle = CardLineStyle.SEQUENCE,
                code = "3C29JSY0",
            ),
            GoldenCode(
                label = "eight actions, MADDER, SEQUENCE",
                actions = listOf(
                    Action(1, Slot(Direction.N, Radius.OUTER), isThrust = false),
                    Action(2, Slot(Direction.NE, Radius.INNER), isThrust = true),
                    Action(3, Slot(Direction.E, Radius.OUTER), isThrust = false),
                    Action(4, Slot(Direction.SE, Radius.INNER), isThrust = true),
                    Action(5, Slot(Direction.S, Radius.OUTER), isThrust = false),
                    Action(6, Slot(Direction.SW, Radius.INNER), isThrust = true),
                    Action(7, Slot(Direction.W, Radius.OUTER), isThrust = false),
                    Action(8, Slot(Direction.NW, Radius.INNER), isThrust = true),
                ),
                palette = CardPalette.MADDER,
                lineStyle = CardLineStyle.SEQUENCE,
                code = "4F25ADJNTX00",
            ),
        )
    }
}
