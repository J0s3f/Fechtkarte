package at.j0s.meyercard.app.domain

/**
 * One of the 44 historical drills, presentable for either hand — cards 1-44
 * and their horizontal mirrors 45-88 aren't 88 separate drills, they're 44
 * drills shown twice. [number] is the drill's number, 1-44, matching
 * [rightHandCard]'s [CardOrigin.Historical.number].
 */
data class HistoricalDrill(val number: Int, val rightHandCard: MeyerCard, val leftHandCard: MeyerCard) {
    init {
        require(number in 1..44) { "number must be in 1..44, was $number" }
        require(rightHandCard.hand == Hand.RIGHT) { "rightHandCard must be Hand.RIGHT, was ${rightHandCard.hand}" }
        require(leftHandCard.hand == Hand.LEFT) { "leftHandCard must be Hand.LEFT, was ${leftHandCard.hand}" }
    }

    fun card(hand: Hand): MeyerCard = when (hand) {
        Hand.RIGHT -> rightHandCard
        Hand.LEFT -> leftHandCard
        Hand.NEUTRAL -> error("a historical drill has no neutral-hand rendering")
    }
}
