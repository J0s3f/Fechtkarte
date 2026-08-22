package at.j0s.meyercard.app.adapter.ui.library

import at.j0s.meyercard.app.domain.Action
import at.j0s.meyercard.app.domain.CardId
import at.j0s.meyercard.app.domain.CardOrigin
import at.j0s.meyercard.app.domain.CardPalette
import at.j0s.meyercard.app.domain.Direction
import at.j0s.meyercard.app.domain.Hand
import at.j0s.meyercard.app.domain.Instruction
import at.j0s.meyercard.app.domain.MeyerCard
import at.j0s.meyercard.app.domain.Radius
import at.j0s.meyercard.app.domain.Slot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

private fun techniqueCard(id: Int, instruction: Instruction?) = MeyerCard(
    id = CardId(id.toLong()),
    actions = listOf(Action(1, Slot(Direction.N, Radius.OUTER), isThrust = false)),
    hand = Hand.NEUTRAL,
    palette = CardPalette.default(Hand.NEUTRAL),
    instruction = instruction,
    origin = CardOrigin.Historical(number = id),
)

private val techniqueCards = listOf(
    techniqueCard(89, instruction = null),
    techniqueCard(90, instruction = Instruction.DOUBLE_FEINT),
    techniqueCard(100, instruction = Instruction.MOULINET),
    techniqueCard(104, instruction = Instruction.PROVOKER_TAKER_HITTER),
)

class TechniqueLibraryStateTest {

    @Test
    @DisplayName("starts on the first technique card")
    fun `starts on the first technique card`() {
        assertEquals(89, TechniqueLibraryState(techniqueCards).current?.id?.value?.toInt())
    }

    @Test
    @DisplayName("filtering by instruction narrows to only that technique")
    fun `filtering by instruction narrows to that technique`() {
        val filtered = TechniqueLibraryState(techniqueCards).withFilter(TechniqueFilter(Instruction.MOULINET))
        assertEquals(listOf(100), filtered.visibleCards.map { it.id.value.toInt() })
    }

    @Test
    @DisplayName("the no-instruction card only shows when the filter is unset")
    fun `the no-instruction card only shows when filter is unset`() {
        val unfiltered = TechniqueLibraryState(techniqueCards)
        assertEquals(89, unfiltered.visibleCards.first().id.value.toInt())

        val filtered = unfiltered.withFilter(TechniqueFilter(Instruction.DOUBLE_FEINT))
        assertNull(filtered.visibleCards.find { it.instruction == null })
    }

    @Test
    @DisplayName("next and last move through the visible cards and clamp")
    fun `next and last move and clamp`() {
        val state = TechniqueLibraryState(techniqueCards)
        assertEquals(90, state.next().current?.id?.value?.toInt())
        assertEquals(104, state.last().current?.id?.value?.toInt())
        assertEquals(104, state.last().next().current?.id?.value?.toInt())
    }
}
