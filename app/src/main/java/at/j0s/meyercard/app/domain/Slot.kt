package at.j0s.meyercard.app.domain

/** A position on the card: a ray and a distance along it. */
data class Slot(val direction: Direction, val radius: Radius) {
    companion object {
        /**
         * The 16 positions the drill generator samples without replacement —
         * every direction at both rings.
         */
        val GENERATOR_SLOTS: List<Slot> = Direction.entries.flatMap { direction ->
            listOf(Slot(direction, Radius.OUTER), Slot(direction, Radius.INNER))
        }
    }
}
