package at.j0s.meyercard.app.adapter.ui.library

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.random.Random

class BrowsePositionTest {

    @Test
    @DisplayName("next stops at the last index instead of overrunning")
    fun `next clamps at the last index`() {
        assertEquals(4, BrowsePosition(4).next(size = 5).index)
    }

    @Test
    @DisplayName("previous stops at 0 instead of going negative")
    fun `previous clamps at zero`() {
        assertEquals(0, BrowsePosition(0).previous().index)
    }

    @Test
    @DisplayName("fastForward advances by 10 but clamps at the last index")
    fun `fastForward advances by 10 and clamps`() {
        assertEquals(10, BrowsePosition(0).fastForward(size = 20).index)
        assertEquals(19, BrowsePosition(15).fastForward(size = 20).index)
    }

    @Test
    @DisplayName("fastBackward retreats by 10 but clamps at zero")
    fun `fastBackward retreats by 10 and clamps`() {
        assertEquals(0, BrowsePosition(5).fastBackward().index)
    }

    @Test
    @DisplayName("last lands on size - 1")
    fun `last lands on size minus one`() {
        assertEquals(9, BrowsePosition(0).last(size = 10).index)
    }

    @Test
    @DisplayName("an empty list's last is index 0, not -1")
    fun `last on an empty list is zero not negative`() {
        assertEquals(0, BrowsePosition(0).last(size = 0).index)
    }

    @Test
    @DisplayName("random always lands within bounds")
    fun `random lands within bounds`() {
        val random = Random(42)
        repeat(50) {
            val index = BrowsePosition(0).random(size = 7, random).index
            assertTrue(index in 0..6, "index $index out of bounds")
        }
    }
}
