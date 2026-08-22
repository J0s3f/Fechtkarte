package at.j0s.meyercard.app.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Instant

class CardOriginTest {

    @Test
    @DisplayName("Historical carries its source card number")
    fun `Historical carries its source card number`() {
        val origin: CardOrigin = CardOrigin.Historical(number = 47)
        assertEquals(47, (origin as CardOrigin.Historical).number)
    }

    @Test
    @DisplayName("Generated carries its creation instant")
    fun `Generated carries its creation instant`() {
        val now = Instant.now()
        val origin: CardOrigin = CardOrigin.Generated(at = now)
        assertEquals(now, (origin as CardOrigin.Generated).at)
    }
}
