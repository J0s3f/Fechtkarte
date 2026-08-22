package at.j0s.meyercard.app.domain

import kotlin.math.abs

/**
 * The eight rays a Meyer Square action can start on, in clockwise compass order
 * starting from the top. Order matters: [angularDistanceTo] depends on it.
 */
enum class Direction {
    N, NE, E, SE, S, SW, W, NW;

    /** Where this ray meets the card rectangle, in normalised card space. */
    fun edgePointNormalised(): CardPoint = when (this) {
        N -> CardPoint(0.5f, 0f)
        NE -> CardPoint(1f, 0f)
        E -> CardPoint(1f, CARD_ASPECT_INVERSE / 2f)
        SE -> CardPoint(1f, CARD_ASPECT_INVERSE)
        S -> CardPoint(0.5f, CARD_ASPECT_INVERSE)
        SW -> CardPoint(0f, CARD_ASPECT_INVERSE)
        W -> CardPoint(0f, CARD_ASPECT_INVERSE / 2f)
        NW -> CardPoint(0f, 0f)
    }

    /**
     * The horizontal mirror used by the 44 historical drill pairs (cards 1-44
     * mirrored to 45-88): N and S are fixed, the diagonal and lateral pairs
     * swap sides.
     */
    fun mirrored(): Direction = when (this) {
        N -> N
        S -> S
        NE -> NW
        NW -> NE
        E -> W
        W -> E
        SE -> SW
        SW -> SE
    }

    /** Steps around the eight-ray compass to [other], 0..4. */
    fun angularDistanceTo(other: Direction): Int {
        val step = abs(ordinal - other.ordinal)
        return minOf(step, entries.size - step)
    }
}
