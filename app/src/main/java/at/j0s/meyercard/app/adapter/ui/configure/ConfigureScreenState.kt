package at.j0s.meyercard.app.adapter.ui.configure

import at.j0s.meyercard.app.domain.CardPalette
import at.j0s.meyercard.app.domain.GenerationPreferences
import at.j0s.meyercard.app.domain.GenerationRule
import at.j0s.meyercard.app.domain.MinimumAngularDistance

private const val DEFAULT_MINIMUM_ANGULAR_DISTANCE_STEPS = 2

/**
 * The Configure screen's state: edits to [GenerationPreferences] that keep
 * its own invariant (`thrustCount ≤ actionCount`) satisfied at every step,
 * not just at the end — so the screen can never briefly hold an
 * unconstructable value while the user is still dragging a slider. Plain
 * data, no Compose dependency, so it's directly unit-testable; the
 * Composable screen holds one of these in `remember`.
 */
data class ConfigureScreenState(val preferences: GenerationPreferences) {

    fun withActionCount(newActionCount: Int) = copy(
        preferences = preferences.copy(
            actionCount = newActionCount,
            thrustCount = minOf(preferences.thrustCount, newActionCount),
        ),
    )

    fun withThrustCount(newThrustCount: Int) = copy(preferences = preferences.copy(thrustCount = newThrustCount))

    fun toggleActionCountIsMaximum() =
        copy(preferences = preferences.copy(actionCountIsMaximum = !preferences.actionCountIsMaximum))

    fun toggleThrustCountIsMaximum() =
        copy(preferences = preferences.copy(thrustCountIsMaximum = !preferences.thrustCountIsMaximum))

    fun withRightHandPalette(palette: CardPalette) = copy(preferences = preferences.copy(rightHandPalette = palette))

    fun withLeftHandPalette(palette: CardPalette) = copy(preferences = preferences.copy(leftHandPalette = palette))

    fun isRuleEnabled(rule: GenerationRule): Boolean = rule in preferences.enabledRules

    fun toggleRule(rule: GenerationRule): ConfigureScreenState {
        val newRules = if (isRuleEnabled(rule)) preferences.enabledRules - rule else preferences.enabledRules + rule
        return copy(preferences = preferences.copy(enabledRules = newRules))
    }

    /**
     * [MinimumAngularDistance] carries its own step count, so "is it
     * enabled" means "is any instance present", not an exact-value lookup
     * like [isRuleEnabled] — the step is adjusted separately, via
     * [withMinimumAngularDistanceSteps], without disabling and re-enabling
     * the rule.
     */
    fun isMinimumAngularDistanceEnabled(): Boolean = preferences.enabledRules.any { it is MinimumAngularDistance }

    fun toggleMinimumAngularDistance(): ConfigureScreenState {
        val newRules = if (isMinimumAngularDistanceEnabled()) {
            preferences.enabledRules.filterNot { it is MinimumAngularDistance }
        } else {
            preferences.enabledRules + MinimumAngularDistance(DEFAULT_MINIMUM_ANGULAR_DISTANCE_STEPS)
        }
        return copy(preferences = preferences.copy(enabledRules = newRules))
    }

    fun withMinimumAngularDistanceSteps(steps: Int): ConfigureScreenState {
        val newRules = preferences.enabledRules.filterNot { it is MinimumAngularDistance } + MinimumAngularDistance(steps)
        return copy(preferences = preferences.copy(enabledRules = newRules))
    }
}
