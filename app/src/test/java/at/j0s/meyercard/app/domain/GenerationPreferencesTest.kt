package at.j0s.meyercard.app.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class GenerationPreferencesTest {

    @Test
    @DisplayName("the documented defaults are actionCount 4, thrustCount 0")
    fun `defaults are actionCount 4 thrustCount 0`() {
        val preferences = GenerationPreferences()
        assertEquals(4, preferences.actionCount)
        assertEquals(0, preferences.thrustCount)
    }

    @Test
    @DisplayName("more thrusts than actions is unconstructable")
    fun `more thrusts than actions is unconstructable`() {
        assertThrows(IllegalArgumentException::class.java) {
            GenerationPreferences(actionCount = 3, thrustCount = 4)
        }
    }

    @Test
    @DisplayName("actionCount outside 1..8 is unconstructable")
    fun `actionCount outside 1 to 8 is unconstructable`() {
        assertThrows(IllegalArgumentException::class.java) { GenerationPreferences(actionCount = 0) }
        assertThrows(IllegalArgumentException::class.java) { GenerationPreferences(actionCount = 9) }
    }

    @Test
    @DisplayName("thrustCount equal to actionCount is allowed")
    fun `thrustCount equal to actionCount is allowed`() {
        val preferences = GenerationPreferences(actionCount = 3, thrustCount = 3)
        assertEquals(3, preferences.thrustCount)
    }
}
