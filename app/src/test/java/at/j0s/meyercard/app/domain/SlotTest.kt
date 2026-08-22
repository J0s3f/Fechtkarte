package at.j0s.meyercard.app.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class SlotTest {

    @Test
    @DisplayName("GENERATOR_SLOTS has exactly 16 distinct entries")
    fun `GENERATOR_SLOTS has 16 distinct entries`() {
        assertEquals(16, Slot.GENERATOR_SLOTS.size)
        assertEquals(16, Slot.GENERATOR_SLOTS.toSet().size)
    }

    @Test
    @DisplayName("every direction appears exactly twice")
    fun `every direction appears exactly twice`() {
        val byDirection = Slot.GENERATOR_SLOTS.groupingBy { it.direction }.eachCount()
        for (direction in Direction.entries) {
            assertEquals(2, byDirection[direction], "expected $direction twice")
        }
    }

    @Test
    @DisplayName("both rings appear exactly eight times")
    fun `both rings appear exactly eight times`() {
        val outerCount = Slot.GENERATOR_SLOTS.count { it.radius == Radius.OUTER }
        val innerCount = Slot.GENERATOR_SLOTS.count { it.radius == Radius.INNER }
        assertEquals(8, outerCount)
        assertEquals(8, innerCount)
    }
}
