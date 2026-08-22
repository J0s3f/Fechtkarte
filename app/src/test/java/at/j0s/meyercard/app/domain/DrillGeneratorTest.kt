package at.j0s.meyercard.app.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class DrillGeneratorTest {

    @Test
    @DisplayName("a fixed seed produces the exact same card every time")
    fun `a fixed seed is reproducible`() {
        val clock = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC)
        val first = DrillGenerator.generate(actionCount = 5, thrustCount = 2, random = Random(42), clock = clock)
        val second = DrillGenerator.generate(actionCount = 5, thrustCount = 2, random = Random(42), clock = clock)
        assertEquals(first, second)
    }

    @Test
    @DisplayName("no two actions share a slot")
    fun `no slot repeats`() {
        val card = DrillGenerator.generate(actionCount = 8, thrustCount = 0, random = Random(1))
        val slots = card.actions.map { it.slot }
        assertEquals(slots.size, slots.toSet().size)
    }

    @Test
    @DisplayName("exactly thrustCount actions are thrusts")
    fun `thrust count is exact`() {
        val card = DrillGenerator.generate(actionCount = 6, thrustCount = 3, random = Random(7))
        assertEquals(3, card.actions.count { it.isThrust })
    }

    @Test
    @DisplayName("when a direction is hit twice, the outer action always comes before the inner one")
    fun `outer precedes inner within the same direction`() {
        // Found on a real generated card, then confirmed against the historical dataset (not
        // guessed): when a direction appears twice on a card, once at each ring, the outer
        // action comes first in 101 of 103 such pairs across all 109 historical cards (97%) --
        // near-universal, not just a stylistic tendency. The baseline generator picked slots
        // uniformly at random with no awareness of this, so a card could freely put every
        // low-numbered action on the inner ring, something the source material essentially
        // never does. actionCount=8 with a wide sweep of seeds maximises how often a
        // same-direction pair actually gets sampled, so the property gets exercised, not
        // just hoped for.
        repeat(500) { seed ->
            val card = DrillGenerator.generate(actionCount = 8, thrustCount = 0, random = Random(seed))
            val byDirection = card.actions.groupBy { it.slot.direction }
            for (actions in byDirection.values) {
                if (actions.size < 2) continue
                val outer = actions.single { it.slot.radius == Radius.OUTER }
                val inner = actions.single { it.slot.radius == Radius.INNER }
                assertTrue(
                    outer.sequenceNumber < inner.sequenceNumber,
                    "seed $seed: outer (${outer.sequenceNumber}) should precede inner (${inner.sequenceNumber}) " +
                        "for direction ${actions.first().slot.direction}",
                )
            }
        }
    }

    @Test
    @DisplayName("an inner action never appears without its direction's outer action also on the card")
    fun `no isolated inner action`() {
        // Found on a real generated card: a direction sampled only at the inner ring, with no
        // outer action anywhere on the same card. T9.5 already made outer precede inner
        // *within a same-direction pair*, but did nothing here, since there is no pair to
        // reorder -- the baseline sampled all 16 slots uniformly, with no notion that an inner
        // slot implies its outer counterpart. Confirmed against the historical dataset rather
        // than assumed: of 94 inner-radius actions across the 109 historical cards, 84 (89%)
        // share their card with that same direction's outer action -- the common case, not an
        // edge case, and the baseline generator should default to it. actionCount=8 across a
        // wide seed sweep maximises how often a lone inner pick would otherwise get sampled.
        repeat(500) { seed ->
            val card = DrillGenerator.generate(actionCount = 8, thrustCount = 0, random = Random(seed))
            val byDirection = card.actions.groupBy { it.slot.direction }
            for (actions in byDirection.values) {
                val hasInner = actions.any { it.slot.radius == Radius.INNER }
                val hasOuter = actions.any { it.slot.radius == Radius.OUTER }
                assertTrue(
                    !hasInner || hasOuter,
                    "seed $seed: ${actions.first().slot.direction} has an inner action but no outer one",
                )
            }
        }
    }

    @Test
    @DisplayName("requesting more than 8 actions is rejected")
    fun `more than 8 actions is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            DrillGenerator.generate(actionCount = 9, thrustCount = 0, random = Random(1))
        }
    }

    @Test
    @DisplayName("requesting 0 actions is rejected")
    fun `zero actions is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            DrillGenerator.generate(actionCount = 0, thrustCount = 0, random = Random(1))
        }
    }

    @Test
    @DisplayName("requesting more thrusts than actions is rejected")
    fun `more thrusts than actions is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            DrillGenerator.generate(actionCount = 3, thrustCount = 4, random = Random(1))
        }
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("an unsatisfiable rule set terminates and reports which rule was relaxed")
    fun `an unsatisfiable rule set terminates and reports the relaxation`() {
        // MinimumAngularDistance(4) with 8 actions has no solution: the
        // 8-ray compass's maximum angular distance is 4 itself, so every consecutive pair
        // would have to be exact opposites, chained across all 8 actions — impossible without
        // reusing a direction, which needs only 4 of the 16 slots (two antipodal directions'
        // OUTER/INNER pairs), nowhere near enough for 8 distinct actions.
        val rule = MinimumAngularDistance(4)
        val outcome = DrillGenerator.generateWithRules(
            actionCount = 8,
            thrustCount = 0,
            rules = listOf(rule),
            random = Random(1),
        )
        assertEquals(listOf(rule), outcome.relaxedRules)
        assertEquals(8, outcome.card.actions.size)
    }

    @Test
    @DisplayName("a satisfiable rule set generates a card without relaxing anything")
    fun `a satisfiable rule set relaxes nothing`() {
        val outcome = DrillGenerator.generateWithRules(
            actionCount = 4,
            thrustCount = 0,
            rules = listOf(NoRepeatedDirection),
            random = Random(1),
        )
        assertTrue(outcome.relaxedRules.isEmpty())
        assertTrue(NoRepeatedDirection.isSatisfiedBy(outcome.card.actions, outcome.card.hand, null))
    }
}
