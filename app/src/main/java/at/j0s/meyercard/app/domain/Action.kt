package at.j0s.meyercard.app.domain

/**
 * A single cut or thrust in a drill sequence. [sequenceNumber] is the numeral
 * printed in the disc — where the drill practitioner starts, nothing more.
 * There is deliberately no end position: this notation marks only where a cut
 * starts, not where it travels to.
 */
data class Action(val sequenceNumber: Int, val slot: Slot, val isThrust: Boolean) {
    init {
        require(sequenceNumber >= 1) { "sequenceNumber must be >= 1, was $sequenceNumber" }
    }
}
