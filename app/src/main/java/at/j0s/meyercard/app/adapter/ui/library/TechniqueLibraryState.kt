package at.j0s.meyercard.app.adapter.ui.library

import at.j0s.meyercard.app.domain.MeyerCard
import kotlin.random.Random

/**
 * The Techniques tab's state: which of the 21 technique cards is showing,
 * under which filter. Plain data — no Compose dependency — so it's directly
 * unit-testable; the Composable screen holds one of these in `remember`.
 */
data class TechniqueLibraryState(
    val allCards: List<MeyerCard>,
    val filter: TechniqueFilter = TechniqueFilter(),
    val position: BrowsePosition = BrowsePosition(0),
) {
    val visibleCards: List<MeyerCard> get() = allCards.filter { filter.matches(it) }
    val current: MeyerCard? get() = visibleCards.getOrNull(position.index)

    fun withFilter(newFilter: TechniqueFilter) = copy(filter = newFilter, position = BrowsePosition(0))

    fun first() = copy(position = position.first())
    fun last() = copy(position = position.last(visibleCards.size))
    fun next() = copy(position = position.next(visibleCards.size))
    fun previous() = copy(position = position.previous())
    fun fastForward() = copy(position = position.fastForward(visibleCards.size))
    fun fastBackward() = copy(position = position.fastBackward())
    fun random(random: Random = Random) = copy(position = position.random(visibleCards.size, random))
}
