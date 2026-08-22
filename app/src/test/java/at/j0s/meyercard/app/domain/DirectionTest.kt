package at.j0s.meyercard.app.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class DirectionTest {

    @ParameterizedTest
    @EnumSource(Direction::class)
    @DisplayName("mirroring is an involution")
    fun `mirroring is an involution`(direction: Direction) {
        assertEquals(direction, direction.mirrored().mirrored())
    }

    @Test
    @DisplayName("N and S are their own mirror")
    fun `N and S are their own mirror`() {
        assertEquals(Direction.N, Direction.N.mirrored())
        assertEquals(Direction.S, Direction.S.mirrored())
    }

    @Test
    @DisplayName("diagonal and lateral pairs mirror onto each other")
    fun `diagonal and lateral pairs mirror onto each other`() {
        assertEquals(Direction.NE, Direction.NW.mirrored())
        assertEquals(Direction.NW, Direction.NE.mirrored())
        assertEquals(Direction.SE, Direction.SW.mirrored())
        assertEquals(Direction.SW, Direction.SE.mirrored())
        assertEquals(Direction.E, Direction.W.mirrored())
        assertEquals(Direction.W, Direction.E.mirrored())
    }

    @ParameterizedTest
    @EnumSource(Direction::class)
    @DisplayName("angular distance is symmetric and never exceeds 4")
    fun `angular distance is symmetric and never exceeds 4`(direction: Direction) {
        for (other in Direction.entries) {
            val forward = direction.angularDistanceTo(other)
            val backward = other.angularDistanceTo(direction)
            assertEquals(forward, backward, "distance must be symmetric")
            assertTrue(forward in 0..4, "distance $forward out of range for $direction -> $other")
        }
    }

    @Test
    @DisplayName("opposite directions are 4 apart")
    fun `opposite directions are 4 apart`() {
        assertEquals(4, Direction.NE.angularDistanceTo(Direction.SW))
        assertEquals(4, Direction.N.angularDistanceTo(Direction.S))
        assertEquals(4, Direction.E.angularDistanceTo(Direction.W))
    }

    @Test
    @DisplayName("a direction is zero distance from itself")
    fun `a direction is zero distance from itself`() {
        for (direction in Direction.entries) {
            assertEquals(0, direction.angularDistanceTo(direction))
        }
    }

    @Test
    @DisplayName("adjacent directions are 1 apart")
    fun `adjacent directions are 1 apart`() {
        assertEquals(1, Direction.N.angularDistanceTo(Direction.NE))
        assertEquals(1, Direction.NW.angularDistanceTo(Direction.N))
    }

    @Test
    @DisplayName("N sits on the top edge at mid-width")
    fun `N sits on the top edge at mid-width`() {
        val point = Direction.N.edgePointNormalised()
        assertEquals(0.5f, point.x)
        assertEquals(0f, point.y)
    }

    @Test
    @DisplayName("NE sits exactly on the top-right corner")
    fun `NE sits exactly on the top-right corner`() {
        val point = Direction.NE.edgePointNormalised()
        assertEquals(1f, point.x)
        assertEquals(0f, point.y)
    }

    @ParameterizedTest
    @EnumSource(Direction::class)
    @DisplayName("every edge point lies on the card rectangle boundary")
    fun `every edge point lies on the card rectangle boundary`(direction: Direction) {
        val point = direction.edgePointNormalised()
        val onVerticalEdge = point.x == 0f || point.x == 1f
        val onHorizontalEdge = point.y == 0f || point.y == CARD_ASPECT_INVERSE
        assertTrue(point.x in 0f..1f, "x out of card bounds for $direction")
        assertTrue(point.y in 0f..CARD_ASPECT_INVERSE, "y out of card bounds for $direction")
        assertTrue(onVerticalEdge || onHorizontalEdge, "$direction is not on the boundary")
    }
}
