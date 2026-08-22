package at.j0s.meyercard.app.adapter.ui.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import at.j0s.meyercard.app.domain.CARD_ASPECT_INVERSE
import at.j0s.meyercard.app.domain.CardPalette
import at.j0s.meyercard.app.domain.Direction
import at.j0s.meyercard.app.domain.Radius
import at.j0s.meyercard.app.domain.Slot
import at.j0s.meyercard.app.domain.toCardPoint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.math.hypot

/**
 * The arithmetic parts of [CardRenderer], extracted so they're checkable on
 * the JVM without a Compose rendering harness. The actual `canvas.drawXxx`
 * calls are covered by T2.4's screenshot tests, not here — see
 * docs/NEXT_STEPS.md T2.2.
 */
class CardRendererGeometryTest {

    @Test
    @DisplayName("disc radius is half of 14% of card width")
    fun `disc radius is half of 14 percent of card width`() {
        assertEquals(70f, discRadiusPx(1000f))
    }

    @Test
    @DisplayName("thrust dot radius is half of 3% of card width")
    fun `thrust dot radius is half of 3 percent of card width`() {
        assertEquals(15f, thrustDotRadiusPx(1000f))
    }

    @Test
    @DisplayName("corner radius is 2% of card width")
    fun `corner radius is 2 percent of card width`() {
        assertEquals(20f, cornerRadiusPx(1000f))
    }

    @Test
    @DisplayName("a thrust dot sits directly below its numeral, clearing the disc by a fixed margin")
    fun `a thrust dot sits directly below its own disc`() {
        val discCenter = Offset(100f, 100f)
        val cardWidthPx = 1000f

        val dot = thrustDotCenter(discCenter, cardWidthPx)

        assertEquals(discCenter.x, dot.x, 0.01f)
        val expectedOffset = discRadiusPx(cardWidthPx) + thrustDotRadiusPx(cardWidthPx) + cardWidthPx * THRUST_DOT_GAP_FRACTION
        assertEquals(discCenter.y + expectedOffset, dot.y, 0.01f)
    }

    @Test
    @DisplayName("a thrust dot stays below its numeral even for slots in the lower half of the card")
    fun `a thrust dot stays below its numeral in the lower half`() {
        // The case that exposed the original bug: an earlier implementation offset the dot
        // *toward the card centre*, which for anything below the centre put the dot ABOVE its
        // numeral — contradicting the notation itself (a thrust is a mark beneath the number)
        // and the Learn screen's own wording. Indistinguishable from the correct behaviour for
        // upper-half slots, which is why it survived until a lower-half thrust was looked at.
        val widthPx = 1000f
        val southOuter = Slot(Direction.S, Radius.OUTER)
        val discCenter = southOuter.toCardPoint().let { Offset(it.x * widthPx, it.y * widthPx) }

        val dot = thrustDotCenter(discCenter, widthPx)

        assertTrue(dot.y > discCenter.y, "thrust dot (${dot.y}) should sit below its numeral (${discCenter.y})")
    }

    @Test
    @DisplayName("every generator slot's thrust dot sits clearly closer to its own disc than to any other")
    fun `a thrust dot sits clearly closer to its own disc than to any other`() {
        val widthPx = 1000f
        val centers = Slot.GENERATOR_SLOTS.associateWith { slot ->
            slot.toCardPoint().let { Offset(it.x * widthPx, it.y * widthPx) }
        }

        for (slot in Slot.GENERATOR_SLOTS) {
            val discCenter = centers.getValue(slot)
            val dot = thrustDotCenter(discCenter, widthPx)
            val ownDistance = hypot(dot.x - discCenter.x, dot.y - discCenter.y)

            val closestOtherDistance = Slot.GENERATOR_SLOTS
                .filter { it != slot }
                .minOf { other -> hypot(dot.x - centers.getValue(other).x, dot.y - centers.getValue(other).y) }

            assertTrue(
                ownDistance < closestOtherDistance,
                "$slot's thrust dot is not clearly closer to its own disc ($ownDistance) than the nearest other ($closestOtherDistance)",
            )
        }
    }

    /**
     * A user found two numeral discs overlapping outright on a real generated card — not a
     * thrust-dot problem at all, a *disc*-vs-disc one, on the two shortest rays (E, W): the
     * card's own aspect ratio makes them shorter than N/S or the diagonals, so [Radius.OUTER]
     * and the original [Radius.INNER] (0.50) put those two discs closer together than twice
     * their own radius. Checked exhaustively across all 16 [Slot.GENERATOR_SLOTS], not just
     * the E/W pair that was actually observed — a narrower check missed this exact bug once
     * already (T8's own earlier thrust-dot regression test only checked S). See
     * DESIGN_CHOICES.md for the fix (`Radius.INNER` moved to 0.32).
     */
    @Test
    @DisplayName("no two of the 16 generator slots' discs overlap")
    fun `no two generator slot discs overlap`() {
        val widthPx = 1000f
        val discRadius = discRadiusPx(widthPx)
        val centers = Slot.GENERATOR_SLOTS.associateWith { slot ->
            slot.toCardPoint().let { Offset(it.x * widthPx, it.y * widthPx) }
        }

        for (slotA in Slot.GENERATOR_SLOTS) {
            for (slotB in Slot.GENERATOR_SLOTS) {
                if (slotA == slotB) continue
                val (centerA, centerB) = centers.getValue(slotA) to centers.getValue(slotB)
                val distance = hypot(centerA.x - centerB.x, centerA.y - centerB.y)
                assertTrue(
                    distance >= 2 * discRadius,
                    "$slotA and $slotB discs overlap: centre distance $distance < ${2 * discRadius}",
                )
            }
        }
    }

    @Test
    @DisplayName("no generator slot's thrust dot lands inside any other generator slot's disc")
    fun `no thrust dot lands inside any other slot's disc`() {
        val widthPx = 1000f
        val minDistance = discRadiusPx(widthPx) + thrustDotRadiusPx(widthPx)
        val centers = Slot.GENERATOR_SLOTS.associateWith { slot ->
            slot.toCardPoint().let { Offset(it.x * widthPx, it.y * widthPx) }
        }

        for (thrustingSlot in Slot.GENERATOR_SLOTS) {
            val dot = thrustDotCenter(centers.getValue(thrustingSlot), widthPx)

            for (otherSlot in Slot.GENERATOR_SLOTS) {
                val distance = hypot(dot.x - centers.getValue(otherSlot).x, dot.y - centers.getValue(otherSlot).y)
                assertTrue(
                    distance >= minDistance,
                    "$thrustingSlot's thrust dot lands too close to $otherSlot's disc: distance $distance < $minDistance",
                )
            }
        }
    }

    @Test
    @DisplayName("a disc at the card centre still gets its thrust dot below it")
    fun `a disc at the card centre still gets its dot below it`() {
        // Radius.CENTRE used to be a degenerate case needing its own guard, back when the dot
        // was positioned relative to the card centre — a disc *at* the centre had no direction
        // to move in. Placing it below the numeral unconditionally removes that special case
        // entirely rather than handling it.
        val center = Offset(500f, 500f)
        val dot = thrustDotCenter(center, 1000f)
        assertEquals(center.x, dot.x, 0.01f)
        assertTrue(dot.y > center.y, "thrust dot should sit below even a centre disc")
    }

    @Test
    @DisplayName("light theme resolves to the palette's own light/dark shades and the light-theme ink/paper")
    fun `light theme resolves the light-theme colours`() {
        val colors = resolveColors(CardPalette.WOAD, isDarkTheme = false)

        assertEquals(Color(CardPalette.WOAD.light), colors.light)
        assertEquals(Color(CardPalette.WOAD.dark), colors.dark)
        assertEquals(Color(CardPalette.INK), colors.ink)
        assertEquals(Color(CardPalette.PAPER), colors.paper)
    }

    @Test
    @DisplayName("dark theme resolves to the palette's dark-theme shades and the dark-theme ink/paper")
    fun `dark theme resolves the dark-theme colours`() {
        val colors = resolveColors(CardPalette.WOAD, isDarkTheme = true)

        assertEquals(Color(CardPalette.WOAD.darkThemeLight), colors.light)
        assertEquals(Color(CardPalette.WOAD.darkThemeDark), colors.dark)
        assertEquals(Color(CardPalette.DARK_THEME_INK), colors.ink)
        assertEquals(Color(CardPalette.DARK_THEME_PAPER), colors.paper)
    }
}
