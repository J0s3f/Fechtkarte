package at.j0s.meyercard.app.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.random.Random

private fun action(direction: Direction, seq: Int = 1) = Action(seq, Slot(direction, Radius.OUTER), isThrust = false)

/**
 * Every concrete rule in this file overrides exactly one of [GenerationRule]'s two methods, so
 * neither existing rule's own tests ever exercise the *other* method's permissive default —
 * this class does, directly, against a minimal implementation that overrides neither. Guards the
 * interface's own contract (identity pool, always satisfied) independently of any specific rule.
 */
class GenerationRuleDefaultsTest {
    private object NoOverrides : GenerationRule

    @Test
    @DisplayName("candidateSlots defaults to identity — the pool passed in comes back unchanged")
    fun `candidateSlots default is identity`() {
        val pool = Slot.GENERATOR_SLOTS
        assertEquals(pool, NoOverrides.candidateSlots(pool, Random(1)))
    }

    @Test
    @DisplayName("isSatisfiedBy defaults to true, regardless of actions or hands")
    fun `isSatisfiedBy default is true`() {
        val actions = listOf(action(Direction.N), action(Direction.N, 2))
        assertTrue(NoOverrides.isSatisfiedBy(actions, Hand.RIGHT, previousHand = Hand.RIGHT))
    }
}

class NoRepeatedDirectionTest {

    @Test
    @DisplayName("many seeded 5-action sequences: satisfied iff no two consecutive actions share a direction")
    fun `satisfied iff no two consecutive actions share a direction`() {
        // Slot sampling is without replacement, but GENERATOR_SLOTS pairs every direction with
        // both an OUTER and an INNER radius — two *distinct* slots (e.g. N-OUTER, N-INNER) can
        // still land consecutively and share a direction. A first version of this test assumed
        // otherwise and asserted every random draw satisfied the rule unconditionally; a clean
        // container build actually running this test for the first time proved that false. This
        // checks the predicate against an independently computed expectation instead.
        var violatingCaseSeen = false
        repeat(200) { seed ->
            val actions = Slot.GENERATOR_SLOTS.shuffled(Random(seed)).take(5)
                .mapIndexed { index, slot -> Action(index + 1, slot, isThrust = false) }
            val expected = actions.zipWithNext().all { (a, b) -> a.slot.direction != b.slot.direction }
            if (!expected) violatingCaseSeen = true
            assertEquals(expected, NoRepeatedDirection.isSatisfiedBy(actions, Hand.RIGHT, null))
        }
        assertTrue(violatingCaseSeen, "no seeded run ever produced a same-direction consecutive pair")
    }

    @Test
    @DisplayName("a repeated consecutive direction is rejected")
    fun `a repeated consecutive direction is rejected`() {
        val actions = listOf(action(Direction.N, 1), action(Direction.N, 2))
        assertFalse(NoRepeatedDirection.isSatisfiedBy(actions, Hand.RIGHT, null))
    }
}

class MinimumAngularDistanceTest {

    @Test
    @DisplayName("minSteps outside 1..4 is rejected")
    fun `minSteps outside 1 to 4 is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { MinimumAngularDistance(0) }
        assertThrows(IllegalArgumentException::class.java) { MinimumAngularDistance(5) }
    }

    @Test
    @DisplayName("many seeded runs: satisfied sequences always have every consecutive pair at least minSteps apart")
    fun `satisfied sequences always meet the minimum distance`() {
        val rule = MinimumAngularDistance(2)
        var satisfiedCount = 0
        repeat(500) { seed ->
            val actions = Slot.GENERATOR_SLOTS.shuffled(Random(seed)).take(4)
                .mapIndexed { index, slot -> Action(index + 1, slot, isThrust = false) }
            if (rule.isSatisfiedBy(actions, Hand.RIGHT, null)) {
                satisfiedCount++
                val distances = actions.zipWithNext { a, b -> a.slot.direction.angularDistanceTo(b.slot.direction) }
                assertTrue(distances.all { it >= 2 }, "distances $distances violate minSteps=2")
            }
        }
        // Not every random draw satisfies a real constraint - if literally none did, the
        // predicate would be untested (or the rule impossible to ever satisfy by chance).
        assertTrue(satisfiedCount > 0, "no seeded run ever satisfied MinimumAngularDistance(2)")
    }

    @Test
    @DisplayName("adjacent rays (distance 1) violate a minimum of 2")
    fun `adjacent rays violate a minimum of 2`() {
        val actions = listOf(action(Direction.N, 1), action(Direction.NE, 2))
        assertFalse(MinimumAngularDistance(2).isSatisfiedBy(actions, Hand.RIGHT, null))
    }
}

class AlternateHandsTest {

    @Test
    @DisplayName("no previous card always satisfies the rule")
    fun `no previous card always satisfies`() {
        assertTrue(AlternateHands.isSatisfiedBy(emptyList(), Hand.RIGHT, previousHand = null))
    }

    @Test
    @DisplayName("many seeded hand picks: satisfied only when the hand differs from the previous one")
    fun `satisfied only when hand differs from previous`() {
        repeat(100) { seed ->
            val random = Random(seed)
            val hand = if (random.nextBoolean()) Hand.RIGHT else Hand.LEFT
            val previousHand = if (random.nextBoolean()) Hand.RIGHT else Hand.LEFT
            assertEquals(hand != previousHand, AlternateHands.isSatisfiedBy(emptyList(), hand, previousHand))
        }
    }
}

class OuterRingOnlyTest {

    @Test
    @DisplayName("many seeded shuffles: every candidate slot is on the outer ring")
    fun `every candidate slot is on the outer ring`() {
        repeat(200) { seed ->
            val candidates = OuterRingOnly.candidateSlots(Slot.GENERATOR_SLOTS, Random(seed))
            assertTrue(candidates.isNotEmpty())
            assertTrue(candidates.all { it.radius == Radius.OUTER })
        }
    }

    @Test
    @DisplayName("exactly the 8 outer slots are offered, one per direction")
    fun `exactly the 8 outer slots are offered`() {
        val candidates = OuterRingOnly.candidateSlots(Slot.GENERATOR_SLOTS, Random(1))
        assertEquals(Direction.entries.toSet(), candidates.map { it.direction }.toSet())
    }
}

class MatchHistoricalDistributionTest {

    @Test
    @DisplayName("many seeded runs: the highest-frequency direction (NE) is picked first more often than the lowest (S)")
    fun `NE is picked first more often than S`() {
        var neFirstCount = 0
        var sFirstCount = 0
        repeat(2000) { seed ->
            val firstDirection = MatchHistoricalDistribution.candidateSlots(Slot.GENERATOR_SLOTS, Random(seed))
                .first().direction
            if (firstDirection == Direction.NE) neFirstCount++
            if (firstDirection == Direction.S) sFirstCount++
        }
        assertTrue(
            neFirstCount > sFirstCount,
            "NE picked first $neFirstCount times, S picked first $sFirstCount times - expected NE (weight 126) to lead S (weight 21)",
        )
    }

    @Test
    @DisplayName("the candidate pool always contains all 16 generator slots, just reordered")
    fun `the candidate pool contains all 16 slots`() {
        val candidates = MatchHistoricalDistribution.candidateSlots(Slot.GENERATOR_SLOTS, Random(1))
        assertEquals(Slot.GENERATOR_SLOTS.toSet(), candidates.toSet())
    }
}
