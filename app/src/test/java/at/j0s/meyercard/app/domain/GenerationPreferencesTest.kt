package at.j0s.meyercard.app.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.random.Random

class GenerationPreferencesTest {

    @Test
    @DisplayName("the documented defaults are actionCount 4, thrustCount 0")
    fun `defaults are actionCount 4 thrustCount 0`() {
        val preferences = GenerationPreferences()
        assertEquals(4, preferences.actionCount)
        assertEquals(0, preferences.thrustCount)
    }

    @Test
    @DisplayName("counts are exact by default, whatever the seed")
    fun `resolveCounts is exact by default`() {
        val preferences = GenerationPreferences(actionCount = 5, thrustCount = 2)
        repeat(20) { seed ->
            val (actionCount, thrustCount) = preferences.resolveCounts(Random(seed))
            assertEquals(5, actionCount)
            assertEquals(2, thrustCount)
        }
    }

    @Test
    @DisplayName("actionCountIsMaximum draws a value in 1..actionCount, never above it")
    fun `actionCountIsMaximum draws within range`() {
        val preferences = GenerationPreferences(actionCount = 5, thrustCount = 0, actionCountIsMaximum = true)
        val drawn = (0 until 200).map { seed -> preferences.resolveCounts(Random(seed)).first }.toSet()
        assertTrue(drawn.all { it in 1..5 }, "every draw must be in 1..5, got $drawn")
        assertTrue(drawn.size > 1, "200 seeds should produce more than one distinct value, got $drawn")
    }

    @Test
    @DisplayName("thrustCount is re-clamped to whatever actionCount was actually drawn")
    fun `thrustCount is clamped to the drawn actionCount`() {
        val preferences = GenerationPreferences(actionCount = 5, thrustCount = 5, actionCountIsMaximum = true)
        repeat(200) { seed ->
            val (actionCount, thrustCount) = preferences.resolveCounts(Random(seed))
            assertTrue(thrustCount <= actionCount, "seed $seed: thrustCount $thrustCount exceeds actionCount $actionCount")
        }
    }

    @Test
    @DisplayName("thrustCountIsMaximum draws a value in 0..thrustCount, never above it")
    fun `thrustCountIsMaximum draws within range`() {
        val preferences = GenerationPreferences(actionCount = 6, thrustCount = 4, thrustCountIsMaximum = true)
        val drawn = (0 until 200).map { seed -> preferences.resolveCounts(Random(seed)).second }.toSet()
        assertTrue(drawn.all { it in 0..4 }, "every draw must be in 0..4, got $drawn")
        assertTrue(drawn.size > 1, "200 seeds should produce more than one distinct value, got $drawn")
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
