package at.j0s.meyercard.app.domain

/**
 * A complete drill card: a sequence of actions, the hand it trains, and where
 * it came from. See docs/PLAN.md §3.
 */
data class MeyerCard(
    val id: CardId,
    val actions: List<Action>,
    val hand: Hand,
    val palette: CardPalette,
    val instruction: Instruction? = null,
    val origin: CardOrigin,
) {
    init {
        require(actions.isNotEmpty()) { "actions must be non-empty" }
        require(actions.size <= 8) { "actions must be at most 8, was ${actions.size}" }

        val sequenceNumbers = actions.map { it.sequenceNumber }
        require(sequenceNumbers.sorted() == (1..actions.size).toList()) {
            "sequence numbers must be exactly 1..${actions.size}, each once, was $sequenceNumbers"
        }

        val slots = actions.map { it.slot }
        require(slots.toSet().size == slots.size) { "no two actions may share a slot" }
    }
}
