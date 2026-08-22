package at.j0s.meyercard.app.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class HandTest {

    @Test
    @DisplayName("NEUTRAL is its own opposite")
    fun `NEUTRAL is its own opposite`() {
        assertEquals(Hand.NEUTRAL, Hand.NEUTRAL.opposite())
    }

    @Test
    @DisplayName("LEFT and RIGHT are each other's opposite")
    fun `LEFT and RIGHT are each other's opposite`() {
        assertEquals(Hand.RIGHT, Hand.LEFT.opposite())
        assertEquals(Hand.LEFT, Hand.RIGHT.opposite())
    }
}
