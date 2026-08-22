package at.j0s.meyercard.app.domain

import kotlin.random.Random

/**
 * An opt-in constraint on generated drills, selected
 * polymorphically rather than through a `when` chain over an enum — the
 * anti-pattern CLAUDE.md calls out. A rule can narrow or re-weight
 * [candidateSlots] before sampling, validate the result via [isSatisfiedBy]
 * after, or both — each rule only needs one of the two, so both have
 * permissive defaults.
 */
interface GenerationRule {
    /** Narrows or re-weights the pool [Random] samples from. Identity by default. */
    fun candidateSlots(available: List<Slot>, random: Random): List<Slot> = available

    /**
     * Whether [actions] (in sequence order), generated for [hand], satisfies
     * this rule. [previousHand] is the previously generated card's hand, if
     * any — only [AlternateHands] uses it. `true` by default.
     */
    fun isSatisfiedBy(actions: List<Action>, hand: Hand, previousHand: Hand?): Boolean = true
}

/** Consecutive actions use different rays. */
object NoRepeatedDirection : GenerationRule {
    override fun isSatisfiedBy(actions: List<Action>, hand: Hand, previousHand: Hand?): Boolean =
        actions.zipWithNext().all { (a, b) -> a.slot.direction != b.slot.direction }
}

/** Consecutive rays are at least [minSteps] apart on the 8-ray compass. */
data class MinimumAngularDistance(val minSteps: Int) : GenerationRule {
    init {
        require(minSteps in 1..4) { "minSteps must be in 1..4, was $minSteps" }
    }

    override fun isSatisfiedBy(actions: List<Action>, hand: Hand, previousHand: Hand?): Boolean =
        actions.zipWithNext().all { (a, b) -> a.slot.direction.angularDistanceTo(b.slot.direction) >= minSteps }
}

/** Successive cards alternate hand instead of each being independently random. */
object AlternateHands : GenerationRule {
    override fun isSatisfiedBy(actions: List<Action>, hand: Hand, previousHand: Hand?): Boolean =
        previousHand == null || hand != previousHand
}

/** Restricts sampling to the 8 outer slots, for simpler drills. */
object OuterRingOnly : GenerationRule {
    override fun candidateSlots(available: List<Slot>, random: Random): List<Slot> =
        available.filter { it.radius == Radius.OUTER }
}

/**
 * Samples directions with the frequencies actually observed across the 109
 * historical cards (NE 126, NW 123, N 69, SW 61, SE 60, W 42, E 40, S 21)
 * instead of uniformly. Implemented as a
 * weighted shuffle — each slot is duplicated in proportion to its
 * direction's historical count, the expanded pool is shuffled, and only the
 * first (highest-priority) occurrence of each slot survives — rather than a
 * strict per-card predicate, since "matches a population's frequencies"
 * isn't something one small sample can meaningfully pass or fail.
 */
object MatchHistoricalDistribution : GenerationRule {
    private val historicalCounts = mapOf(
        Direction.NE to 126, Direction.NW to 123, Direction.N to 69, Direction.SW to 61,
        Direction.SE to 60, Direction.W to 42, Direction.E to 40, Direction.S to 21,
    )

    override fun candidateSlots(available: List<Slot>, random: Random): List<Slot> {
        val weighted = available.flatMap { slot -> List(historicalCounts.getValue(slot.direction)) { slot } }
        return weighted.shuffled(random).distinct()
    }
}
