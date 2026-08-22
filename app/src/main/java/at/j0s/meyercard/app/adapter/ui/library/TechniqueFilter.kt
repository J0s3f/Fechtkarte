package at.j0s.meyercard.app.adapter.ui.library

import at.j0s.meyercard.app.domain.Instruction
import at.j0s.meyercard.app.domain.MeyerCard

/** `null` means "show all", including card 89, which carries no instruction at all. */
data class TechniqueFilter(val instruction: Instruction? = null) {
    fun matches(card: MeyerCard): Boolean = instruction == null || card.instruction == instruction
}
