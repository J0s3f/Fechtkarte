package at.j0s.meyercard.app.domain

/**
 * Which hand a card trains. NEUTRAL is for technique cards (89-109) that
 * aren't handedness-specific.
 */
enum class Hand {
    LEFT, RIGHT, NEUTRAL;

    fun opposite(): Hand = when (this) {
        LEFT -> RIGHT
        RIGHT -> LEFT
        NEUTRAL -> NEUTRAL
    }
}
