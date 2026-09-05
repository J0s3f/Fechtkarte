package at.j0s.meyercard.app.adapter.ui.library

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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.random.Random

private fun drill(number: Int, actionCount: Int = 2, thrustCount: Int = 0): HistoricalDrill {
    fun card(hand: Hand, id: Int) = MeyerCard(
        id = CardId(id.toLong()),
        actions = Direction.entries.take(actionCount).mapIndexed { i, direction ->
            Action(sequenceNumber = i + 1, slot = Slot(direction, Radius.OUTER), isThrust = i < thrustCount)
        },
        hand = hand,
        palette = CardPalette.default(hand),
        origin = CardOrigin.Historical(number = id),
    )
    return HistoricalDrill(number, card(Hand.RIGHT, number), card(Hand.LEFT, number + 44))
}

private val fourDrills = listOf(
    drill(1, actionCount = 2, thrustCount = 0),
    drill(2, actionCount = 3, thrustCount = 1),
    drill(3, actionCount = 3, thrustCount = 0),
    drill(4, actionCount = 5, thrustCount = 2),
)

class DrillsLibraryStateTest {

    @Test
    @DisplayName("starts on the first drill, right hand")
    fun `starts on the first drill right hand`() {
        val state = DrillsLibraryState(fourDrills)
        assertEquals(1, state.current?.number)
        assertEquals(Hand.RIGHT, state.hand)
    }

    @Test
    @DisplayName("toggleHand flips right to left and back")
    fun `toggleHand flips right to left and back`() {
        val state = DrillsLibraryState(fourDrills)
        assertEquals(Hand.LEFT, state.toggleHand().hand)
        assertEquals(Hand.RIGHT, state.toggleHand().toggleHand().hand)
    }

    @Test
    @DisplayName("next and previous move by one drill and clamp at the ends")
    fun `next and previous move and clamp`() {
        val state = DrillsLibraryState(fourDrills)
        assertEquals(2, state.next().current?.number)
        assertEquals(1, state.previous().current?.number)
        assertEquals(4, state.last().next().current?.number)
    }

    @Test
    @DisplayName("filtering by action count narrows the visible drills and resets to the first one")
    fun `filtering narrows visible drills and resets position`() {
        val state = DrillsLibraryState(fourDrills).last()
        val filtered = state.withFilter(DrillFilter(actionCount = 3))
        assertEquals(listOf(2, 3), filtered.visibleDrills.map { it.number })
        assertEquals(2, filtered.current?.number)
    }

    @Test
    @DisplayName("filtering by thrust count narrows the visible drills")
    fun `filtering by thrust count narrows visible drills`() {
        val filtered = DrillsLibraryState(fourDrills).withFilter(DrillFilter(thrustCount = 0))
        assertEquals(listOf(1, 3), filtered.visibleDrills.map { it.number })
    }

    @Test
    @DisplayName("a filter matching nothing leaves current null rather than throwing")
    fun `a filter matching nothing leaves current null`() {
        val filtered = DrillsLibraryState(fourDrills).withFilter(DrillFilter(actionCount = 8))
        assertEquals(emptyList<HistoricalDrill>(), filtered.visibleDrills)
        assertNull(filtered.current)
    }

    @Test
    @DisplayName("first jumps back to the first drill")
    fun `first jumps to the first drill`() {
        val state = DrillsLibraryState(fourDrills).last()
        assertEquals(1, state.first().current?.number)
    }

    @Test
    @DisplayName("fastForward and fastBackward move by ten and clamp at the ends")
    fun `fastForward and fastBackward move by ten and clamp`() {
        val state = DrillsLibraryState(fourDrills)
        assertEquals(4, state.fastForward().current?.number)
        assertEquals(1, state.last().fastBackward().current?.number)
    }

    @Test
    @DisplayName("random lands on one of the visible drills")
    fun `random lands on a visible drill`() {
        val landed = DrillsLibraryState(fourDrills).random(Random(1)).current?.number
        assertTrue(fourDrills.map { it.number }.contains(landed))
    }

    @Test
    @DisplayName("random on an empty filtered list stays null rather than throwing")
    fun `random on an empty filtered list stays null`() {
        val filtered = DrillsLibraryState(fourDrills).withFilter(DrillFilter(actionCount = 8))
        assertNull(filtered.random().current)
    }
}
