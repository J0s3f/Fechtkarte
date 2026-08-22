package at.j0s.meyercard.app.adapter.ui.library

import at.j0s.meyercard.app.domain.HistoricalDrill

/** `null` means "show all" for that criterion. */
data class DrillFilter(val actionCount: Int? = null, val thrustCount: Int? = null) {
    fun matches(drill: HistoricalDrill): Boolean {
        val actions = drill.rightHandCard.actions
        return (actionCount == null || actions.size == actionCount) &&
            (thrustCount == null || actions.count { it.isThrust } == thrustCount)
    }
}
