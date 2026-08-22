package at.j0s.meyercard.app.domain

import java.time.Clock
import java.time.Instant
import kotlin.random.Random

/**
 * The drill generator (docs/PLAN.md §5). [generate] is the baseline: sample
 * [actionCount] of the 16 generator slots without replacement, mark
 * [thrustCount] of them as thrusts, and pick a hand at random.
 * [generateWithRules] layers opt-in [GenerationRule]s on top, with bounded
 * retry and relaxation for rule sets that turn out unsatisfiable.
 *
 * [random] and [clock] are both injected everywhere, not read from a
 * global/default source, so a fixed seed produces a genuinely reproducible
 * [MeyerCard] — including its [CardOrigin.Generated] timestamp, not just its
 * actions.
 */
object DrillGenerator {
    private const val DEFAULT_ATTEMPTS_PER_RULE_SET = 500

    fun generate(
        actionCount: Int,
        thrustCount: Int,
        random: Random = Random,
        clock: Clock = Clock.systemUTC(),
    ): MeyerCard {
        validateCounts(actionCount, thrustCount)
        return buildCandidate(Slot.GENERATOR_SLOTS, actionCount, thrustCount, random, clock)
    }

    /**
     * As [generate], but only returns a card every rule in [rules] is
     * satisfied by. Retries up to [attemptsPerRuleSet] times against the
     * full rule set; if none of those attempts satisfy it, relaxes the most
     * recently added rule (the last element of [rules]) and starts a fresh
     * batch of attempts against the smaller set. Always terminates: with
     * zero rules left, every [GenerationRule]'s hooks default to a no-op, so
     * that final attempt cannot fail.
     */
    fun generateWithRules(
        actionCount: Int,
        thrustCount: Int,
        rules: List<GenerationRule>,
        random: Random = Random,
        clock: Clock = Clock.systemUTC(),
        previousHand: Hand? = null,
        attemptsPerRuleSet: Int = DEFAULT_ATTEMPTS_PER_RULE_SET,
    ): GenerationOutcome {
        validateCounts(actionCount, thrustCount)

        var activeCount = rules.size
        while (true) {
            val activeRules = rules.take(activeCount)
            repeat(attemptsPerRuleSet) {
                val pool = activeRules.fold(Slot.GENERATOR_SLOTS) { slots, rule -> rule.candidateSlots(slots, random) }
                if (pool.size < actionCount) return@repeat

                val card = buildCandidate(pool, actionCount, thrustCount, random, clock)
                if (activeRules.all { it.isSatisfiedBy(card.actions, card.hand, previousHand) }) {
                    return GenerationOutcome(card, relaxedRules = rules.drop(activeCount))
                }
            }
            check(activeCount > 0) { "no card satisfies even an empty rule set - should be impossible" }
            activeCount--
        }
    }

    private fun validateCounts(actionCount: Int, thrustCount: Int) {
        require(actionCount in 1..8) { "actionCount must be in 1..8, was $actionCount" }
        require(thrustCount in 0..actionCount) {
            "thrustCount must be in 0..actionCount ($actionCount), was $thrustCount"
        }
    }

    private fun buildCandidate(
        pool: List<Slot>,
        actionCount: Int,
        thrustCount: Int,
        random: Random,
        clock: Clock,
    ): MeyerCard {
        val slots = orderOuterBeforeInner(promoteIsolatedInner(pool.shuffled(random).take(actionCount)))
        val thrustIndices = (0 until actionCount).shuffled(random).take(thrustCount).toSet()
        val actions = slots.mapIndexed { index, slot ->
            Action(sequenceNumber = index + 1, slot = slot, isThrust = index in thrustIndices)
        }
        val hand = if (random.nextBoolean()) Hand.RIGHT else Hand.LEFT

        return MeyerCard(
            // Not yet persisted — Room assigns the real id on insert, once a
            // generated card is actually saved. 0 is a placeholder, not a
            // claim about identity.
            id = CardId(0L),
            actions = actions,
            hand = hand,
            palette = CardPalette.default(hand),
            origin = CardOrigin.Generated(at = Instant.now(clock)),
        )
    }

    /**
     * A direction sampled only at [Radius.INNER], with no [Radius.OUTER] slot for that same
     * direction anywhere in [slots], has its lone slot promoted to outer instead. Found on a
     * real generated card — [orderOuterBeforeInner] only reorders a same-direction *pair*, so
     * it does nothing when there is no pair to reorder, and the baseline sampling itself has no
     * notion that an inner pick implies its outer counterpart. Confirmed against the historical
     * dataset before fixing: of 94 inner-radius actions across the 109 historical cards, 84
     * (89%) share their card with that direction's outer action — the common case, so the
     * baseline generator should default to it rather than sampling either with equal weight.
     *
     * Always safe to promote: [pool] never contains both radii of the same direction more than
     * once each, so if [Radius.OUTER] for this direction wasn't already selected, promoting to
     * it cannot collide with an already-selected slot.
     */
    private fun promoteIsolatedInner(slots: List<Slot>): List<Slot> {
        val outerDirections = slots.filter { it.radius == Radius.OUTER }.map { it.direction }.toSet()
        return slots.map { slot ->
            if (slot.radius == Radius.INNER && slot.direction !in outerDirections) {
                Slot(slot.direction, Radius.OUTER)
            } else {
                slot
            }
        }
    }

    /**
     * When a direction was sampled twice — once at each ring — the outer slot must come before
     * the inner one. Found on a real generated card, then confirmed against the historical
     * dataset: 101 of 103 same-direction pairs across all 109 historical cards have the outer
     * action first (97%), not a stylistic tendency worth an opt-in [GenerationRule] but a
     * near-universal invariant the baseline generator itself should honour. [Slot.GENERATOR_SLOTS]
     * has exactly two slots per direction, so a direction can appear at most twice — a single
     * swap per direction is always enough, no direction can need reordering more than once.
     */
    private fun orderOuterBeforeInner(slots: List<Slot>): List<Slot> {
        val ordered = slots.toMutableList()
        for (direction in Direction.entries) {
            val outerIndex = ordered.indexOfFirst { it.direction == direction && it.radius == Radius.OUTER }
            val innerIndex = ordered.indexOfFirst { it.direction == direction && it.radius == Radius.INNER }
            if (outerIndex != -1 && innerIndex != -1 && innerIndex < outerIndex) {
                val outerSlot = ordered[outerIndex]
                ordered[outerIndex] = ordered[innerIndex]
                ordered[innerIndex] = outerSlot
            }
        }
        return ordered
    }
}

/** A generated card, plus which rules (if any, in the order they were added) had to be dropped to produce it. */
data class GenerationOutcome(val card: MeyerCard, val relaxedRules: List<GenerationRule>)
