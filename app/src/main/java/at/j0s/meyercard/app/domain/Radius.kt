package at.j0s.meyercard.app.domain

/**
 * A slot's distance from the card centre along its [Direction]'s ray: `0f` at
 * the centre, `1f` at the card edge. Holds the generator's two-ring
 * restriction ([OUTER], [INNER]) and the historical cards' free placement in
 * one type.
 */
@JvmInline
value class Radius(val value: Float) {
    init {
        require(value in 0f..1f) { "Radius must be in 0f..1f, was $value" }
    }

    companion object {
        val CENTRE = Radius(0f)

        /**
         * The generator's inner ring. Originally 0.50 — moved to 0.32 (T8, found on a real
         * generated card) once it became clear that value put the inner and outer discs of
         * the two shortest rays (E, W) close enough to overlap outright, since the card's own
         * aspect ratio makes those rays much shorter than N/S or the diagonals. Chosen by an
         * exhaustive check of every pair of the 16 generator slots — disc-disc and
         * thrust-dot-vs-any-disc — not just the one pair that was actually observed
         * overlapping. Legibility and edge-fidelity mattered more than
         * preserving the original 0.50 measurement exactly here.
         */
        val INNER = Radius(0.32f)

        /** The generator's outer ring. */
        val OUTER = Radius(0.75f)
    }
}
