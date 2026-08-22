package at.j0s.meyercard.app.adapter.ui.library

import at.j0s.meyercard.app.domain.Hand
import at.j0s.meyercard.app.domain.HistoricalDrill
import kotlin.random.Random

/**
 * The Drills tab's state: which of the 44 drills is showing, in which hand,
 * under which filter. Plain data — no Compose dependency — so it's directly
 * unit-testable; the Composable screen holds one of these in `remember`.
 */
data class DrillsLibraryState(
    val allDrills: List<HistoricalDrill>,
    val filter: DrillFilter = DrillFilter(),
    val hand: Hand = Hand.RIGHT,
    val position: BrowsePosition = BrowsePosition(0),
) {
    val visibleDrills: List<HistoricalDrill> get() = allDrills.filter { filter.matches(it) }
    val current: HistoricalDrill? get() = visibleDrills.getOrNull(position.index)

    fun withFilter(newFilter: DrillFilter) = copy(filter = newFilter, position = BrowsePosition(0))
    fun withHand(newHand: Hand) = copy(hand = newHand)
    fun toggleHand() = withHand(if (hand == Hand.RIGHT) Hand.LEFT else Hand.RIGHT)

    fun first() = copy(position = position.first())
    fun last() = copy(position = position.last(visibleDrills.size))
    fun next() = copy(position = position.next(visibleDrills.size))
    fun previous() = copy(position = position.previous())
    fun fastForward() = copy(position = position.fastForward(visibleDrills.size))
    fun fastBackward() = copy(position = position.fastBackward())
    fun random(random: Random = Random) = copy(position = position.random(visibleDrills.size, random))
}
