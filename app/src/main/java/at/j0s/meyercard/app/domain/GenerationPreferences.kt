package at.j0s.meyercard.app.domain

import kotlin.random.Random

/**
 * The user's saved generator configuration. The
 * `thrustCount ≤ actionCount` invariant is enforced here, in the type, so no
 * screen can put the model into an invalid state — validation lives once,
 * at construction, not scattered across whichever UI happens to edit it.
 *
 * [actionCount]/[thrustCount] stay the *configured* ceiling even when
 * [actionCountIsMaximum]/[thrustCountIsMaximum] are set — [resolveCounts] is where a maximum
 * turns into the actual value for one card, so this type's own invariant never has to reason
 * about randomness.
 */
data class GenerationPreferences(
    val actionCount: Int = 4,
    val thrustCount: Int = 0,
    val actionCountIsMaximum: Boolean = false,
    val thrustCountIsMaximum: Boolean = false,
    val rightHandPalette: CardPalette = CardPalette.DEFAULT_RIGHT,
    val leftHandPalette: CardPalette = CardPalette.DEFAULT_LEFT,
    val enabledRules: List<GenerationRule> = emptyList(),
    val cardLineStyle: CardLineStyle = CardLineStyle.COMPASS,
    val shakeToGenerateEnabled: Boolean = true,
) {
    init {
        require(actionCount in 1..8) { "actionCount must be in 1..8, was $actionCount" }
        require(thrustCount in 0..actionCount) {
            "thrustCount must be in 0..actionCount ($actionCount), was $thrustCount"
        }
    }

    /**
     * The actionCount/thrustCount to use for one generated card. When
     * [actionCountIsMaximum]/[thrustCountIsMaximum] are set, each call draws a fresh random
     * value up to the configured count instead of using it exactly — "up to N actions" rather
     * than always exactly N.
     *
     * [thrustCount] is re-clamped against the *drawn* action count, not the configured one:
     * a draw that comes up short on actions can't call for more thrusts than it has actions to
     * put them on, even when [thrustCount] itself wasn't drawn as a maximum.
     */
    fun resolveCounts(random: Random = Random): Pair<Int, Int> {
        val resolvedActionCount = if (actionCountIsMaximum) random.nextInt(1, actionCount + 1) else actionCount
        val resolvedThrustCount = if (thrustCountIsMaximum) random.nextInt(0, thrustCount + 1) else thrustCount
        return resolvedActionCount to resolvedThrustCount.coerceAtMost(resolvedActionCount)
    }
}
