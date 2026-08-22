package at.j0s.meyercard.app.domain

/**
 * The user's saved generator configuration (docs/PLAN.md §6). The
 * `thrustCount ≤ actionCount` invariant is enforced here, in the type, so no
 * screen can put the model into an invalid state — validation lives once,
 * at construction, not scattered across whichever UI happens to edit it.
 */
data class GenerationPreferences(
    val actionCount: Int = 4,
    val thrustCount: Int = 0,
    val rightHandPalette: CardPalette = CardPalette.DEFAULT_RIGHT,
    val leftHandPalette: CardPalette = CardPalette.DEFAULT_LEFT,
    val enabledRules: List<GenerationRule> = emptyList(),
) {
    init {
        require(actionCount in 1..8) { "actionCount must be in 1..8, was $actionCount" }
        require(thrustCount in 0..actionCount) {
            "thrustCount must be in 0..actionCount ($actionCount), was $thrustCount"
        }
    }
}
