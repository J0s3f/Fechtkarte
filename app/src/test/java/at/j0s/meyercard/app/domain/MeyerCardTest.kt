package at.j0s.meyercard.app.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Instant

class MeyerCardTest {

    private fun action(seq: Int, direction: Direction, radius: Radius = Radius.OUTER) =
        Action(seq, Slot(direction, radius), isThrust = false)

    private fun card(actions: List<Action>) = MeyerCard(
        id = CardId(1L),
        actions = actions,
        hand = Hand.RIGHT,
        palette = CardPalette.WOAD,
        origin = CardOrigin.Generated(Instant.now()),
    )

    @Test
    @DisplayName("a well-formed card constructs successfully")
    fun `a well-formed card constructs successfully`() {
        val built = card(
            listOf(
                action(1, Direction.N),
                action(2, Direction.NE),
                action(3, Direction.SE),
            )
        )
        assertEquals(3, built.actions.size)
    }

    @Test
    @DisplayName("empty actions throws")
    fun `empty actions throws`() {
        assertThrows(IllegalArgumentException::class.java) { card(emptyList()) }
    }

    @Test
    @DisplayName("more than 8 actions throws")
    fun `more than 8 actions throws`() {
        val nineActions = Direction.entries.mapIndexed { index, direction ->
            action(index + 1, direction, if (index < 8) Radius.OUTER else Radius.INNER)
        } + action(9, Direction.N, Radius.INNER)
        assertThrows(IllegalArgumentException::class.java) { card(nineActions) }
    }

    @Test
    @DisplayName("sequence numbers must be exactly 1..n, each once")
    fun `sequence numbers must be exactly 1 to n each once`() {
        assertThrows(IllegalArgumentException::class.java) {
            card(listOf(action(1, Direction.N), action(3, Direction.NE)))
        }
    }

    @Test
    @DisplayName("duplicate sequence numbers throw")
    fun `duplicate sequence numbers throw`() {
        assertThrows(IllegalArgumentException::class.java) {
            card(listOf(action(1, Direction.N), action(1, Direction.NE)))
        }
    }

    @Test
    @DisplayName("two actions sharing a slot throws")
    fun `two actions sharing a slot throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            card(
                listOf(
                    action(1, Direction.N, Radius.OUTER),
                    action(2, Direction.N, Radius.OUTER),
                )
            )
        }
    }
}
