package at.j0s.meyercard.app.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class ActionTest {

    private val slot = Slot(Direction.N, Radius.OUTER)

    @ParameterizedTest
    @ValueSource(ints = [0, -1, -100])
    @DisplayName("sequence number below 1 throws")
    fun `sequence number below 1 throws`(sequenceNumber: Int) {
        assertThrows(IllegalArgumentException::class.java) {
            Action(sequenceNumber, slot, isThrust = false)
        }
    }

    @Test
    @DisplayName("sequence number 1 is accepted")
    fun `sequence number 1 is accepted`() {
        val action = Action(1, slot, isThrust = false)
        assertEquals(1, action.sequenceNumber)
    }
}
