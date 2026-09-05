package at.j0s.meyercard.app.adapter.ui.configure

import at.j0s.meyercard.app.domain.AlternateHands
import at.j0s.meyercard.app.domain.CardLineStyle
import at.j0s.meyercard.app.domain.CardPalette
import at.j0s.meyercard.app.domain.GenerationPreferences
import at.j0s.meyercard.app.domain.MinimumAngularDistance
import at.j0s.meyercard.app.domain.NoRepeatedDirection
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class ConfigureScreenStateTest {

    @Test
    @DisplayName("lowering actionCount below the current thrustCount clamps thrustCount too")
    fun `lowering actionCount clamps thrustCount`() {
        val state = ConfigureScreenState(GenerationPreferences(actionCount = 6, thrustCount = 5))
        val updated = state.withActionCount(3)
        assertEquals(3, updated.preferences.actionCount)
        assertEquals(3, updated.preferences.thrustCount)
    }

    @Test
    @DisplayName("raising actionCount leaves thrustCount untouched")
    fun `raising actionCount leaves thrustCount untouched`() {
        val state = ConfigureScreenState(GenerationPreferences(actionCount = 3, thrustCount = 1))
        val updated = state.withActionCount(6)
        assertEquals(6, updated.preferences.actionCount)
        assertEquals(1, updated.preferences.thrustCount)
    }

    @Test
    @DisplayName("toggling a simple rule on then off returns to the original set")
    fun `toggling a simple rule on then off returns to the original set`() {
        val state = ConfigureScreenState(GenerationPreferences())
        val toggledOn = state.toggleRule(NoRepeatedDirection)
        assertTrue(toggledOn.isRuleEnabled(NoRepeatedDirection))

        val toggledOff = toggledOn.toggleRule(NoRepeatedDirection)
        assertFalse(toggledOff.isRuleEnabled(NoRepeatedDirection))
        assertEquals(state.preferences.enabledRules, toggledOff.preferences.enabledRules)
    }

    @Test
    @DisplayName("two different rules can be enabled at once, independently")
    fun `two different rules can be enabled independently`() {
        val state = ConfigureScreenState(GenerationPreferences())
            .toggleRule(NoRepeatedDirection)
            .toggleRule(AlternateHands)

        assertTrue(state.isRuleEnabled(NoRepeatedDirection))
        assertTrue(state.isRuleEnabled(AlternateHands))
        assertEquals(2, state.preferences.enabledRules.size)
    }

    @Test
    @DisplayName("MinimumAngularDistance is enabled/disabled as a family, regardless of its step value")
    fun `MinimumAngularDistance is toggled as a family`() {
        val state = ConfigureScreenState(GenerationPreferences(enabledRules = listOf(MinimumAngularDistance(3))))
        assertTrue(state.isMinimumAngularDistanceEnabled())

        val disabled = state.toggleMinimumAngularDistance()
        assertFalse(disabled.isMinimumAngularDistanceEnabled())
        assertTrue(disabled.preferences.enabledRules.none { it is MinimumAngularDistance })
    }

    @Test
    @DisplayName("enabling MinimumAngularDistance adds a default step, changing the step replaces it")
    fun `enabling adds a default step, changing the step replaces it`() {
        val enabled = ConfigureScreenState(GenerationPreferences()).toggleMinimumAngularDistance()
        assertTrue(enabled.isMinimumAngularDistanceEnabled())

        val updated = enabled.withMinimumAngularDistanceSteps(4)
        assertEquals(listOf(MinimumAngularDistance(4)), updated.preferences.enabledRules)
    }

    @Test
    @DisplayName("toggling actionCountIsMaximum flips only that flag")
    fun `toggling actionCountIsMaximum flips only that flag`() {
        val state = ConfigureScreenState(GenerationPreferences(actionCount = 5, thrustCount = 2))
        val toggled = state.toggleActionCountIsMaximum()
        assertTrue(toggled.preferences.actionCountIsMaximum)
        assertFalse(toggled.preferences.thrustCountIsMaximum)
        assertEquals(5, toggled.preferences.actionCount)
        assertEquals(2, toggled.preferences.thrustCount)

        val toggledBack = toggled.toggleActionCountIsMaximum()
        assertFalse(toggledBack.preferences.actionCountIsMaximum)
    }

    @Test
    @DisplayName("toggling thrustCountIsMaximum flips only that flag")
    fun `toggling thrustCountIsMaximum flips only that flag`() {
        val state = ConfigureScreenState(GenerationPreferences(actionCount = 5, thrustCount = 2))
        val toggled = state.toggleThrustCountIsMaximum()
        assertTrue(toggled.preferences.thrustCountIsMaximum)
        assertFalse(toggled.preferences.actionCountIsMaximum)
    }

    @Test
    @DisplayName("card line style defaults to COMPASS, matching today's always-drawn compass")
    fun `card line style defaults to COMPASS`() {
        assertEquals(CardLineStyle.COMPASS, ConfigureScreenState(GenerationPreferences()).preferences.cardLineStyle)
    }

    @Test
    @DisplayName("selecting a card line style replaces only that setting")
    fun `selecting a card line style replaces only that setting`() {
        val state = ConfigureScreenState(GenerationPreferences())

        val sequence = state.withCardLineStyle(CardLineStyle.SEQUENCE)
        assertEquals(CardLineStyle.SEQUENCE, sequence.preferences.cardLineStyle)

        val backToCompass = sequence.withCardLineStyle(CardLineStyle.COMPASS)
        assertEquals(CardLineStyle.COMPASS, backToCompass.preferences.cardLineStyle)
    }

    @Test
    @DisplayName("toggling shakeToGenerateEnabled flips only that flag")
    fun `toggling shakeToGenerateEnabled flips only that flag`() {
        val state = ConfigureScreenState(GenerationPreferences())
        assertTrue(state.preferences.shakeToGenerateEnabled)

        val disabled = state.toggleShakeToGenerate()
        assertFalse(disabled.preferences.shakeToGenerateEnabled)

        val reenabled = disabled.toggleShakeToGenerate()
        assertTrue(reenabled.preferences.shakeToGenerateEnabled)
    }

    @Test
    @DisplayName("selecting a palette replaces only that hand's palette")
    fun `selecting a palette replaces only that hand's palette`() {
        val state = ConfigureScreenState(GenerationPreferences())
        val updated = state.withRightHandPalette(CardPalette.IRIS)
        assertEquals(CardPalette.IRIS, updated.preferences.rightHandPalette)
        assertEquals(state.preferences.leftHandPalette, updated.preferences.leftHandPalette)
    }
}
