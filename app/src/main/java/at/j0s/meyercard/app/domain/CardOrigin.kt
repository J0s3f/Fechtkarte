package at.j0s.meyercard.app.domain

import java.time.Instant

/** Where a card came from: a historical manuscript pattern, or the generator. */
sealed interface CardOrigin {
    /**
     * [number] is the card's number in the historical dataset, 1-109.
     * [sourceNote] records a deviation from the source artwork worth
     * surfacing — e.g. cards 25/69's renumbering, see D3 in
     * `DESIGN_CHOICES.md`. Absent for the other 107 cards.
     */
    data class Historical(val number: Int, val sourceNote: String? = null) : CardOrigin

    data class Generated(val at: Instant) : CardOrigin
}
