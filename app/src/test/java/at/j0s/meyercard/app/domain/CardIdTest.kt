package at.j0s.meyercard.app.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class CardIdTest {

    @Test
    @DisplayName("wraps its value without transformation")
    fun `wraps its value without transformation`() {
        assertEquals(47L, CardId(47L).value)
    }

    @Test
    @DisplayName("equal values are equal ids")
    fun `equal values are equal ids`() {
        assertEquals(CardId(1L), CardId(1L))
    }
}
